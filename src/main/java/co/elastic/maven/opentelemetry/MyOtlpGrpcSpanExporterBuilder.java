/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.opentelemetry.exporter.otlp.internal.SslUtil;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.util.Objects.requireNonNull;

/**
 * Builder of {@link MyOtlpGrpcSpanExporter}
 */
public class MyOtlpGrpcSpanExporterBuilder {
    private static final long DEFAULT_TIMEOUT_SECS = 10;

    private long timeoutNanos = TimeUnit.SECONDS.toNanos(DEFAULT_TIMEOUT_SECS);
    private String endpointAsString;
    private Metadata metadata = null;
    private byte[] trustedCertificatesPem = null;

    MyOtlpGrpcSpanExporterBuilder() {

    }
    public MyOtlpGrpcSpanExporterBuilder setTimeout(Duration timeout) {
        this.timeoutNanos = timeout.toNanos();
        return this;
    }

    public MyOtlpGrpcSpanExporterBuilder setEndpoint(String endpointAsString) {
        this.endpointAsString = endpointAsString;
        return this;
    }

    public MyOtlpGrpcSpanExporterBuilder setTrustedCertificates(byte[] trustedCertificatesPem) {
        this.trustedCertificatesPem = trustedCertificatesPem.clone();
        return this;
    }

    public MyOtlpGrpcSpanExporterBuilder addHeader(String key, String value) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.put(Metadata.Key.of(key, ASCII_STRING_MARSHALLER), value);
        return this;
    }

    public MyOtlpGrpcSpanExporter build() {
        URI endpoint = getEndpointAsUri();
        final ManagedChannelBuilder<?> managedChannelBuilder =
                ManagedChannelBuilder.forTarget(endpoint.getAuthority());

        if (endpoint.getScheme().equals("https")) {
            managedChannelBuilder.useTransportSecurity();
        } else {
            managedChannelBuilder.usePlaintext();
        }

        if (metadata != null) {
            managedChannelBuilder.intercept(MetadataUtils.newAttachHeadersInterceptor(metadata));
        }

        if (trustedCertificatesPem != null) {
            try {
                SslUtil.setTrustedCertificatesPem(managedChannelBuilder, trustedCertificatesPem);
            } catch (SSLException e) {
                throw new IllegalStateException(
                        "Could not set trusted certificates for gRPC TLS connection, are they valid "
                                + "X.509 in PEM format?",
                        e);
            }
        }

        ManagedChannel channel = managedChannelBuilder.build();


        OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder()
                .setChannel(channel)
                .setTimeout(this.timeoutNanos, TimeUnit.NANOSECONDS)
                .build();
        return new MyOtlpGrpcSpanExporter(otlpGrpcSpanExporter, channel);
    }

    /**
     * @see OtlpGrpcSpanExporterBuilder#setEndpoint(String)
     */
    @Nonnull
    private URI getEndpointAsUri() {
        requireNonNull(endpointAsString, "endpoint");
        URI uri;
        try {
            uri = new URI(endpointAsString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint, must be a URL: " + endpointAsString, e);
        }

        if (uri.getScheme() == null
                || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
            throw new IllegalArgumentException(
                    "Invalid endpoint, must start with http:// or https://: " + uri);
        }
        return uri;
    }
}
