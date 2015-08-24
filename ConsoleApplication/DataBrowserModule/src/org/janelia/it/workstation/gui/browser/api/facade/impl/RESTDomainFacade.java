package org.janelia.it.workstation.gui.browser.api.facade.impl;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import com.google.common.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Implementation of the DomainFacade using secure RESTful connection
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class RESTDomainFacade implements DomainFacade {
    Client client;
    Map<String, WebTarget> serviceEndpoints;
    String serverUrl;

    public RESTDomainFacade(String serverUrl) throws UnknownHostException {
        this.serverUrl = serverUrl;
        client = ClientBuilder.newClient();
        client.register(JacksonFeature.class);
        registerRestUris();
    }

    private void registerRestUris() {
        serviceEndpoints = new HashMap<String,WebTarget>();
        serviceEndpoints.put("workspace", client.target(serverUrl + "workspace?subjectKey=user:asoy"));
        serviceEndpoints.put("sample", client.target(serverUrl + "sample"));
        serviceEndpoints.put("alignmentboard", client.target(serverUrl + "alignmentboard"));
        serviceEndpoints.put("ontology", client.target(serverUrl + "ontology"));
    }

    public Workspace getDefaultWorkspace() {
        Response response = serviceEndpoints.get("workspace").request("application/json").get();
        Workspace workspace = response.readEntity(Workspace.class);
        int responseStatus = response.getStatus();

        //return dao.getDefaultWorkspace(SessionMgr.getSubjectKey());
        return workspace;
    }
    public List<Subject> getSubjects() {
        return null;
    }

    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id) {
        return null;
    }

    public DomainObject getDomainObject(Reference reference) {
        return null;
    }

    public List<DomainObject> getDomainObjects(List<Reference> references) {
        return null;
    }

    public List<DomainObject> getDomainObjects(String type, Collection<Long> ids) {
        return null;
    }

    public List<Annotation> getAnnotations(Collection<Long> targetIds) {
        return null;
    }

    public Collection<Workspace> getWorkspaces() {
        return null;
    }

    public Collection<Ontology> getOntologies() {
        return null;
    }

    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
    }

    public TreeNode create(TreeNode treeNode) throws Exception {
        return null;
    }

    public ObjectSet create(ObjectSet objectSet) throws Exception {
        return null;
    }

    public Filter create(Filter filter) throws Exception {
        return null;
    }

    public Filter update(Filter filter) throws Exception {
        return null;
    }

    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        return null;
    }

    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        return null;
    }

    public Annotation create(Annotation annotation) throws Exception {
         return null;
    }

    public void remove(Annotation annotation) throws Exception {

    }

    public void changePermissions(ObjectSet objectSet, String granteeKey, String rights, boolean grant) throws Exception {

    }


    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        return null;
    }

    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        return null;
    }

    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        return null;
    }

    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        return null;
    }

    public static void main(String[] args) throws Exception {
        String REST_SERVER_URL = "http://schauderd-ws1.janelia.priv:8080/compute/";
        RESTDomainFacade testclient = new RESTDomainFacade(REST_SERVER_URL);
        Workspace test = testclient.getDefaultWorkspace();
        // should equal "Default Workspace"
        System.out.println (test.getName());

    }
}
