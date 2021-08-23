/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service to configure the {@link OpenTelemetry} instance.
 *
 * Mimic the {@link <a href="https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure">OpenTelemetry SDK Autoconfigure</a>}
 * that could not be used as is due to class loading issues when declaring the Maven OpenTelemetry extension using the pom.xml {@code <extension>} declaration.
 *
 * Exception example: https://gist.github.com/cyrille-leclerc/57903e63d1f162969154eec3bb82a576
 */
@Component(role = OpenTelemetrySdkService.class, hint = "opentelemetry-service")
public class OpenTelemetrySdkService implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private RuntimeInformation runtimeInformation;

    private OpenTelemetrySdk openTelemetrySdk;

    private Tracer tracer;

    private SpanExporter spanExporter;

    public synchronized Tracer getTracer() {
        if (tracer == null) {
            // OTEL_EXPORTER_OTLP_ENDPOINT
            String otlpEndpoint = System.getProperty("otel.exporter.otlp.endpoint",
                    System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
            if (StringUtils.isBlank(otlpEndpoint)) {
                logger.debug("No -Dotel.exporter.otlp.endpoint property or OTEL_EXPORTER_OTLP_ENDPOINT environment variable found, use a NOOP tracer");
                GlobalOpenTelemetry.set(OpenTelemetry.noop());
            } else {
                // OtlpGrpcSpanExporterBuilder spanExporterBuilder = OtlpGrpcSpanExporter.builder();
                MyOtlpGrpcSpanExporterBuilder spanExporterBuilder = MyOtlpGrpcSpanExporter.builder();
                spanExporterBuilder.setEndpoint(otlpEndpoint);

                // OTEL_EXPORTER_OTLP_HEADERS
                String otlpExporterHeadersAsString = System.getProperty("otel.exporter.otlp.headers",
                        System.getenv("OTEL_EXPORTER_OTLP_HEADERS"));
                Map<String, String> otlpExporterHeaders = OtelUtils.getCommaSeparatedMap(otlpExporterHeadersAsString);
                otlpExporterHeaders.forEach(spanExporterBuilder::addHeader);

                // OTEL_EXPORTER_OTLP_TIMEOUT
                String otlpExporterTimeoutMillis = System.getProperty("otel.exporter.otlp.timeout",
                        System.getenv("OTEL_EXPORTER_OTLP_TIMEOUT"));
                if (StringUtils.isNotBlank(otlpExporterTimeoutMillis)) {
                    try {
                        spanExporterBuilder.setTimeout(Duration.ofMillis(Long.parseLong(otlpExporterTimeoutMillis)));
                    } catch (NumberFormatException e) {
                        logger.warn("Skip invalid OTLP timeout " + otlpExporterTimeoutMillis, e);
                    }
                }

                this.spanExporter = spanExporterBuilder.build();

                // OTEL_RESOURCE_ATTRIBUTES
                AttributesBuilder resourceAttributes = Attributes.builder();
                Resource mavenResource = getMavenResource();
                resourceAttributes.putAll(mavenResource.getAttributes());
                String otelResourceAttributesAsString = System.getProperty("otel.resource.attributes",
                        System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
                if (StringUtils.isNotBlank(otelResourceAttributesAsString)) {
                    Map<String, String> otelResourceAttributes = OtelUtils.getCommaSeparatedMap(otelResourceAttributesAsString);
                    // see io.opentelemetry.sdk.autoconfigure.EnvironmentResource.getAttributes
                    otelResourceAttributes.forEach(resourceAttributes::put);
                }

                logger.debug("Export OpenTelemetry traces to {} with attributes: {}", otlpEndpoint, StringUtils.defaultIfBlank(otelResourceAttributesAsString, ""));

                final BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build();
                SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                        .setResource(Resource.create(resourceAttributes.build()))
                        .addSpanProcessor(batchSpanProcessor)
                        .build();

                this.openTelemetrySdk = OpenTelemetrySdk.builder()
                        .setTracerProvider(sdkTracerProvider)
                        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                        .buildAndRegisterGlobal();
            }
            this.tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.contrib.maven");
        }
        return tracer;
    }

    /**
     * Don't use a {@code io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider} due to classloading issue when loading
     * the Maven OpenTelemetry extension as a pom.xml {@code <extension>}.
     * See exception https://gist.github.com/cyrille-leclerc/2694ce214b7f95b38e1dab02dc36390d
     */
    protected @Nonnull
    Resource getMavenResource() {
        final String mavenVersion = this.runtimeInformation.getMavenVersion();
        final Attributes attributes = Attributes.of(ResourceAttributes.SERVICE_NAME, "maven", ResourceAttributes.SERVICE_VERSION, mavenVersion);
        return Resource.create(attributes);
    }

    @Override
    public void close() throws IOException {
        if (this.openTelemetrySdk != null) {
            logger.debug("OpenTelemetry: Shutdown SDK Trace Provider...");
            long before = System.currentTimeMillis();
            final CompletableResultCode sdkProviderShutdown = this.openTelemetrySdk.getSdkTracerProvider().shutdown();
            sdkProviderShutdown.join(10, TimeUnit.SECONDS);
            if (sdkProviderShutdown.isSuccess()) {
                logger.debug("OpenTelemetry: SDK Trace Provider shutdown in " + (System.currentTimeMillis() - before) + " ms");
            } else {
                logger.warn("OpenTelemetry: Failure to shutdown SDK Trace Provider in " + (System.currentTimeMillis() - before) + " ms, done: " + sdkProviderShutdown.isDone() + " success: " + sdkProviderShutdown.isSuccess());
            }
            // fix https://github.com/cyrille-leclerc/opentelemetry-maven-extension/issues/1
            // working around https://github.com/open-telemetry/opentelemetry-java/issues/3521
            this.spanExporter.close();
        }
    }
}
