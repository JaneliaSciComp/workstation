package org.janelia.jacs2.service.impl;

import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class JacsJobRunner {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsTaskDispatcher jacsTaskDispatcher;
    private final ScheduledExecutorService scheduler;

    public JacsJobRunner() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    private void doWork() {
        try {
            logger.debug("Dispatch JACS jobs");
            jacsTaskDispatcher.dispatchServices();
        } catch (Exception e) {
            logger.error("Critical error - job dispatch failed", e);
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
