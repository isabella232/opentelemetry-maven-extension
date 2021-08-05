/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven;

import co.elastic.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class OtelEventSpy extends AbstractEventSpy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpanRegistry spanRegistry = new SpanRegistry();

    @Override
    public void init(EventSpy.Context context) throws Exception {

        String otelExporterOtlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (otelExporterOtlpEndpoint == null) {
            SpanExporter spanExporter = new LoggingSpanExporter();
            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spanExporter)).build();

            OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).buildAndRegisterGlobal();
            logger.warn("OpenTelemetry configured with Logging exporter");
        } else {
            logger.info("OpenTelemetry configured with environment variables (OTEL_EXPORTER_OTLP_ENDPOINT: " + otelExporterOtlpEndpoint + "...)");
        }
    }

    @Override
    public void onEvent(Object event) throws Exception {
        try {

            if (event instanceof ExecutionEvent) {
                ExecutionEvent executionEvent = (ExecutionEvent) event;


                onExecutionEvent(executionEvent);


            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void onExecutionEvent(ExecutionEvent executionEvent) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.contrib.maven");

        switch (executionEvent.getType()) {

            case ProjectDiscoveryStarted:
                break;
            case SessionStarted: {
                logger.warn("Session started");
                // TODO handle TRACEPARENT
                MavenProject currentProject = executionEvent.getSession().getCurrentProject();
                Span span = tracer.spanBuilder(currentProject.getGroupId() + ":" + currentProject.getArtifactId())
                        .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID, currentProject.getGroupId())
                        .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID, currentProject.getArtifactId())
                        .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION, currentProject.getVersion())
                        .startSpan();
                spanRegistry.setRootSpan(span);
            }
            break;
            case SessionEnded: {
                spanRegistry.removeRootSpan().end();
            }
            break;
            case ProjectSkipped:
            case ProjectStarted:
            case ProjectSucceeded:
            case ProjectFailed:
            case MojoSkipped: {
                MojoExecution mojoExecution = executionEvent.getMojoExecution();
                if (mojoExecution == null) {
                    logger.error("No MojoExecution found for MojoSkipped: " + executionEvent.getException());
                } else {
                    Span mojoExecutionSpan = spanRegistry.removeSpan(mojoExecution);
                    mojoExecutionSpan.setStatus(StatusCode.ERROR, "Mojo Skipped");
                    mojoExecutionSpan.end();
                }
            }
            break;
            case MojoStarted: {
                MojoExecution mojoExecution = executionEvent.getMojoExecution();

                Span rootSpan = spanRegistry.getRootSpan();
                try (Scope scope = rootSpan.makeCurrent()) {

                    Span span = tracer.spanBuilder(
                                    mojoExecution.getPlugin().getArtifactId() + ":" + mojoExecution.getGoal() +
                                            " (" + executionEvent.getMojoExecution().getExecutionId() + ")" +
                                            " @ " + executionEvent.getProject().getArtifactId() + " ")

                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID, executionEvent.getProject().getGroupId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID, executionEvent.getProject().getArtifactId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION, executionEvent.getProject().getVersion())

                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PLUGIN_GROUP_ID, mojoExecution.getPlugin().getGroupId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PLUGIN_ARTIFACT_ID, mojoExecution.getPlugin().getArtifactId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PLUGIN_VERSION, mojoExecution.getPlugin().getVersion())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_EXECUTION_GOAL, mojoExecution.getGoal())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_EXECUTION_ID, mojoExecution.getExecutionId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_EXECUTION_LIFECYCLE_PHASE, mojoExecution.getLifecyclePhase())
                            .startSpan();
                    spanRegistry.putSpan(span, mojoExecution);
                }
            }
            break;
            case MojoSucceeded: {
                MojoExecution mojoExecution = executionEvent.getMojoExecution();
                Span mojoExecutionSpan = spanRegistry.removeSpan(mojoExecution);
                mojoExecutionSpan.setStatus(StatusCode.OK);

                mojoExecutionSpan.end();
            }
            break;
            case MojoFailed: {
                MojoExecution mojoExecution = executionEvent.getMojoExecution();
                Span mojoExecutionSpan = spanRegistry.removeSpan(mojoExecution);
                mojoExecutionSpan.setStatus(StatusCode.ERROR, "Mojo Failed");
                mojoExecutionSpan.end();
            }
            break;
            case ForkStarted:
            case ForkSucceeded:
            case ForkFailed:
            case ForkedProjectStarted:
            case ForkedProjectSucceeded:
            case ForkedProjectFailed:
        }
    }


    @Override
    public void close() {

    }

}
