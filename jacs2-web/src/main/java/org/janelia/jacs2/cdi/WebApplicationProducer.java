package org.janelia.jacs2.cdi;

import org.janelia.jacs2.job.BackgroundJobs;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class WebApplicationProducer {

    @Singleton
    @Produces
    public BackgroundJobs jobs() {
        return new BackgroundJobs();
    }
}
