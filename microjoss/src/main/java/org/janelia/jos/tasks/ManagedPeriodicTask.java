package org.janelia.jos.tasks;

import io.dropwizard.lifecycle.Managed;

import com.google.common.util.concurrent.AbstractScheduledService;

/**
 * From http://seerealized.com/blog/2015/02/13/running-background-tasks-in-dropwizard-with-guava/
 */
public class ManagedPeriodicTask implements Managed {

    private final AbstractScheduledService periodicTask;

    public ManagedPeriodicTask(AbstractScheduledService periodicTask) {
        this.periodicTask = periodicTask;
    }

    @Override
    public void start() throws Exception {
        periodicTask.startAsync().awaitRunning();
    }

    @Override
    public void stop() throws Exception {
        periodicTask.stopAsync().awaitTerminated();
    }
}

