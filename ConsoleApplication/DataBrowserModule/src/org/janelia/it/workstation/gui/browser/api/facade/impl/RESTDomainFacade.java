package org.janelia.it.workstation.gui.browser.api.facade.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.solr.common.SolrDocumentList;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.ObjectSetCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.ontology.Tag;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.solr.SolrJsonResults;
import org.janelia.it.jacs.shared.solr.SolrParams;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the DomainFacade using secure RESTful connection
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class RESTDomainFacade implements DomainFacade {

    private static final Logger log = LoggerFactory.getLogger(RESTDomainFacade.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");

    Client client;
    Map<String, WebTarget> serviceEndpoints;
    String serverUrl;

    public RESTDomainFacade() {
        this(REMOTE_API_URL);
    }

    public RESTDomainFacade(String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("Using server URL: {}",serverUrl);
        client = ClientBuilder.newClient();
        client.register(JacksonFeature.class);
        registerRestUris();
    }

    private void registerRestUris() {
        serviceEndpoints = new HashMap<String,WebTarget>();
        serviceEndpoints.put("workspace", client.target(serverUrl + "workspace"));
        serviceEndpoints.put("workspaces", client.target(serverUrl + "workspaces"));
        serviceEndpoints.put("domainobject", client.target(serverUrl + "domainobject"));
        serviceEndpoints.put("ontology", client.target(serverUrl + "ontology"));
        serviceEndpoints.put("annotation", client.target(serverUrl + "annotation"));
        serviceEndpoints.put("filter", client.target(serverUrl + "filter"));
        serviceEndpoints.put("treenode", client.target(serverUrl + "treenode"));
        serviceEndpoints.put("objectset", client.target(serverUrl + "objectset"));
        serviceEndpoints.put("user", client.target(serverUrl + "user"));
        serviceEndpoints.put("dataset", client.target(serverUrl + "dataset"));
        serviceEndpoints.put("login", client.target(serverUrl + "login"));
        serviceEndpoints.put("search", client.target(serverUrl + "search"));
    }

    // general CRUD for all domain object hierarchies
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        Collection<Long> ids = new ArrayList<Long>();
        ids.add(id);
        List<DomainObject> objList = getDomainObjects(domainClass.getName(), ids);
        if (objList!=null && objList.size()>0) {
            return (T)objList.get(0);
        }
        return null;
    }
    
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) {
        // TODO: implement
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
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(refList);
        Response response = serviceEndpoints.get("domainobject")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObject from server" + refList)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }

    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setObjectType(className);
        query.setObjectIds(new ArrayList<Long>(ids));

        Response response = serviceEndpoints.get("domainobject")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getDomainObjects from server" + ids)) {
            return null;
        }
        List<DomainObject> domainObjs = response.readEntity(new GenericType<List<DomainObject>>() {
        });
        return domainObjs;
    }

    @Override
    public List<DomainObject> getDomainObjects(ReverseReference reference) {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setObjectType(domainObject.getClass().getName());
        List<Long> objectIdList = new ArrayList<Long>();
        objectIdList.add(domainObject.getId());
        query.setObjectIds(objectIdList);
        query.setPropertyName(propName);
        query.setPropertyValue(propValue);
        Response response = serviceEndpoints.get("domainobject")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateProperty from server" + propName + "," + propValue)) {
            return null;
        }
        DomainObject domainObj = response.readEntity(DomainObject.class);
        return domainObj;
    }

    public List<Annotation> getAnnotations(Collection<Reference> references) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setReferences(new ArrayList<Reference>(references));

        Response response = serviceEndpoints.get("annotation")
                .path("details")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request getAnnotations from server" + references)) {
            return null;
        }
        List<Annotation> annotations = response.readEntity(new GenericType<List<Annotation>>() {
        });
        return annotations;
    }

    public Annotation create(Annotation annotation) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(annotation);
        Response response = serviceEndpoints.get("annotation")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createAnnotation from server" + annotation)) {
            return null;
        }
        Annotation newAnnotation = response.readEntity(Annotation.class);
        return newAnnotation;
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(annotation);
        Response response = serviceEndpoints.get("annotation")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateAnnotation from server" + annotation)) {
            return null;
        }
        Annotation newAnnotation = response.readEntity(Annotation.class);
        return newAnnotation;
    }

    public void remove(Annotation annotation) throws Exception {
        Response response = serviceEndpoints.get("annotation")
                .queryParam("annotationId", annotation.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        checkBadResponse(response.getStatus(), "problem making request removeAnnotation from server" + annotation);
    }

    public void remove(DataSet dataSet) throws Exception {
        Response response = serviceEndpoints.get("dataset")
                .queryParam("dataSetId", dataSet.getId())
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        checkBadResponse(response.getStatus(), "problem making request removeDataSet from server" + dataSet);
    }

    public Workspace getDefaultWorkspace() {
        Response response = serviceEndpoints.get("workspace")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDefaultWorkspace from server")) {
            return null;
        }
        Workspace workspace = response.readEntity(Workspace.class);
        return workspace;
    }

    public Collection<Workspace> getWorkspaces() {
        Response response = serviceEndpoints.get("workspaces")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getWorkspaces from server")) {
            return null;
        }

        Collection<Workspace> foo = response.readEntity(new GenericType<List<Workspace>>() {});
        System.out.println (foo);
        return foo;
    }

    public Collection<DataSet> getDataSets() {
        Response response = serviceEndpoints.get("dataset")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getDataSets from server")) {
            return null;
        }
        return response.readEntity(new GenericType<List<DataSet>>() {});
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) {
        // TODO: implement
        throw new UnsupportedOperationException();
    }
    
    public DataSet create(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(dataSet);
        Response response = serviceEndpoints.get("dataset")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createDataSet from server")) {
            return null;
        }
        DataSet newDataSet = response.readEntity(DataSet.class);
        return newDataSet;
    }

    public DataSet update(DataSet dataSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(dataSet);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = serviceEndpoints.get("dataset")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateDataSet to server" + dataSet)) {
            return null;
        }
        DataSet newDataSet = response.readEntity(DataSet.class);
        return newDataSet;
    }

    public Collection<Ontology> getOntologies() {
        Response response = serviceEndpoints.get("ontology")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getOntologies from server")) {
            return null;
        }
        return response.readEntity(new GenericType<List<Ontology>>() {});
    }

    @Override
    public Ontology create(Ontology ontology) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(ontology);
        Response response = serviceEndpoints.get("ontology")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createOntology from server")) {
            return null;
        }
        Ontology newOntology = response.readEntity(Ontology.class);
        return newOntology;
    }

    @Override
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        List<Long> objectIds = new ArrayList<Long>();
        objectIds.add(ontologyId);
        objectIds.add(parentTermId);
        query.setObjectIds(objectIds);
        List<Integer> orderList = new ArrayList<>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[i]));
        }
        query.setOrdering(orderList);
        Response response = serviceEndpoints.get("ontology")
                .path("terms")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request reorderOntologyTerms to server" + ontologyId + "," + parentTermId + "," + order)) {
            return null;
        }
        Ontology newOntology = response.readEntity(Ontology.class);
        return newOntology;
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        List<Long> objectIds = new ArrayList<Long>();
        objectIds.add(ontologyId);
        objectIds.add(parentTermId);
        query.setObjectIds(objectIds);
        query.setObjectList(new ArrayList<OntologyTerm>(terms));
        List<Integer> ordering = new ArrayList<>();
        ordering.add(index);
        query.setOrdering(ordering);
        Response response = serviceEndpoints.get("ontology")
                .path("terms")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request addOntologyTerms to server" + ontologyId + "," + parentTermId + "," + terms)) {
            return null;
        }
        Ontology newOntology = response.readEntity(Ontology.class);
        return newOntology;
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        Response response = serviceEndpoints.get("ontology")
                .path("terms")
                .queryParam("ontologyId", ontologyId)
                .queryParam("parentTermId", parentTermId)
                .queryParam("termId", termId)
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeOntologyTerms to server" + ontologyId + "," + parentTermId + "," + termId)) {
            return null;
        }
        Ontology newOntology = response.readEntity(Ontology.class);
        return newOntology;
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Response response = serviceEndpoints.get("ontology")
                .queryParam("ontologyId", ontologyId)
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .delete();
        checkBadResponse(response.getStatus(), "problem making request removeOntology to server" + ontologyId);
    }

    public Filter create(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = serviceEndpoints.get("filter")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createFilter to server" + filter)) {
            return null;
        }
        Filter newFilter = response.readEntity(Filter.class);
        return newFilter;
    }

    public Filter update(Filter filter) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(filter);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = serviceEndpoints.get("filter")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateFilter to server" + filter)) {
            return null;
        }
        Filter newFilter = response.readEntity(Filter.class);
        return newFilter;
    }

    /**
     * Performs a search against the SolrServer and returns the results.
     * @param query the query to execute against the search server
     * @return the search results
     * @throws Exception something went wrong
     */
    public SolrJsonResults performSearch(SolrParams query) {
        Response response = serviceEndpoints.get("search")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making search request to the server" + query)) {
            return null;
        }
        SolrJsonResults results = response.readEntity(SolrJsonResults.class);
        return results;
    }

    public TreeNode create(TreeNode treeNode) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(treeNode);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = serviceEndpoints.get("treenode")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createTreeNode to server" + treeNode)) {
            return null;
        }
        TreeNode newTreeNode = response.readEntity(TreeNode.class);
        return newTreeNode;
    }

    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        List<Integer> orderList = new ArrayList<>();
        for (int i=0; i<order.length; i++) {
            orderList.add(new Integer(order[i]));
        }
        query.setOrdering(orderList);
        Response response = serviceEndpoints.get("treenode")
                .path("reorder")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request reorderChildrenInTreeNode to server" + treeNode + "," + order)) {
            return null;
        }
        TreeNode sortedTreeNode = response.readEntity(TreeNode.class);
        return sortedTreeNode;
    }

    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        query.setReferences(new ArrayList<Reference>(references));
        Response response = serviceEndpoints.get("treenode")
                .path("children")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request removeChildrenFromTreeNode to server" + treeNode + "," + references)) {
            return null;
        }
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        return updatedTreeNode;
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(treeNode);
        query.setReferences(new ArrayList<Reference>(references));
        Response response = serviceEndpoints.get("treenode")
                .path("children")
                .request("application/json")
                .put(Entity.json(query));
        TreeNode updatedTreeNode = response.readEntity(TreeNode.class);
        if (checkBadResponse(response.getStatus(), "problem making request addChildrenToTreeNode to server" + treeNode + "," + references)) {
            return null;
        }
        return updatedTreeNode;
    }

    public ObjectSet create(ObjectSet objectSet) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(objectSet);
        Response response = serviceEndpoints.get("objectset")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createObjectset to server")) {
            return null;
        }
        return response.readEntity(ObjectSet.class);
    }

    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(objectSet);
        query.setReferences(new ArrayList<Reference>(references));
        Response response = serviceEndpoints.get("objectset")
                .path("member")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request addMembersToObjectSet to server" + objectSet + "," + references)) {
            return null;
        }
        ObjectSet updatedObjectSet = response.readEntity(ObjectSet.class);
        return updatedObjectSet;
    }

    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(objectSet);
        query.setReferences(new ArrayList<Reference>(references));
        Response response = serviceEndpoints.get("objectset")
                .path("member")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request removeMembersFromObjectSet to server" + objectSet + "," + references)) {
            return null;
        }
        ObjectSet updatedObjectSet = response.readEntity(ObjectSet.class);
        return updatedObjectSet;
    }

    public Subject loginSubject(String username, String password) {
        String credentials = new String("Basic " + Base64.encodeAsString(username + ":" + password));
        Response response = serviceEndpoints.get("login")
                .request("application/json")
                .header("Authorization", credentials)
                .get();
        if (checkBadResponse(response.getStatus(), "problem making authenticating user against the server")) {
            return null;
        }
        Subject authSubject = response.readEntity(Subject.class);
        return authSubject;
    }

    public List<Subject> getSubjects() {
        Response response = serviceEndpoints.get("user")
                .path("subjects")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjects to server")) {
            return null;
        }
        List<Subject> subjects = response.readEntity(new GenericType<List<Subject>>() {
        });
        return subjects;
    }

    public Subject getSubjectByKey(String key) {
        Response response = serviceEndpoints.get("user")
                .path("subject")
                .queryParam("subjectKey", key)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjectByKey to server")) {
            return null;
        }
        Subject subject = response.readEntity(Subject.class);
        return subject;
    }

    @Override
    public List<Preference> getPreferences() {
        Response response = serviceEndpoints.get("user")
                .path("preferences")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getPreferences to server")) {
            return null;
        }
        List<Preference> preferences = response.readEntity(new GenericType<List<Preference>>(){});
        return preferences;
    }

    @Override
    public Preference savePreference(Preference preference) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setPreference(preference);
        Response response = serviceEndpoints.get("user")
                .path("preferences")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making saving request savePreferences to server " + preference)) {
            return null;
        }
        Preference newPref = response.readEntity(Preference.class);
        return newPref;
    }

    @Override
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("subjectKey", AccessManager.getSubjectKey());
        params.put("targetClass", domainObject.getClass().getName());
        params.put("targetId", domainObject.getId());
        params.put("granteeKey", granteeKey);
        params.put("rights", rights);
        params.put("grant", new Boolean(grant));
        Response response = serviceEndpoints.get("user")
                .path("permissions")
                .request("application/json")
                .put(Entity.json(params));
        if (checkBadResponse(response.getStatus(), "problem making request changePermissions to server " + domainObject + "," + granteeKey + "," + rights + "," + grant)) {
            return null;
        }
        return this.getDomainObject(new Reference(domainObject.getClass().getName(), domainObject.getId()));
    }

    private boolean checkBadResponse (int responseStatus, String failureError) {
        if (responseStatus<200 || responseStatus>=300) {
            log.error("ERROR RESPONSE: " + responseStatus);
            log.error(failureError);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        // SOME QUICKIE TESTS BEFORE UNIT TESTS ARE ADDED

        String REST_SERVER_URL = "http://schauderd-ws1.janelia.priv:8080/compute/";
        RESTDomainFacade testclient = new RESTDomainFacade(REST_SERVER_URL);
        System.out.println("ASDFASDF");
        Sample test = (Sample)testclient.getDomainObject(org.janelia.it.jacs.model.domain.sample.Sample.class, new Long("1734424924644180066"));
        System.out.println (test.getLine());
        test = (Sample)testclient.getDomainObject(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
        System.out.println(test.getLine());
        List<Reference> referenceList = new ArrayList<Reference>();
        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1734424924644180066")));
        List<DomainObject> test2 = testclient.getDomainObjects(referenceList);
        System.out.println(test2);
        DomainObject foo = testclient.updateProperty(test, "effector", "QUACKTHEDUCK");
        System.out.println(((Sample) foo).getEffector());

        // annotations
        Annotation testAnnotation = new Annotation();
        testAnnotation.setKey("Partly_OK");
        testAnnotation.setTarget(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
        testAnnotation.setName("TESTANNOTATION");
        OntologyTermReference ontTest = new OntologyTermReference();
        ontTest.setOntologyId(new Long("1870641514531520601"));
        ontTest.setOntologyTermId(new Long("1898740256916635737"));
        testAnnotation.setKeyTerm(ontTest);
        Annotation newAnnotation = testclient.create(testAnnotation);
        System.out.println(newAnnotation.getName());
        referenceList = new ArrayList<Reference>();
        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
        referenceList.add(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069234")));
        List<Annotation> annotationList = testclient.getAnnotations(referenceList);
        System.out.println(annotationList);
        testclient.remove(newAnnotation);

        // workspaces
        Workspace workspace = testclient.getDefaultWorkspace();
        System.out.println (workspace.getChildren());
        Collection<Workspace> workspaces = testclient.getWorkspaces();
        System.out.println (workspaces);

        // ontologies
        Collection<Ontology> ontologies = testclient.getOntologies();
        System.out.println (ontologies);
        Ontology ont = new Ontology();
        List<OntologyTerm> terms = new ArrayList<OntologyTerm>();
        Category category = new Category();
        category.setName("IISCATEGORY");
        terms.add(category);
        Ontology newOnt = testclient.create(ont);
        newOnt.setTerms(new ArrayList<OntologyTerm>());
        System.out.println(newOnt.getId());

        // ontology terms
        newOnt = testclient.addTerms(newOnt.getId(),newOnt.getId(), terms, 0);
        System.out.println(newOnt.getTerms());
        List<OntologyTerm> categoryList = new ArrayList<OntologyTerm>();
        Tag tag = new Tag();
        tag.setName("IISTAG1");
        categoryList.add(tag);
        tag = new Tag();
        tag.setName("IISTAG2");
        categoryList.add(tag);
        newOnt = testclient.addTerms(newOnt.getId(),newOnt.getTerms().get(0).getId(), categoryList, 0);
        testclient.reorderTerms(newOnt.getId(), newOnt.getTerms().get(0).getId(), new int[]{1, 0});
        OntologyTerm testTerm = newOnt.getTerms().get(0).getTerms().get(0);
        newOnt = testclient.removeTerm(newOnt.getId(),newOnt.getTerms().get(0).getId(), testTerm.getId());
        System.out.println(newOnt.getTerms().get(0).getTerms());
        testclient.removeOntology(newOnt.getId());

        // filter
        Filter newFilter = new Filter();
        newFilter.setSearchString("whatevers");
        newFilter.setSearchClass("whateversclass");
        List<Criteria> critList = new ArrayList<Criteria>();
        ObjectSetCriteria crit = new ObjectSetCriteria();
        crit.setObjectSetName("objectName");
        crit.setObjectSetReference(new Reference("org.janelia.it.jacs.model.domain.sample.Sample", new Long("1714569876758069346")));
        critList.add(crit);
        newFilter.setCriteriaList(critList);
        newFilter = testclient.create(newFilter);
        System.out.println (newFilter.getId());
        newFilter.setName("WACKAMOLE");
        newFilter = testclient.update(newFilter);
        System.out.println (newFilter.getName());

        // treenodes
        TreeNode treenode = new TreeNode();
        treenode = testclient.create(treenode);
        System.out.println (treenode.getId());
        treenode = testclient.addChildren(treenode, referenceList, new Integer (0));
        System.out.println (treenode.getNumChildren());
        int[] order = {1,0};
        treenode = testclient.reorderChildren(treenode, order);
        System.out.println (treenode.getChildren().get(0).getTargetId());
        treenode = testclient.removeChildren(treenode, referenceList);
        System.out.println (treenode.getNumChildren());

        // objectsets
        ObjectSet objectset = new ObjectSet();
        objectset = testclient.create(objectset);
        System.out.println (objectset.getId());
        objectset = testclient.addMembers(objectset, referenceList);
        System.out.println (objectset.getNumMembers());
        objectset = testclient.removeMembers(objectset, referenceList);
        System.out.println (objectset.getNumMembers());

        // user settings
        List<Subject> subjects = testclient.getSubjects();
        System.out.println(subjects);
        List<Preference> preferences = testclient.getPreferences();
        System.out.println (preferences);
        Preference newPref = new Preference();
        newPref.setCategory("TEST");
        newPref.setKey("test");
        newPref.setValue("value");
        newPref = testclient.savePreference(newPref);
        System.out.println (newPref.getCategory());
        objectset = (ObjectSet)testclient.changePermissions(objectset, "user:schauderd", "write", true);
        System.out.println (objectset.getWriters());
    }
}
