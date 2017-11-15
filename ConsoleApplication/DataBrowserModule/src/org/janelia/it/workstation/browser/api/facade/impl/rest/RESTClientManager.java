package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.janelia.it.workstation.browser.ConsoleApp;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RESTClientManager {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private static final String DEFAULT_REMOTE_REST_URL = ConsoleApp.getConsoleApp().getRemoteRestUrl();
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
    private LoadingCache<String, Boolean> failureCache;
    
    public RESTClientManager() {
        this(DEFAULT_REMOTE_REST_URL);
    }

    public RESTClientManager(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("Using server URL: {}",serverUrl);

        this.failureCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                    new CacheLoader<String, Boolean>() {
                        public Boolean load(String key) throws Exception {
                            return false;
                        }
                    });
        
        this.client = ClientBuilder.newClient();
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
                //.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                //.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                String key = beanOrClass.getClass().getName()+"."+propertyName;
                if (failureCache.getIfPresent(key)==null) {
                    log.error("Failed to deserialize property which does not exist in model: {}",key);
                    failureCache.put(key, true);
                }
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
        serviceEndpoints.put("userGetOrCreate", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/user/getorcreate"));
        serviceEndpoints.put("search", client.target(serverUrl + REMOTE_DATA_PREFIX + "/search"));
        serviceEndpoints.put("sample", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/sample"));
        serviceEndpoints.put("release", client.target(serverUrl  + REMOTE_PROCESS_PREFIX + "/release"));
        serviceEndpoints.put("sampleProcess", client.target(serverUrl  + REMOTE_PROCESS_PREFIX + "/sample"));
        serviceEndpoints.put("summary", client.target(serverUrl + REMOTE_DATA_PREFIX + "/summary"));
        
    }

    public WebTarget getSummaryEndpoint() {
        return serviceEndpoints.get("summary");
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

    public WebTarget getUserGetOrCreateEndpoint() {
        return serviceEndpoints.get("userGetOrCreate");
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
