package org.janelia.jacs2.cdi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
