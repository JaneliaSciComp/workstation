package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTClientManager {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");
    private static final String REMOTE_DATA_PREFIX = ConsoleProperties.getInstance().getProperty("domain.facade.rest.data.prefix");
    private static RESTClientManager instance;

    private Client client;
    private String serverUrl;
    private Map<String, WebTarget> serviceEndpoints;

    public RESTClientManager() {
        this(REMOTE_API_URL);
    }

    public RESTClientManager(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("Using server URL: {}",serverUrl);
        client = ClientBuilder.newClient();
        client.register(JacksonFeature.class);
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
        serviceEndpoints.put("objectset", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/objectset"));
        serviceEndpoints.put("user", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/user"));
        serviceEndpoints.put("dataset", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/dataset"));
        serviceEndpoints.put("login", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/login"));
        serviceEndpoints.put("search", client.target(serverUrl + REMOTE_DATA_PREFIX + "/search"));
        serviceEndpoints.put("sample", client.target(serverUrl  + REMOTE_DATA_PREFIX + "/sample"));
    }

    public static RESTClientManager getInstance() {
        if (instance==null) {
            instance = new RESTClientManager();
        }
        return instance;
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

    public WebTarget getObjectSetEndpoint() {
        return serviceEndpoints.get("objectset");
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
    
}
