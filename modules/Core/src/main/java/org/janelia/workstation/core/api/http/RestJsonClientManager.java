package org.janelia.workstation.core.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ClientConfig clientConfig = new ClientConfig();
        // values are in milliseconds
        clientConfig.property(ClientProperties.READ_TIMEOUT, 2000);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 500);
        clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, false);

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(50, TimeUnit.MILLISECONDS);
        connectionManager.setValidateAfterInactivity(50);

        // tell the config about the connection manager
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        // tell the connector about the config, which includes the connection manager and timeouts
        ApacheConnectorProvider connectorProvider = new ApacheConnectorProvider();

        // tell the config about the connector
        clientConfig.connectorProvider(connectorProvider);

        JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        jsonProvider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
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
        ClientRequestFilter headerFilter = requestContext -> {
            for (Entry<String, String> entry : HttpServiceUtils.getExtraHeaders(auth).entrySet()) {
                requestContext.getHeaders().add(entry.getKey(), entry.getValue());
            }
        };

        ClientResponseFilter followRedirectFilter = (clientRequestContext, clientResponseContext) -> {
            if (clientResponseContext.getStatusInfo().getFamily() != Response.Status.Family.REDIRECTION) {
                return;
            }

            Response resp = clientRequestContext.getClient()
                    .target(clientResponseContext.getLocation())
                    .request()
                    .method(clientRequestContext.getMethod());

            clientResponseContext.setEntityStream((InputStream) resp.getEntity());
            clientResponseContext.setStatusInfo(resp.getStatusInfo());
            clientResponseContext.setStatus(resp.getStatus());
        };

        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .register(MultiPartFeature.class)
                .register(jsonProvider)
                .register(headerFilter)
                .register(followRedirectFilter)
                .build();
    }

    public Client getHttpClient(boolean auth) {
        return auth ? authClient : client;
    }

    public WebTarget getTarget(String serverUrl, boolean auth) {
        return auth ? authClient.target(serverUrl) : client.target(serverUrl);
    }
}
