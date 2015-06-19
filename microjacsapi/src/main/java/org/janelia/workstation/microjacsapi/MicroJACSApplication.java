package org.janelia.workstation.microjacsapi;

import io.dropwizard.Application;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MicroJACSApplication extends Application<MicroJACSConfiguration> {
    
    private static final Logger log = LoggerFactory.getLogger(MicroJACSApplication.class);
    
    public static void main(String[] args) throws Exception {
        new MicroJACSApplication().run(args);
    }

    @Override
    public String getName() {
        return "microjacsapi";
    }

    @Override
    public void initialize(Bootstrap<MicroJACSConfiguration> bootstrap) {
        // nothing to do yet
    }
    
    @Override
    public void run(MicroJACSConfiguration configuration, Environment environment) {
                
        MongoManaged mongoManaged = new MongoManaged(configuration);
        environment.lifecycle().manage(mongoManaged);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        
        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        
        environment.jersey().register(new JacksonMessageBodyProvider(objectMapper, environment.getValidator()));
        
        MongoHealthCheck healthCheck = new MongoHealthCheck(mongoManaged);
        environment.healthChecks().register("mongo", healthCheck);
        
        DomainObjectResource resource = new DomainObjectResource(mongoManaged, objectMapper);
        environment.jersey().register(resource);
    }
}
