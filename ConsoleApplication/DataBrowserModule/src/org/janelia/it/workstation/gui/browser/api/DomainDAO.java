package org.janelia.it.workstation.gui.browser.api;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.common.net.MediaType;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnector;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.WebTarget;

import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The main domain-object DAO for the JACS system.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAO {
    Client client;
    private Map<String, WebTarget> serviceEndpoints;
    String serverUrl;

    private static final Logger log = Logger.getLogger(DomainDAO.class);
    
    // connect to client
    // map out the different methods calling domainDAO.  replace each one with appropriate RESTful call
    // the DAO shouldn't know anything about mongo

    public DomainDAO(String serverUrl) throws UnknownHostException {
        this.serverUrl = serverUrl;
        ClientConfig clientConfig = new ClientConfig();
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        clientConfig.property(ClientProperties.READ_TIMEOUT, 2000);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 500);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        ApacheConnector connector = new ApacheConnector(clientConfig);
        clientConfig.connector(connector);
        client = ClientBuilder.newClient(clientConfig);
        client.register(JacksonFeature.class);

        registerRestUris(client);
    }

    private void registerRestUris () {
        serviceEndpoints.put("workspace", client.target(serverUrl + "workspace"));
        serviceEndpoints.put("sample", client.target(serverUrl + "sample"));
        serviceEndpoints.put("alignmentboard", client.target(serverUrl + "alignmentboard"));
        serviceEndpoints.put("ontology", client.target(serverUrl + "ontology"));
    }
    
    public List<Annotation> getAnnotations(String subjectKey, Long targetId) {

    }
    
    public List<Annotation> getAnnotations(String subjectKey, Collection<Long> targetIds) {

    }
    
    public Workspace getDefaultWorkspace(String subjectKey) {
        Response response = serviceEndpoints.get("workspace").request(MediaType.APPLICATION_JSON).get();
        Workspace workspace = response.readEntity(Workspace.class);
        int responseStatus = response.getStatus();
        return workspace;
    }
    
    public Collection<Workspace> getWorkspaces(String subjectKey) {

    }

    public Collection<Ontology> getOntologies(String subjectKey) {

    }

    public void changePermissions(String subjectKey, String type, Long id, String granteeKey, String rights, boolean grant) throws Exception {

    }
    
    public void changePermissions(String subjectKey, String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {

    }

    public void reorderChildren(String subjectKey, TreeNode treeNode, int[] order) throws Exception {
        

    }

    public void addChild(String subjectKey, TreeNode treeNode, DomainObject domainObject) throws Exception {

    }
    
    public void addChildren(String subjectKey, TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {

    }
    
    public void removeChild(String subjectKey, TreeNode treeNode, DomainObject domainObject) throws Exception {

    }
    
    public void removeChildren(String subjectKey, TreeNode treeNode, Collection<DomainObject> domainObjects) throws Exception {

    }

    public void updateProperty(String subjectKey, DomainObject domainObject, String propName, String propValue) {

    }
    
}
