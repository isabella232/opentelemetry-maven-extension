/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class OtelUtilsTest {

    @Test
    public void test_getCommaSeparatedMap_resources() {
        String resourceAttributes = "service.name=frontend,service.namespace=com-mycompany-mydomain,service.version=1.0,deployment.environment=production";
        final Map<String, String> actualResourceAttributes = OtelUtils.getCommaSeparatedMap(resourceAttributes);

        assertEquals("frontend", actualResourceAttributes.get("service.name"));
        assertEquals("com-mycompany-mydomain", actualResourceAttributes.get("service.namespace"));
        assertEquals("1.0", actualResourceAttributes.get("service.version"));
        assertEquals("production", actualResourceAttributes.get("deployment.environment"));

    }

}