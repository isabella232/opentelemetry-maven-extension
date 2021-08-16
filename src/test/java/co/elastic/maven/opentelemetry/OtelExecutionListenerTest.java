/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OtelExecutionListenerTest {

    @Test
    public void getPluginArtifactIdShortName_builtinPluginName() {
        OtelExecutionListener otelEventSpy = new OtelExecutionListener();
        String actual = otelEventSpy.getPluginArtifactIdShortName("maven-clean-plugin");
        String expected  = "clean";
        assertEquals(expected, actual);
    }

    @Test
    public void getPluginArtifactIdShortName_thirdPartyPluginName() {
        OtelExecutionListener otelEventSpy = new OtelExecutionListener();
        String actual = otelEventSpy.getPluginArtifactIdShortName("spotbugs-maven-plugin");
        String expected  = "spotbugs";
        assertEquals(expected, actual);
    }
}