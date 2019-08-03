package org.janelia.workstation.core.api.http;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RestJsonClientManager {

    private static final Logger log = LoggerFactory.getLogger(RestJsonClientManager.class);

    // Singleton
    private static RestJsonClientManager instance;

    public static RestJsonClientManager getInstance() {
        if (instance == null) {
            instance = new RestJsonClientManager();
        }
        return instance;
    }

    private LoadingCache<String, Boolean> failureCache;
    private Client authClient;
    private Client client;

    public RestJsonClientManager() {

        this.failureCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, Boolean>() {
                    public Boolean load(String key) throws Exception {
                        return false;
                    }
                });

        this.authClient = buildClient(true);
        this.client = buildClient(false);
    }

    private Client buildClient(boolean auth) {

        Client client = ClientBuilder.newClient();
        client.register(MultiPartFeature.class);

        JacksonJsonProvider provider = new JacksonJaxbJsonProvider()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        client.register(provider);

        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                String key = beanOrClass.getClass().getName() + "." + propertyName;
                if (failureCache.getIfPresent(key) == null) {
                    log.error("Failed to deserialize property which does not exist in model: {}", key);
                    failureCache.put(key, true);
                }
                // JW-33050: We must skip the content here, or further processing may be broken.
                jp.skipChildren();
                return true;
            }
        });

        // Add application id to every request, and for authed requests, add the JWT token
        ClientRequestFilter headerFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                for (Entry<String, String> entry : HttpServiceUtils.getExtraHeaders(auth).entrySet()) {
                    requestContext.getHeaders().add(entry.getKey(), entry.getValue());
                }
            }
        };
        client.register(headerFilter);

        return client;
    }

    public Client getHttpClient(boolean auth) {
        return auth ? authClient : client;
    }

    public WebTarget getTarget(String serverUrl, boolean auth) {
        return auth ? authClient.target(serverUrl) : client.target(serverUrl);
    }
}
