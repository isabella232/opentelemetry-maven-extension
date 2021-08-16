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
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
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

@Component(role = OpenTelemetrySdkService.class, hint = "opentelemetry-service")
public class OpenTelemetrySdkService implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private RuntimeInformation runtimeInformation;

    private OpenTelemetrySdk openTelemetrySdk;

    private Tracer tracer;

    public synchronized Tracer getTracer() {
        if (tracer == null) {
            // OTEL_EXPORTER_OTLP_ENDPOINT
            String otlpEndpoint = System.getProperty("otel.exporter.otlp.endpoint",
                    System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
            if (StringUtils.isBlank(otlpEndpoint)) {
                GlobalOpenTelemetry.set(OpenTelemetry.noop());
            } else {
                OtlpGrpcSpanExporterBuilder spanExporterBuilder = OtlpGrpcSpanExporter.builder();
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
                    spanExporterBuilder.setTimeout(Duration.ofMillis(Long.parseLong(otlpExporterTimeoutMillis)));
                }

                SpanExporter spanExporter = spanExporterBuilder.build();

                AttributesBuilder resourceAttributes = Attributes.builder();
                Resource mavenResource = getMavenResource();
                resourceAttributes.putAll(mavenResource.getAttributes());

                // OTEL_RESOURCE_ATTRIBUTES
                String otelResourceAttributesAsString = System.getProperty("otel.resource.attributes",
                        System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
                if (StringUtils.isNotBlank(otelResourceAttributesAsString)) {
                    Map<String, String> otelResourceAttributes = OtelUtils.getCommaSeparatedMap(otelResourceAttributesAsString);
                    // see io.opentelemetry.sdk.autoconfigure.EnvironmentResource.getAttributes
                    otelResourceAttributes.forEach(resourceAttributes::put);
                }

                SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                        .setResource(Resource.create(resourceAttributes.build()))
                        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                        .build();

                // TODO OTEL_PROPAGATORS otel.propagators https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#propagator

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
     * See https://gist.github.com/cyrille-leclerc/2694ce214b7f95b38e1dab02dc36390d
     */
    protected @Nonnull
    Resource getMavenResource() {
        final String mavenVersion = this.runtimeInformation.getMavenVersion();
        final Attributes attributes = Attributes.of(ResourceAttributes.SERVICE_NAME, "maven", ResourceAttributes.SERVICE_VERSION, mavenVersion);
        return Resource.create(attributes);
    }

    /**
     * See https://gist.github.com/cyrille-leclerc/57903e63d1f162969154eec3bb82a576
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (this.openTelemetrySdk != null) {
            logger.info("Shutdown OTLP exporter...");
            long before = System.currentTimeMillis();
            final CompletableResultCode completableResultCode = this.openTelemetrySdk.getSdkTracerProvider().shutdown();

            completableResultCode.join(10, TimeUnit.SECONDS);
            logger.info("OTLP exporter shut down in " + (System.currentTimeMillis() - before) + " ms, done: " + completableResultCode.isDone() + " success: " + completableResultCode.isSuccess());
        }
    }
}
