/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import io.grpc.ManagedChannel;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper of {@link OtlpGrpcSpanExporter} to temporarily fix {@link <a href="https://github.com/cyrille-leclerc/opentelemetry-maven-extension/issues/1">NoClassDefFoundError on GRPC classes after the Maven build is finished #1</a>}
 * thanks to a workaround of {@link <a href="https://github.com/open-telemetry/opentelemetry-java/issues/3521">OtlpGrpcExporter/Netty still active after SdkTracerProvider#shutdown() #3521</a>}
 */
public class MyOtlpGrpcSpanExporter implements SpanExporter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    final OtlpGrpcSpanExporter otlpGrpcSpanExporter;
    final ManagedChannel managedChannel;

    MyOtlpGrpcSpanExporter(OtlpGrpcSpanExporter otlpGrpcSpanExporter, ManagedChannel managedChannel) {
        this.otlpGrpcSpanExporter = otlpGrpcSpanExporter;
        this.managedChannel = managedChannel;
    }

    public static MyOtlpGrpcSpanExporterBuilder builder() {
        return new MyOtlpGrpcSpanExporterBuilder();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        return otlpGrpcSpanExporter.export(spans);
    }

    @Override
    public CompletableResultCode flush() {
        return otlpGrpcSpanExporter.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return otlpGrpcSpanExporter.shutdown();
    }

    @Override
    public void close() {
        {
            logger.debug("Shutdown otlpGrpcSpanExporter...");
            long before = System.nanoTime();
            final CompletableResultCode spanExporterShutdown = otlpGrpcSpanExporter.shutdown().join(10, TimeUnit.SECONDS);
            if (spanExporterShutdown.isSuccess()) {
                logger.debug("OtlpGrpcSpanExporter shutdown in " + Duration.ofNanos(System.nanoTime() - before).toMillis() + "ms");
            } else {
                logger.warn("Failure to shutdown OtlpGrpcSpanExporter in " + Duration.ofNanos(System.nanoTime() - before).toMillis() + "ms");
            }
        }
        {
            logger.debug("Shutdown GRPC managed channel...");
            long before = System.nanoTime();
            this.managedChannel.shutdown();
            try {
                boolean terminated = this.managedChannel.awaitTermination(10, TimeUnit.SECONDS);
                if (terminated) {
                    logger.debug("GRPC managed channel shutdown in " + Duration.ofNanos(System.nanoTime() - before).toMillis() + "ms");
                } else {
                    logger.warn("Failure to shutdown GRPC managed channel in " + Duration.ofNanos(System.nanoTime() - before).toMillis() + "ms");
                }
            } catch (InterruptedException e) {
                logger.warn("Silently ignore " + e, e);
            }
        }
    }
}
