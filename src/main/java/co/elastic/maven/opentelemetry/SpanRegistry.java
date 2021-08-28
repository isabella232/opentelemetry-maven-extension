/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package co.elastic.maven.opentelemetry;

import io.opentelemetry.api.trace.Span;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Holds the state of the spans in progress
 * FIXME support multi module projects
 */
@Component(role = SpanRegistry.class)
public class SpanRegistry {

    private Span rootSpan;

    private final Map<MojoExecutionKey, Span> mojoExecutionKeySpanMap = new HashMap<>();
    private final Map<MavenProjectKey, Span> mavenProjectKeySpanMap = new HashMap<>();

    @Nullable
    public Span getRootSpan() {
        return rootSpan;
    }

    @Nonnull
    public Span getSpan(@Nonnull MavenProject mavenProject) {
        final MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
        final Span span = this.mavenProjectKeySpanMap.get(key);
        if (span == null) {
            throw new IllegalStateException("Span not started for project " + mavenProject.getGroupId() + ":" + mavenProject.getArtifactId());
        }
        return span;
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

    public void putSpan(@Nonnull Span span, @Nonnull MavenProject mavenProject) {
        MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
        Span previousSpanForKey = mavenProjectKeySpanMap.put(key, span);
        if (previousSpanForKey != null) {
            throw new IllegalStateException();
        }
    }

    @Nonnull
    public Span removeSpan(@Nonnull MavenProject mavenProject) throws IllegalStateException {
        MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
        Span span = mavenProjectKeySpanMap.remove(key);
        if (span == null) {
            throw new IllegalStateException();
        }
        return span;
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
            throw new IllegalStateException("Root span already defined " + this.rootSpan);
        }
        this.rootSpan = rootSpan;
    }

    private static class MavenProjectKey {
        final String groupId;
        final String artifactId;

        @Nonnull
        public static MavenProjectKey fromMavenProject(@Nonnull MavenProject mavenProject) {
            return new MavenProjectKey(mavenProject.getGroupId(), mavenProject.getArtifactId());
        }

        private MavenProjectKey(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MavenProjectKey that = (MavenProjectKey) o;
            return groupId.equals(that.groupId) && artifactId.equals(that.artifactId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId);
        }

        @Override
        public String toString() {
            return "MavenProjectKey{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    '}';
        }
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
