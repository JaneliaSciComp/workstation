package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class RESTClientManager {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");

    private static final String REMOTE_DATA_PREFIX = "data";
    private static final String REMOTE_PROCESS_PREFIX = "process";
    
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
    private Map<String, WebTarget> serviceEndpoints;

    public RESTClientManager() {
        this(REMOTE_API_URL);
    }

    public RESTClientManager(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("Using server URL: {}",serverUrl);
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                log.error("Failed to deserialize property which does not exist in model: {}.{}",beanOrClass.getClass().getName(),propertyName);
                return true;
            }
        });
        client = ClientBuilder.newClient();
        client.register(provider);
        client.register(MultiPartFeature.class);
        registerRestUris();
    }

    private void registerRestUris() {
        serviceEndpoints = new HashMap<>();
        serviceEndpoints.put("workspace", client.target(serverUrl + REMOTE_DATA_PREFIX + "/workspace"));
        serviceEndpoints.put("workspaces", client.target(serverUrl + REMOTE_DATA_PREFIX + "/workspaces"));
        serviceEndpoints.put("domainobject", client.target(serverUrl + REMOTE_DATA_PREFIX + "/domainobject"));
        serviceEndpoints.put("ontology", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/ontology"));
        serviceEndpoints.put("annotation", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/annotation"));
        serviceEndpoints.put("filter", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/filter"));
        serviceEndpoints.put("treenode", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/treenode"));
        serviceEndpoints.put("user", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/user"));
        serviceEndpoints.put("dataset", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/dataset"));
        serviceEndpoints.put("login", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/login"));
        serviceEndpoints.put("search", client.target(serverUrl + REMOTE_DATA_PREFIX + "/search"));
        serviceEndpoints.put("sample", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/sample"));
        serviceEndpoints.put("release", client.target(serverUrl  + REMOTE_PROCESS_PREFIX + "/release"));
        serviceEndpoints.put("sampleProcess", client.target(serverUrl  + REMOTE_PROCESS_PREFIX + "/sample"));
        
    }

    public WebTarget getWorkspaceEndpoint() {
        return serviceEndpoints.get("workspace");
    }

    public WebTarget getWorkspacesEndpoint() {
        return serviceEndpoints.get("workspaces");
    }

    public WebTarget getDomainObjectEndpoint() {
        return serviceEndpoints.get("domainobject");
    }

    public WebTarget getOntologyEndpoint() {
        return serviceEndpoints.get("ontology");
    }

    public WebTarget getAnnotationEndpoint() {
        return serviceEndpoints.get("annotation");
    }

    public WebTarget getFilterEndpoint() {
        return serviceEndpoints.get("filter");
    }

    public WebTarget getTreeNodeEndpoint() {
        return serviceEndpoints.get("treenode");
    }

    public WebTarget getUserEndpoint() {
        return serviceEndpoints.get("user");
    }

    public WebTarget getDataSetEndpoint() {
        return serviceEndpoints.get("dataset");
    }

    public WebTarget getLoginEndpoint() {
        return serviceEndpoints.get("login");
    }

    public WebTarget getSearchEndpoint() {
        return serviceEndpoints.get("search");
    }

    public WebTarget getSampleEndpoint() {
        return serviceEndpoints.get("sample");
    }

    public WebTarget getReleaseEndpoint() {
        return serviceEndpoints.get("release");
    }
    
    public WebTarget getSampleProcessEndpoint() {
        return serviceEndpoints.get("sampleProcess");
    }
    
}
