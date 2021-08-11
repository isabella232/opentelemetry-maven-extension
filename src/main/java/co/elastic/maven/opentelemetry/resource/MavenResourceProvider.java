/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package co.elastic.maven.opentelemetry.resource;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.maven.rtinfo.internal.DefaultRuntimeInformation;

public class MavenResourceProvider implements ResourceProvider {
    @Override
    public Resource createResource(ConfigProperties config) {
        // FIXME better solution to get the maven version
        final String mavenVersion = new DefaultRuntimeInformation().getMavenVersion();
        Attributes attributes = Attributes.of(ResourceAttributes.SERVICE_NAME, "maven", ResourceAttributes.SERVICE_VERSION, mavenVersion);
        System.err.println("MAVEN RESOURCE " + attributes.asMap());
        return Resource.create(attributes);
    }
}
