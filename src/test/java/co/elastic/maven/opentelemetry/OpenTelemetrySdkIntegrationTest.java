/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OpenTelemetrySdkIntegrationTest {

    private static final Random RANDOM = new Random();

    @Test
    public void testShutDown() {
        String otlpEndpoint = "http://localhost:4317";
        OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(otlpGrpcSpanExporter).build();
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "junit")))
                .addSpanProcessor(batchSpanProcessor)
                .build();

        final OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        final Tracer tracer = GlobalOpenTelemetry.getTracer("junit");

        for (int i = 0; i < 10; i++) {
            final Span rootSpan = tracer.spanBuilder("opentelemetry-sdk-shut-down-test").startSpan();
            try (final Scope scope = rootSpan.makeCurrent()) {
                Thread.sleep(RANDOM.nextInt(50));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                rootSpan.end();
            }
        }


        System.err.println("Shutdown OpenTelemetry SDK Trace Provider...");
        long beforeNanos = System.nanoTime();
        final CompletableResultCode sdkProviderShutDown = sdkTracerProvider.shutdown();
        final CompletableResultCode join = sdkProviderShutDown.join(1, TimeUnit.SECONDS);
        Assert.assertEquals("SdkTracerProvider gracefully shut down", true, join.isSuccess());
        long afterNanos = System.nanoTime();
        System.err.println("OpenTelemetry SDK Trace Provider shutdown in " +
                TimeUnit.NANOSECONDS.toMillis(afterNanos - beforeNanos) + "ms, " +
                "There should be not activity on the OTLP GRPC Exporter after this step"
        );
    }
}
