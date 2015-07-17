package org.janelia.jos;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.basic.BasicAuthFactory;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.bson.types.ObjectId;
import org.janelia.jos.auth.SimpleAuthenticator;
import org.janelia.jos.auth.SimplePrincipal;
import org.janelia.jos.mongo.MongoHealthCheck;
import org.janelia.jos.mongo.MongoManaged;
import org.janelia.jos.scality.ScalityDAO;
import org.janelia.jos.tasks.ManagedPeriodicTask;
import org.janelia.jos.tasks.ScalityDeletionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Janelia Object Store REST API. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MicroJOSApplication extends Application<MicroJOSConfiguration> {
    
    private static final Logger log = LoggerFactory.getLogger(MicroJOSApplication.class);
    
    public static void main(String[] args) throws Exception {
        new MicroJOSApplication().run(args);
    }

    @Override
    public String getName() {
        return "microjosapi";
    }

    @Override
    public void initialize(Bootstrap<MicroJOSConfiguration> bootstrap) {
        bootstrap.addBundle(new SwaggerBundle<MicroJOSConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(MicroJOSConfiguration configuration) {
                return configuration.getSwaggerConfiguration();
            }
        });
    }
    
    @Override
    public void run(MicroJOSConfiguration config, Environment environment) {

        // Mongo
        MongoManaged mongoManaged = new MongoManaged(config.getMongoConfiguration());
        environment.lifecycle().manage(mongoManaged);
        MongoHealthCheck healthCheck = new MongoHealthCheck(mongoManaged);
        environment.healthChecks().register("mongo", healthCheck);
        
        // Authentication
        environment.jersey().register(AuthFactory.binder(
                new BasicAuthFactory<SimplePrincipal>(new SimpleAuthenticator(),"Basic Realm",SimplePrincipal.class)));
        
        // JSON Serialization 
        ObjectMapper objectMapper = new CustomObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        
        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
                
        // TODO: This registers the object mapper but it throws a warning about not being able to overwrite the existing JacksonMessageBodyProvider.
        // It works, but there should be a nicer way to do this. 
        environment.jersey().register(new JacksonMessageBodyProvider(objectMapper, environment.getValidator()));

        // HTTP client
        HttpClient httpClient = new HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build(getName());
        ScalityDAO scality = new ScalityDAO(config.getScalityConfiguration(), httpClient);

        // Resources
        JOSObjectResource resource = new JOSObjectResource(mongoManaged, objectMapper, scality);
        environment.jersey().register(resource);

        // Scheduled tasks
        final ScalityDeletionTask periodicTask = new ScalityDeletionTask(mongoManaged, scality);
        final Managed managedImplementer = new ManagedPeriodicTask(periodicTask);
        environment.lifecycle().manage(managedImplementer);

        // Metrics
//        final JmxReporter reporter = JmxReporter.forRegistry(environment.metrics()).build();
//        reporter.start();
        
//        final Graphite graphite = new Graphite(new InetSocketAddress("localhost", 8090));
//        final GraphiteReporter reporter = GraphiteReporter.forRegistry(environment.metrics())
//                                                          .prefixedWith("rokickik-wm1")
//                                                          .convertRatesTo(TimeUnit.SECONDS)
//                                                          .convertDurationsTo(TimeUnit.MILLISECONDS)
//                                                          .filter(MetricFilter.ALL)
//                                                          .build(graphite);
//        reporter.start(1, TimeUnit.MINUTES);
        
        log.info("Janelia Object Store API is ready");
    }
    
    public class CustomObjectMapper extends ObjectMapper {
        public CustomObjectMapper() {
            SimpleModule module = new SimpleModule("ObjectIdModule");
            // Creates nice representations of MongoDB ObjectIds
            module.addSerializer(ObjectId.class, new ObjectIdSerializer());
            module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
            this.registerModule(module);
        }
    }
    
    public class ObjectIdSerializer extends JsonSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }

    public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new ObjectId(p.getText());
        }
    }
    
}
