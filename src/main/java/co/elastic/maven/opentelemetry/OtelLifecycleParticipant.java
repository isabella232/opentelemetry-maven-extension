/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add the {@link OtelExecutionListener} to the lifecycle of the MAven execution
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class OtelLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement(role = ExecutionListener.class, hint = "otel-execution-listener")
    private OtelExecutionListener otelExecutionListener;

    @Requirement
    private OpenTelemetrySdkService openTelemetrySdkService;

    /**
     * For an unknown reason, {@link #afterProjectsRead(MavenSession)} is invoked when the module is declared as an extension in pom.xml but {@link #afterSessionStart(MavenSession)} is not invoked
     */
    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        this.openTelemetrySdkService.getTracer(); // TODO remove force initialization
        OtelExecutionListener.registerOtelExecutionListener(session, this.otelExecutionListener);
        logger.debug("OpenTelemetry: afterProjectsRead");
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        this.openTelemetrySdkService.getTracer(); // TODO remove force initialization
        OtelExecutionListener.registerOtelExecutionListener(session, this.otelExecutionListener);
        logger.debug("OpenTelemetry: afterSessionStart");
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {

    }
}
