package org.janelia.jacs2.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.jacs2.cdi.qualifier.ApplicationProperties;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dao.mongo.utils.TimebasedIdentifierGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Properties;

public class ApplicationProducer {

    @Singleton
    @Produces
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.instance().getDefaultObjectMapper();
    }

    @Singleton
    @Produces
    public TimebasedIdentifierGenerator idGenerator(@PropertyValue(name = "TimebasedIdentifierGenerator.DeploymentContext") Integer deploymentContext) {
        return new TimebasedIdentifierGenerator(deploymentContext);
    }

    @Produces
    @PropertyValue(name = "")
    public String stringPropertyValue(@ApplicationProperties Properties properties, InjectionPoint injectionPoint) {
        final PropertyValue property = injectionPoint.getAnnotated().getAnnotation(PropertyValue.class);
        return properties.getProperty(property.name());
    }

    @Produces
    @PropertyValue(name = "")
    public Integer integerPropertyValue(@ApplicationProperties Properties properties, InjectionPoint injectionPoint) {
        String stringValue = stringPropertyValue(properties, injectionPoint);
        return stringValue == null ? null : Integer.valueOf(stringValue);
    }

    @ApplicationScoped
    @ApplicationProperties
    @Produces
    public Properties properties() throws IOException {
        return new ApplicationPropertiesProvider()
                .fromDefaultResource()
                .fromEnvVar("JACS2_CONFIG")
                .build();
    }

}
