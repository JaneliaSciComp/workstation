package org.janelia.it.workstation.browser.api.sage_responder;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class RESTClientManager {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("sageResponder.rest.url");
    
    // Singleton
    private static RESTClientManager instance;
    public static RESTClientManager getInstance() {
        if (instance==null) {
            instance = new RESTClientManager();
        }
        return instance;
    }
    
    private Client client;
    private String serverUrl;

    private RESTClientManager() {
        this(REMOTE_API_URL);
    }

    public RESTClientManager(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("Using server URL: {}",serverUrl);
        client = ClientBuilder.newClient();
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                log.error("Failed to deserialize property which does not exist in model: {}.{}",beanOrClass.getClass().getName(),propertyName);
                return true;
            }
        });

        client.register(provider);
    }

    public WebTarget getPublishingInfoLineEndpoint() {
        return client.target(serverUrl + "publishinginfo/line/");
    }
    
}
