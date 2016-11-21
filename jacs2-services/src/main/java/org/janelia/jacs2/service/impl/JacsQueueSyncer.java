package org.janelia.jacs2.service.impl;

import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.interceptor.InvocationContext;
import java.util.concurrent.TimeUnit;

@Startup
@Singleton
public class JacsQueueSyncer {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher jacsTaskDispatcher;
    @Resource
    private ManagedScheduledExecutorService executorService;

    public void doWork() {
        try {
            logger.debug("Sync JACS jobs");
            jacsTaskDispatcher.syncServiceQueue();
        } catch (Exception e) {
            logger.error("Critical error - syncing job queue failed", e);
        }
    }

    @PostConstruct
    public void initialize(InvocationContext ctx) {
        executorService.scheduleAtFixedRate(() -> doWork(), 30L, 30L, TimeUnit.SECONDS);
    }

}
