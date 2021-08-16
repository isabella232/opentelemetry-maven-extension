/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry.surefire;

import co.elastic.maven.opentelemetry.SpanRegistry;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.TestSetReportEntry;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME how to register this listener?
 */
@Component(role = OtelRunListener.class)
public class OtelRunListener implements RunListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement
    private SpanRegistry spanRegistry;

    @Override
    public void testSetStarting(TestSetReportEntry report) {

    }

    @Override
    public void testSetCompleted(TestSetReportEntry report) {

    }

    @Override
    public void testStarting(ReportEntry report) {
        logger.warn("testStarting(" + report + ")");
    }

    @Override
    public void testSucceeded(ReportEntry report) {
        logger.warn("testSucceeded(" + report + ")");
    }

    @Override
    public void testAssumptionFailure(ReportEntry report) {
        logger.warn("testAssumptionFailure(" + report + ")");
    }

    @Override
    public void testError(ReportEntry report) {
        logger.warn("testError(" + report + ")");
    }

    @Override
    public void testFailed(ReportEntry report) {
        logger.warn("testFailed(" + report + ")");
    }

    @Override
    public void testSkipped(ReportEntry report) {
        logger.warn("testSkipped(" + report + ")");
    }

    @Override
    public void testExecutionSkippedByUser() {

    }
}
