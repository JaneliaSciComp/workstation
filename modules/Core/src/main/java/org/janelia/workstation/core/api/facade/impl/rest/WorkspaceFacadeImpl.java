package org.janelia.workstation.core.api.facade.impl.rest;

import org.janelia.model.access.domain.search.DocumentSearchParams;
import org.janelia.model.access.domain.search.DocumentSearchResults;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ReverseReference;
import org.janelia.model.domain.dto.DomainQuery;
import org.janelia.model.domain.files.SyncedPath;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.facade.interfaces.WorkspaceFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WorkspaceFacadeImpl extends RESTClientBase implements WorkspaceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFacadeImpl.class);

    private WebTarget service;

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
    public DocumentSearchResults performSearch(DocumentSearchParams query) throws Exception {
        WebTarget target = service.path("data/search");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(DocumentSearchResults.class);
    }

    @Override
    public Workspace getDefaultWorkspace() throws Exception {
        WebTarget target = service.path("data/workspace");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        return response.readEntity(Workspace.class);
    }

    @Override
    public Collection<Workspace> getWorkspaces() throws Exception {
        String currentPrincipal = AccessManager.getSubjectKey();
        WebTarget target = service.path("data/workspaces");
        Response response = target
                .queryParam("subjectKey", currentPrincipal)
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Workspace>>() {})
                .stream()
                .sorted(new DomainObjectComparator(currentPrincipal))
                .collect(Collectors.toList());
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(treeNode);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = service.path("data/treenode");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TreeNode.class);
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = service.path("data/filter");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Filter.class);
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = service.path("data/filter");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Filter.class);
    }

    @Override
    public List<DomainObject> getChildren(Node node, String sortCriteria, int page, int pageSize) throws Exception {
        WebTarget target = service.path("data/node/children");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("nodeRef", Reference.createFor(node))
                .queryParam("sortCriteria", sortCriteria)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<DomainObject>>() {});
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T addChildren(T node, Collection<Reference> references, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        query.setReferences(new ArrayList<>(references));
        WebTarget target = service.path("data/node/children");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return (T)response.readEntity(node.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T removeChildren(T node, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        query.setReferences(new ArrayList<>(references));
        WebTarget target = service.path("data/node/children");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return (T)response.readEntity(node.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> T reorderChildren(T node, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(node);
        List<Integer> orderList = new ArrayList<>();
        for (int i : order) {
            orderList.add(i);
        }
        query.setOrdering(orderList);
        WebTarget target = service.path("data/node/reorder");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return (T)response.readEntity(node.getClass());
    }

    @Override
    public List<Reference> getContainerReferences(DomainObject object) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(object);
        WebTarget target = service.path("data/domainobject/references");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Reference>>() {});
    }

    @Override
    public List<SyncedRoot> getSyncedRoots() throws Exception {
        WebTarget target = service.path("data/syncedRoot");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<SyncedRoot>>() {});
    }

    @Override
    public List<SyncedPath> getChildren(SyncedRoot root) throws Exception {
        WebTarget target = service.path("data/syncedRoot/children");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("syncedRootId", root.getId())
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<SyncedPath>>() {});
    }

    @Override
    public SyncedRoot create(SyncedRoot syncedRoot) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(syncedRoot);
        // We need to be able to create SyncedRoots on behalf of groups
        if (syncedRoot.getOwnerKey() != null) {
            query.setSubjectKey(syncedRoot.getOwnerKey());
        }
        else {
            query.setSubjectKey(AccessManager.getSubjectKey());
        }
        WebTarget target = service.path("data/syncedRoot");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(SyncedRoot.class);
    }

    @Override
    public SyncedRoot update(SyncedRoot syncedRoot) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(syncedRoot);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = service.path("data/syncedRoot");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(SyncedRoot.class);
    }

    @Override
    public void remove(SyncedRoot syncedRoot) throws Exception {
        WebTarget target = service.path("data/syncedRoot");
        Response response = target
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("syncedRootId", syncedRoot.getId())
                .request(MediaType.APPLICATION_JSON)
                .delete();
        checkBadResponse(target, response);
    }
}
