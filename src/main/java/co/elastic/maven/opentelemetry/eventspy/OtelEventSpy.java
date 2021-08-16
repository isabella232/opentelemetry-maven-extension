/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry.eventspy;

import co.elastic.maven.opentelemetry.ChainedExecutionListener;
import co.elastic.maven.opentelemetry.OtelExecutionListener;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Note that when using an {@link org.apache.maven.execution.ExecutionListener} instead of an {@link AbstractEventSpy},
 * the events are not invoked during the execution when the extension is attached using `${maven.home}/lib/ext`
 */
// @Component(role = EventSpy.class, hint = "otel-event-spy")
public class OtelEventSpy extends AbstractEventSpy {

    @Requirement(hint = "otel-execution-listener")
    private OtelExecutionListener otelExecutionListener;

    @Override
    public void onEvent(Object event) throws Exception {
        super.onEvent(event);
        System.err.println("OtelEventSpy - " + event);
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            switch (executionEvent.getType()) {
                case ProjectDiscoveryStarted:
                case SessionStarted:
                    OtelExecutionListener.registerOtelExecutionListener(executionEvent.getSession(), this.otelExecutionListener);
                    break;
                default:
                    // skip
            }
        }
    }
}
