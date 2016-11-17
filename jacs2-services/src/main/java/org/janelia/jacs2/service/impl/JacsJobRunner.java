package org.janelia.jacs2.service.impl;

import org.slf4j.Logger;

import javax.ejb.AccessTimeout;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

@Singleton
public class JacsJobRunner {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsServiceDispatcher jacsServiceDispatcher;

    @AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
    @Schedule(second = "*/60", minute = "*", hour = "*", persistent = false)
    public void doWork() {
        try {
            logger.debug("Dispatch JACS jobs");
            jacsServiceDispatcher.dispatchServices();
        } catch (Exception e) {
            logger.error("Critical error - job dispatch failed", e);
        }
    }
}
