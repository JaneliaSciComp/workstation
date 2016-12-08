package org.janelia.jacs2.cdi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class ObjectMapperFactory {
    private static final ObjectMapperFactory INSTANCE = new ObjectMapperFactory();

    private final ObjectMapper objectMapper;

    private ObjectMapperFactory() {
        objectMapper = new ObjectMapper()
                .registerModule(new JodaModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static ObjectMapperFactory instance() {
        return INSTANCE;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
