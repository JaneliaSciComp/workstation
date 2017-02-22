package org.janelia.jacs2.asyncservice.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
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

    private final JacsServiceQueue jacsServiceQueue;
    private final ScheduledExecutorService scheduler;
    private final Logger logger;
    private int initialDelay;
    private int period;

    @Inject
    JacsQueueSyncer(JacsServiceQueue jacsServiceQueue,
                    @PropertyValue(name = "service.queue.InitialDelayInSeconds") int initialDelay,
                    @PropertyValue(name = "service.queue.PeriodInSeconds") int period,
                    Logger logger) {
        this.jacsServiceQueue = jacsServiceQueue;
        this.initialDelay = initialDelay == 0 ? 30 : initialDelay;
        this.period = period == 0 ? 10 : period;
        this.logger = logger;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("JACS-SYNC-%d")
                .setDaemon(true)
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1, threadFactory);
    }

    private void doWork() {
        try {
            jacsServiceQueue.refreshServiceQueue();
        } catch (Exception e) {
            logger.error("Critical error - syncing job queue failed", e);
        }
    }

    @PostConstruct
    public void initialize() {
        scheduler.scheduleAtFixedRate(() -> doWork(), initialDelay, period, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }
}
