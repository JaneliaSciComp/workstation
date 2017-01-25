package org.janelia.jacs2.asyncservice.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Singleton
public class JacsQueueSyncer {

    private final JacsServiceDispatcher jacsServiceDispatcher;
    private final ScheduledExecutorService scheduler;
    private final Logger logger;

    @Inject
    JacsQueueSyncer(JacsServiceDispatcher jacsServiceDispatcher, Logger logger) {
        this.jacsServiceDispatcher = jacsServiceDispatcher;
        this.logger = logger;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("JACS-SYNC-%d")
                .setDaemon(true)
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1, threadFactory);
    }

    private void doWork() {
        try {
            jacsServiceDispatcher.syncServiceQueue();
        } catch (Exception e) {
            logger.error("Critical error - syncing job queue failed", e);
        }
    }

    @PostConstruct
    public void initialize() {
        scheduler.scheduleAtFixedRate(() -> doWork(), 30L, 10L, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }
}
