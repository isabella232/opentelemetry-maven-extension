/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package co.elastic.maven.opentelemetry;

import io.opentelemetry.api.trace.Span;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component(role = SpanRegistry.class)
public class SpanRegistry {

    private Span rootSpan;

    private final Map<MojoExecutionKey, Span> mojoExecutionKeySpanMap = new HashMap<>();

    @Nullable
    public Span getRootSpan() {
        return rootSpan;
    }

    @Nonnull
    public Span getRootSpanNotNull() {
        if (rootSpan == null) {
            throw new IllegalStateException("Root span not defined");
        }
        return rootSpan;
    }

    @Nonnull
    public Span removeRootSpan() {
        if (rootSpan == null) {
            throw new IllegalStateException("Root span not defined");
        }
        if (!this.mojoExecutionKeySpanMap.isEmpty()) {
            throw new IllegalStateException("Remaining children spans: " + this.mojoExecutionKeySpanMap.keySet().stream().map(MojoExecutionKey::toString).collect(Collectors.joining(", ")));
        }
        return rootSpan;
    }

    public void putSpan(@Nonnull Span span, @Nonnull MojoExecution mojoExecution) {
        MojoExecutionKey key = MojoExecutionKey.fromMojoExecution(mojoExecution);
        Span previousSpanForKey = mojoExecutionKeySpanMap.put(key, span);
        if (previousSpanForKey != null) {
            throw new IllegalStateException();
        }
    }

    @Nonnull
    public Span removeSpan(@Nonnull MojoExecution mojoExecution) throws IllegalStateException {
        MojoExecutionKey key = MojoExecutionKey.fromMojoExecution(mojoExecution);
        Span span = mojoExecutionKeySpanMap.remove(key);
        if (span == null) {
            throw new IllegalStateException();
        }
        return span;
    }

    /**
     * @param rootSpan
     * @throws IllegalStateException Root span already defined
     */
    public void setRootSpan(@Nonnull Span rootSpan) throws IllegalStateException {
        if (this.rootSpan != null) {
            throw new IllegalStateException("Root span already defined");
        }
        this.rootSpan = rootSpan;
    }

    private static class MojoExecutionKey {
        final String executionId;
        final String goal;
        final String groupId;
        final String artifactId;
        final String pluginGroupId;
        final String pluginArtifactId;

        @Nonnull
        public static MojoExecutionKey fromMojoExecution(@Nonnull MojoExecution mojoExecution) {
            if (mojoExecution == null) {
                throw new NullPointerException("Given MojoExecution is null");
            }
            Plugin plugin = mojoExecution.getPlugin();
            if (plugin == null) {
                throw new NullPointerException("Plugin is null for MojoExecution " + mojoExecution.identify());

            }
            return new MojoExecutionKey(mojoExecution.getExecutionId(),
                    mojoExecution.getGoal(),
                    mojoExecution.getGroupId(),
                    mojoExecution.getArtifactId(),
                    plugin.getGroupId(),
                    plugin.getArtifactId());
        }

        public MojoExecutionKey(String executionId, String goal, String groupId, String artifactId, String pluginGroupId, String pluginArtifactId) {
            this.executionId = executionId;
            this.goal = goal;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.pluginGroupId = pluginGroupId;
            this.pluginArtifactId = pluginArtifactId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MojoExecutionKey that = (MojoExecutionKey) o;
            return Objects.equals(executionId, that.executionId) && Objects.equals(goal, that.goal) && Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(pluginGroupId, that.pluginGroupId) && Objects.equals(pluginArtifactId, that.pluginArtifactId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(executionId, goal, groupId, artifactId, pluginGroupId, pluginArtifactId);
        }

        @Override
        public String toString() {
            return "MojoExecutionKey{" +
                    "executionId='" + executionId + '\'' +
                    ", goal='" + goal + '\'' +
                    ", groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", pluginGroupId='" + pluginGroupId + '\'' +
                    ", pluginArtifactId='" + pluginArtifactId + '\'' +
                    '}';
        }
    }
}
