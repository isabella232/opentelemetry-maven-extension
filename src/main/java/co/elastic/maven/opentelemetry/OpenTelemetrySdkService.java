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
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
public class OpenTelemetrySdkService implements Initializable, Disposable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private RuntimeInformation runtimeInformation;

    private OpenTelemetrySdk openTelemetrySdk;

    private Tracer tracer;

    private SpanExporter spanExporter;

    @Override
    public synchronized void dispose() {
        logger.debug("OpenTelemetry: dispose OpenTelemetrySdkService...");
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
            try {
                this.spanExporter.close();
            } catch (NoClassDefFoundError error) {
                if (logger.isDebugEnabled()) {
                    logger.warn("OpenTelemetry: NoClassDefFoundError shutting down SpanExporter: " + error.getMessage(), error);
                } else {
                    logger.warn("OpenTelemetry: NoClassDefFoundError shutting down SpanExporter: " + error.getMessage());
                }
            }
            GlobalOpenTelemetry.resetForTest();
            this.openTelemetrySdk = null;
        }
        logger.debug("OpenTelemetry: OpenTelemetrySdkService disposed");
    }

    /**
     * TODO add support for `OTEL_EXPORTER_OTLP_CERTIFICATE`
     */
    @Override
    public void initialize() throws InitializationException {
        logger.debug("OpenTelemetry: initialize OpenTelemetrySdkService...");
        // OTEL_EXPORTER_OTLP_ENDPOINT
        String otlpEndpoint = System.getProperty("otel.exporter.otlp.endpoint",
                System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
        if (StringUtils.isBlank(otlpEndpoint)) {
            logger.debug("OpenTelemetry: No -Dotel.exporter.otlp.endpoint property or OTEL_EXPORTER_OTLP_ENDPOINT environment variable found, use a NOOP tracer");
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
                    logger.warn("OpenTelemetry: Skip invalid OTLP timeout " + otlpExporterTimeoutMillis, e);
                }
            }

            this.spanExporter = spanExporterBuilder.build();

            // OTEL_RESOURCE_ATTRIBUTES
            AttributesBuilder resourceAttributesBuilder = Attributes.builder();
            Resource mavenResource = getMavenResource();
            resourceAttributesBuilder.putAll(mavenResource.getAttributes());
            String otelResourceAttributesAsString = System.getProperty("otel.resource.attributes",
                    System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
            if (StringUtils.isNotBlank(otelResourceAttributesAsString)) {
                Map<String, String> otelResourceAttributes = OtelUtils.getCommaSeparatedMap(otelResourceAttributesAsString);
                // see io.opentelemetry.sdk.autoconfigure.EnvironmentResource.getAttributes
                otelResourceAttributes.forEach(resourceAttributesBuilder::put);
            }
            final Attributes resourceAttributes = resourceAttributesBuilder.build();

            logger.debug("OpenTelemetry: Export OpenTelemetry traces to {} with attributes: {}", otlpEndpoint, resourceAttributes);

            final BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build();
            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                    .setResource(Resource.create(resourceAttributes))
                    .addSpanProcessor(batchSpanProcessor)
                    .build();

            this.openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .buildAndRegisterGlobal();
        }
        this.tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.contrib.maven");
    }

    public Tracer getTracer() {
        if (tracer == null) {
            throw new IllegalStateException("Not initialized");
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
}
