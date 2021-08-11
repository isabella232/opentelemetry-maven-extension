/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry.eventspy;

import co.elastic.maven.opentelemetry.SpanRegistry;
import co.elastic.maven.opentelemetry.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;

@Named
@Singleton
public class OtelEventSpy extends AbstractEventSpy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private SpanRegistry spanRegistry;

    @Override
    public void init(EventSpy.Context context) throws Exception {
        // https://github.com/open-telemetry/opentelemetry-java/blob/v1.4.1/sdk-extensions/autoconfigure/README.md#otlp-exporter-both-span-and-metric-exporters
        String otelExporterOtlpEndpointSysProperty= System.getProperty("otel.exporter.otlp.endpoint");
        String otelExporterOtlpEndpointEnvVar = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");

        if (otelExporterOtlpEndpointSysProperty == null && otelExporterOtlpEndpointEnvVar == null) {
            // see https://github.com/open-telemetry/opentelemetry-java/blob/v1.4.1/sdk-extensions/autoconfigure/README.md#logging-exporter
            System.setProperty("otel.traces.exporter", "logging");
            logger.warn("OpenTelemetry configured with Logging exporter"); // FIXME lower logging level
        } else {
            logger.warn("OpenTelemetry configured with environment variables (OTEL_EXPORTER_OTLP_ENDPOINT: " + otelExporterOtlpEndpointEnvVar + "...)"); // FIXME lower logging level
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
                MavenProject currentProject = executionEvent.getSession().getCurrentProject();
                TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
                    @Override
                    public Iterable<String> keys(Map<String, String> environmentVariables) {
                        return environmentVariables.keySet();
                    }

                    @Nullable
                    @Override
                    public String get(@Nullable Map<String, String> environmentVariables, String key) {
                        return environmentVariables == null ? null : environmentVariables.get(key.toUpperCase(Locale.ROOT));
                    }
                };
                io.opentelemetry.context.Context context = W3CTraceContextPropagator.getInstance().extract(io.opentelemetry.context.Context.current(), System.getenv(), getter);
                try (Scope scope = context.makeCurrent()) {
                    Span span = tracer.spanBuilder(currentProject.getGroupId() + ":" + currentProject.getArtifactId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID, currentProject.getGroupId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID, currentProject.getArtifactId())
                            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION, currentProject.getVersion())
                            .startSpan();
                    spanRegistry.setRootSpan(span);
                }
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
                                    getPluginArtifactIdShortName(mojoExecution.getArtifactId()) + ":" + mojoExecution.getGoal() +
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

    /**
     * maven-clean-plugin -> clean
     * sisu-maven-plugin -> sisu
     * spotbugs-maven-plugin -> spotbugs
     *
     * @param pluginArtifactId
     * @return
     */
    @Nonnull
    protected String getPluginArtifactIdShortName(@Nonnull String pluginArtifactId) {
        if (pluginArtifactId.endsWith("-maven-plugin")) {
            return pluginArtifactId.substring(0, pluginArtifactId.length() - "-maven-plugin".length());
        } else if (pluginArtifactId.startsWith("maven-") && pluginArtifactId.endsWith("-plugin")) {
            return pluginArtifactId.substring("maven-".length(), pluginArtifactId.length() - "-plugin".length());
        } else {
            return pluginArtifactId;
        }
    }

    @Override
    public void close() {

    }

}
