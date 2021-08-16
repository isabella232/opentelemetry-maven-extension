/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * see https://github.com/odavid/maven-plugins/blob/d1c013a0c4adbe21ec7a57ac39bed8d5376a39a0/maven-logging-extension/src/main/java/com/github/ohaddavid/maven/extensions/LoggingLifecycleParticipant.java
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class OtelLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Requirement(role = ExecutionListener.class, hint = "otel-execution-listener")
    private OtelExecutionListener otelExecutionListener;

    /**
     * For an unknown reason, {@link #afterProjectsRead(MavenSession)} is invoked when the module is declared as an extension in pom.xml but {@link #afterSessionStart(MavenSession)} is not invoked
     */
    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        OtelExecutionListener.registerOtelExecutionListener(session, this.otelExecutionListener);
        logger.debug("afterProjectsRead");
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        OtelExecutionListener.registerOtelExecutionListener(session, this.otelExecutionListener);
        logger.debug("afterSessionStart");
    }

    /*
    [INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.773 s
[INFO] Finished at: 2021-08-13T18:03:24+02:00
[INFO] ------------------------------------------------------------------------
Exception in thread "BatchSpanProcessor_WorkerThread-1" java.lang.NoClassDefFoundError: io/grpc/internal/ManagedChannelImpl$1NotifyStateChanged
        at io.grpc.internal.ManagedChannelImpl.notifyWhenStateChanged(ManagedChannelImpl.java:1316)
        at io.grpc.internal.ForwardingManagedChannel.notifyWhenStateChanged(ForwardingManagedChannel.java:78)
        at io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter.shutdown(OtlpGrpcSpanExporter.java:183)
        at io.opentelemetry.sdk.trace.export.BatchSpanProcessor$Worker.lambda$shutdown$2(BatchSpanProcessor.java:256)
        at io.opentelemetry.sdk.common.CompletableResultCode.succeed(CompletableResultCode.java:85)
        at io.opentelemetry.sdk.trace.export.BatchSpanProcessor$Worker.flush(BatchSpanProcessor.java:241)
        at io.opentelemetry.sdk.trace.export.BatchSpanProcessor$Worker.run(BatchSpanProcessor.java:204)
        at java.base/java.lang.Thread.run(Thread.java:829)
Caused by: java.lang.ClassNotFoundException: io.grpc.internal.ManagedChannelImpl$1NotifyStateChanged
        at org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy.loadClass(SelfFirstStrategy.java:50)
        at org.codehaus.plexus.classworlds.realm.ClassRealm.unsynchronizedLoadClass(ClassRealm.java:271)
        at org.codehaus.plexus.classworlds.realm.ClassRealm.loadClass(ClassRealm.java:247)
        at org.codehaus.plexus.classworlds.realm.ClassRealm.loadClass(ClassRealm.java:239)
        ... 8 more

     */
    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        super.afterSessionEnd(session);
    }
}
