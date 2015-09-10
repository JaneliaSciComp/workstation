package org.janelia.it.workstation.gui.browser.api.facade.impl;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.*;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.GenericType;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
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

    public RESTDomainFacade(String serverUrl) {
        this.serverUrl = serverUrl;
        client = ClientBuilder.newClient();
        client.register(JacksonFeature.class);
        registerRestUris();
    }

    private void registerRestUris() {
        serviceEndpoints = new HashMap<String,WebTarget>();
        serviceEndpoints.put("workspace", client.target(serverUrl + "workspace"));
        serviceEndpoints.put("domainobject", client.target(serverUrl + "domainobject"));
        serviceEndpoints.put("ontology", client.target(serverUrl + "ontology"));
        serviceEndpoints.put("annotation", client.target(serverUrl + "annotation"));
        serviceEndpoints.put("filter", client.target(serverUrl + "filter"));
        serviceEndpoints.put("treenode", client.target(serverUrl + "treenode"));
        serviceEndpoints.put("objectset", client.target(serverUrl + "objectset"));
        serviceEndpoints.put("user", client.target(serverUrl + "user"));
    }

    // general CRUD for all domain object hierarchies
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id) {
        Collection<Long> ids = new ArrayList<Long>();
        ids.add(id);
        List<DomainObject> objList = getDomainObjects(domainClass.getSimpleName(), ids);
        if (objList!=null && objList.size()>0) {
            return objList.get(0);
        }
        return null;
    }

    public DomainObject getDomainObject(Reference reference) {
        List<Reference> refList = new ArrayList<Reference>();
        refList.add(reference);
        List<DomainObject> domainObjList = getDomainObjects(refList);
        if (domainObjList!=null && domainObjList.size()>0) {
            return domainObjList.get(0);
        }
        return null;
    }

    public List<DomainObject> getDomainObjects(List<Reference> refList) {
        DomainQuery query = new DomainQuery();
        //query.setSubjectKey(SessionMgr.getSubjectKey());
        query.setSubjectKey("group:leetlab");
        query.setReferences(refList);
        Response response = serviceEndpoints.get("domainobject")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        int responseStatus = response.getStatus();
        return domainObjs;
    }

    public List<DomainObject> getDomainObjects(String type, Collection<Long> ids) {
        DomainQuery query = new DomainQuery();
        //query.setSubjectKey(SessionMgr.getSubjectKey());
        query.setSubjectKey("group:leetlab");
        query.setObjectType(type);
        query.setObjectIds(new ArrayList<Long>(ids));

        Response response = serviceEndpoints.get("domainobject")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        // until we resolve adding domain object specific methods or class info into mongo collections, multi if statement
        List<?> domainObjs = null;
        if (type.endsWith("DataSet")) {
            domainObjs = response.readEntity(new GenericType<List<DataSet>>(){});
        } else if (type.endsWith("Image")) {
            domainObjs = response.readEntity(new GenericType<List<Image>>(){});
        } else if (type.endsWith("LSMImage")) {
            domainObjs = response.readEntity(new GenericType<List<LSMImage>>(){});
        } else if (type.endsWith("NeuronFragment")) {
            domainObjs = response.readEntity(new GenericType<List<NeuronFragment>>(){});
        } else if (type.endsWith("NeuronSeparation")) {
            domainObjs = response.readEntity(new GenericType<List<NeuronSeparation>>(){});
        } else if (type.endsWith("ObjectiveSample")) {
            domainObjs = response.readEntity(new GenericType<List<ObjectiveSample>>(){});
        } else if (type.endsWith("PipelineError")) {
            domainObjs = response.readEntity(new GenericType<List<PipelineError>>(){});
        } else if (type.endsWith("PipelineResult")) {
            domainObjs = response.readEntity(new GenericType<List<PipelineResult>>(){});
        } else if (type.endsWith("Sample")) {
            domainObjs = response.readEntity(new GenericType<List<Sample>>(){});
        } else if (type.endsWith("SampleAlignmentResult")) {
            domainObjs = response.readEntity(new GenericType<List<SampleAlignmentResult>>(){});
        } else if (type.endsWith("SampleCellCountingResult")) {
            domainObjs = response.readEntity(new GenericType<List<SampleCellCountingResult>>(){});
        } else if (type.endsWith("SamplePipelineRun")) {
            domainObjs = response.readEntity(new GenericType<List<SamplePipelineRun>>(){});
        } else if (type.endsWith("SampleProcessingResult")) {
            domainObjs = response.readEntity(new GenericType<List<SampleProcessingResult>>(){});
        } else if (type.endsWith("SampleTile")) {
            domainObjs = response.readEntity(new GenericType<List<SampleTile>>(){});
        }

        List<DomainObject> resultList = new ArrayList<DomainObject>();
        for (int i=0; i<domainObjs.size(); i++) {
            resultList.add((DomainObject)domainObjs.get(i));
        }
        int responseStatus = response.getStatus();
        return resultList;
    }

    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(SessionMgr.getSubjectKey());
        query.setObjectType(domainObject.getClass().getSimpleName());
        List<Long> objectIdList = new ArrayList<Long>();
        objectIdList.add(domainObject.getId());
        query.setObjectIds(objectIdList);
        query.setPropertyName(propName);
        query.setPropertyValue(propValue);
        Response response = serviceEndpoints.get("domainobject")
                .request("application/json")
                .post(Entity.json(query));
        DomainObject domainObj = response.readEntity(DomainObject.class);
        int responseStatus = response.getStatus();
        return domainObj;
    }

    // CRUD for annotations/ontologies
    public List<Annotation> getAnnotations(Collection<Long> targetIds) {
        Response response = serviceEndpoints.get("annotation")
                .queryParam("annotationIds", new ArrayList<Long>(targetIds))
                .request("application/json")
                .get();
        List<Annotation> annotations = response.readEntity(new GenericType<List<Annotation>>(){});
        int responseStatus = response.getStatus();
        return annotations;
    }

    public Annotation create(Annotation annotation) throws Exception {
        Response response = serviceEndpoints.get("annotation")
                .request("application/json")
                .put(Entity.json(annotation));
        Annotation newAnnotation = response.readEntity(Annotation.class);
        int responseStatus = response.getStatus();
        return newAnnotation;
    }

    public void remove(Annotation annotation) throws Exception {
        Response response = serviceEndpoints.get("annotation")
                .queryParam("annotationId", annotation.getId())
                .request("application/json")
                .delete();
        int responseStatus = response.getStatus();
    }

    // Views
    public Workspace getDefaultWorkspace() {
        Response response = serviceEndpoints.get("workspace")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .get();
        Workspace workspace = response.readEntity(Workspace.class);
        int responseStatus = response.getStatus();

        return workspace;
    }

    public Collection<Workspace> getWorkspaces() {
        Response response = serviceEndpoints.get("workspace")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .queryParam("option", "full")
                .request("application/json")
                .get();
        List<Workspace> workspaces = response.readEntity(new GenericType<List<Workspace>>(){});
        int responseStatus = response.getStatus();

        return workspaces;
    }

    public Collection<Ontology> getOntologies() {
        Response response = serviceEndpoints.get("ontology")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .get();
        List<Ontology> ontologies = response.readEntity(new GenericType<List<Ontology>>() {
        });
        int responseStatus = response.getStatus();

        return ontologies;
    }

    // search
    public Filter create(Filter filter) throws Exception {
        Response response = serviceEndpoints.get("filter")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .put(Entity.json(filter));
        Filter newFilter = response.readEntity(Filter.class);
        int responseStatus = response.getStatus();

        return newFilter;
    }

    // TO DO: determine whether this should be merged with create
    public Filter update(Filter filter) throws Exception {
        Response response = serviceEndpoints.get("filter")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .post(Entity.json(filter));
        Filter updatedFilter = response.readEntity(Filter.class);
        int responseStatus = response.getStatus();

        return updatedFilter;
    }

    // collections manipulation
    public TreeNode create(TreeNode treeNode) throws Exception {
        Response response = serviceEndpoints.get("treenode")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .put(Entity.json(treeNode));
        TreeNode newTreeNode = response.readEntity(TreeNode.class);
        int responseStatus = response.getStatus();

        return newTreeNode;
    }

    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        List<Integer> orderList = new ArrayList<Integer>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[0]));
        }
        Response response = serviceEndpoints.get("treenode")
                .path("reorder")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .queryParam("treeNodeId", treeNode.getId())
                .request("application/json")
                .post(Entity.json(orderList));
        TreeNode sortedTreeNode = response.readEntity(TreeNode.class);
        int responseStatus = response.getStatus();

        return sortedTreeNode;
    }

    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        Response response = serviceEndpoints.get("treenode")
                .path("children")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .queryParam("treeNodeId", treeNode.getId())
                .queryParam("children", references)
                .request("application/json")
                .delete();
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        int responseStatus = response.getStatus();

        return updatedTreeNode;
    }

    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        for (Reference ref: references) {
            treeNode.addChild(ref);
        }
        Response response = serviceEndpoints.get("treenode")
                .path("children")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .post(Entity.json(treeNode));
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        int responseStatus = response.getStatus();

        return updatedTreeNode;
    }

    public ObjectSet create(ObjectSet objectSet) throws Exception {
        Response response = serviceEndpoints.get("objectset")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .put(Entity.json(objectSet));
        ObjectSet newObjectSet = response.readEntity(ObjectSet.class);
        int responseStatus = response.getStatus();

        return newObjectSet;
    }

    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        for (Reference ref: references) {
            objectSet.addMember(ref.getTargetId());
        }
        Response response = serviceEndpoints.get("objectset")
                .path("member")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .post(Entity.json(objectSet));
        ObjectSet updatedObjectSet = response.readEntity(ObjectSet.class);
        int responseStatus = response.getStatus();

        return updatedObjectSet;
    }

    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        for (Reference ref: references) {
            objectSet.removeMember(ref.getTargetId());
        }
        Response response = serviceEndpoints.get("objectset")
                .path("member")
                .queryParam("subjectKey", SessionMgr.getSubjectKey())
                .request("application/json")
                .post(Entity.json(objectSet));
        ObjectSet updatedObjectSet = response.readEntity(ObjectSet.class);
        int responseStatus = response.getStatus();

        return updatedObjectSet;
    }

    // general user info/permissions/etc
    public List<Subject> getSubjects() {
        Response response = serviceEndpoints.get("user")
                .path("subjects")
                .request("application/json")
                .get();
        List<Subject> subjects = response.readEntity(new GenericType<List<Subject>>(){});
        int responseStatus = response.getStatus();
        return subjects;
    }

    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {

    }

    public void changePermissions(ObjectSet objectSet, String granteeKey, String rights, boolean grant) throws Exception {

    }

    public static void main(String[] args) throws Exception {
        String REST_SERVER_URL = "http://schauderd-ws1.janelia.priv:8080/compute/";
        RESTDomainFacade testclient = new RESTDomainFacade(REST_SERVER_URL);
        Sample test = (Sample)testclient.getDomainObject(org.janelia.it.jacs.model.domain.sample.Sample.class, new Long("1980402565539430407"));
        System.out.println (test.getLine());

    }
}
