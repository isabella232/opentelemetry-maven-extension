package co.elastic.maven.opentelemetry.eventspy;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class OtelEventSpyTest {

    @Test
    public void getPluginArtifactIdShortName_builtinPluginName() {
        OtelEventSpy otelEventSpy = new OtelEventSpy();
        String actual = otelEventSpy.getPluginArtifactIdShortName("maven-clean-plugin");
        String expected  = "clean";
        assertEquals(expected, actual);
    }

    @Test
    public void getPluginArtifactIdShortName_thirdPartyPluginName() {
        OtelEventSpy otelEventSpy = new OtelEventSpy();
        String actual = otelEventSpy.getPluginArtifactIdShortName("spotbugs-maven-plugin");
        String expected  = "spotbugs";
        assertEquals(expected, actual);
    }
}