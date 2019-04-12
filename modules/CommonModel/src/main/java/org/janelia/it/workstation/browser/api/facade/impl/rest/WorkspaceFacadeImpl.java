package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.http.RESTClientBase;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceFacadeImpl extends RESTClientBase implements WorkspaceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFacadeImpl.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");

    private WebTarget service;
    
    public WorkspaceFacadeImpl() {
        this(REMOTE_API_URL);
    }

    public WorkspaceFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    /**
     * Performs a search against the SolrServer and returns the results.
     * @param query the query to execute against the search server
     * @return the search results
     * @throws Exception something went wrong
     */
    @Override
    public SolrJsonResults performSearch(SolrParams query) throws Exception {
        Response response = service.path("data/search")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making search request to the server: " + query)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(SolrJsonResults.class);
    }

    @Override
    public Workspace getDefaultWorkspace() throws Exception {
        Response response = service.path("data/workspace")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDefaultWorkspace from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Workspace.class);
    }

    @Override
    public Collection<Workspace> getWorkspaces() throws Exception {
        String currentSubjectKey = AccessManager.getSubjectKey();
        Response response = service.path("data/workspaces")
                .queryParam("subjectKey", currentSubjectKey)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getWorkspaces from server")) {
            throw new WebApplicationException(response);
        }
        try {
            return response.readEntity(new GenericType<List<Workspace>>() {});
        } catch (Exception e) {
            throw new WebApplicationException("Error de-serializing all workspaces for " + currentSubjectKey, e);
        }
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(treeNode);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = service.path("data/treenode")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createTreeNode to server: " + treeNode)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TreeNode.class);
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = service.path("data/filter")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createFilter to server: " + filter)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Filter.class);
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = service.path("data/filter")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateFilter to server: " + filter)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Filter.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T addChildren(T node, Collection<Reference> references, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        query.setReferences(new ArrayList<>(references));
        Response response = service.path("data/node")
                .path("children")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request addChildrenToNode to server: " + node + "," + references)) {
            throw new WebApplicationException(response);
        }
        return (T)response.readEntity(node.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T removeChildren(T node, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        query.setReferences(new ArrayList<>(references));
        Response response = service.path("data/node")
                .path("children")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request removeChildrenFromNode to server: " + node + "," + references)) {
            throw new WebApplicationException(response);
        }
        return (T)response.readEntity(node.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T reorderChildren(T node, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        List<Integer> orderList = new ArrayList<>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[i]));
        }
        query.setOrdering(orderList);
        Response response = service.path("data/node")
                .path("reorder")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request reorderChildrenInNode to server: " + node + "," + order)) {
            throw new WebApplicationException(response);
        }
        return (T)response.readEntity(node.getClass());
    }

    @Override
    public List<Reference> getContainerReferences(DomainObject object) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(object);
        Response response = service.path("data/domainobject")
                .path("references")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request to get Ancestors from server for: " + object.getId())) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Reference>>() {});
    }
}
