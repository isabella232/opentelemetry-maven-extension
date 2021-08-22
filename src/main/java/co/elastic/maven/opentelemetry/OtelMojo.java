/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * See https://github.com/socram8888/ascii-banner-maven-plugin/blob/c8d8d471ce6fbe74e12df4b374bf5f755bd12ee5/src/main/java/pet/orca/maven/asciibanner/AsciiBannerSetupMojo.java
 */
@Mojo(name = "otel", defaultPhase = LifecyclePhase.INITIALIZE, instantiationStrategy = InstantiationStrategy.SINGLETON)
public class OtelMojo extends AbstractMojo {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * {@code OTEL_EXPORTER_OTLP_ENDPOINT}
     */
    @Parameter(property = "otel.endpoint")
    private String endpoint;

    /**
     * {@code OTEL_EXPORTER_OTLP_HEADERS}
     */
    @Parameter(property = "otel.headers")
    private String headers;

    @Parameter(property = "otel.timeout")
    private long timeout;

    /**
     * See https://github.com/socram8888/ascii-banner-maven-plugin/blob/c8d8d471ce6fbe74e12df4b374bf5f755bd12ee5/src/main/java/pet/orca/maven/asciibanner/AsciiBannerSetupMojo.java
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Requirement
    private OtelExecutionListener otelExecutionListener;

    /**
     * https://github.com/open-telemetry/opentelemetry-java/blob/v1.4.1/sdk-extensions/autoconfigure/README.md#logging-exporter
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties properties = new Properties();
        if (StringUtils.isNotBlank(this.endpoint)) {
            properties.setProperty("otel.exporter.otlp.endpoint", this.endpoint);
        }
        if (StringUtils.isNotBlank(this.headers)) {
            properties.setProperty("otel.exporter.otlp.headers", this.headers);
        }
        if (timeout != 0) {
            properties.setProperty("otel.exporter.otlp.timeout", Long.toString(this.timeout));
        }

        OtelExecutionListener.registerOtelExecutionListener(mavenSession, this.otelExecutionListener);

        logger.warn("Set properties " + properties);
    }
}
