package org.janelia.it.workstation.gui.browser.api.facade.impl;

import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.security.BasicAuthToken;
import org.janelia.it.jacs.shared.solr.*;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;

/**
 * Implementation of the DomainFacade using a direct MongoDB connection.
 *
 * NOT FOR PRODUCTION USE!
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDomainFacade implements DomainFacade {

    protected static final String MONGO_SERVER_URL = "dev-mongodb";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "";
    protected static final String MONGO_PASSWORD = "";

    private final DomainDAO dao;

    public MongoDomainFacade() throws Exception {
        this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }

    @Override
    public List<Subject> getSubjects() {
        return dao.getSubjects();
    }

    @Override
    public Subject getSubjectByKey(String key) {
        return dao.getSubjectByKey(key);
    }

    @Override
    public Subject loginSubject (String username, String password) {
        BasicAuthToken userInfo = new BasicAuthToken();
        userInfo.setUsername(username);
        userInfo.setPassword(password);

        Subject user = dao.getSubjectByKey("user:" + userInfo.getUsername());
        return user;
     }

    @Override
    public List<Preference> getPreferences() {
        return dao.getPreferences(AccessManager.getSubjectKey());
    }

    @Override
    public Preference savePreference(Preference preference) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), preference);
    }

    @Override
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainClass, id);
    }

    @Override
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) {
        return dao.getDomainObjectsByName(AccessManager.getSubjectKey(), domainClass, name);
    }
    
    @Override
    public DomainObject getDomainObject(Reference reference) {
        return dao.getDomainObject(AccessManager.getSubjectKey(), reference);
    }

    @Override
    public List<DomainObject> getDomainObjects(List<Reference> references) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), references);
    }

    @Override
    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), className, ids);
    }

    @Override
    public List<DomainObject> getDomainObjects(ReverseReference reference) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), reference);
    }

    @Override
    public List<Annotation> getAnnotations(Collection<Reference> references) {
        return dao.getAnnotations(AccessManager.getSubjectKey(), references);
    }

    @Override
    // return null since SolrConnector is in compute module
    public SolrJsonResults performSearch(SolrParams queryParams) throws Exception {
        SolrQuery query = SolrQueryBuilder.deSerializeSolrQuery(queryParams);
        SolrResults sr = ModelMgr.getModelMgr().searchSolr(query, false);
        SolrJsonResults sjr = new SolrJsonResults();
        Map<String,List<FacetValue>> facetValues = new HashMap<>();
        for (final FacetField ff : sr.getResponse().getFacetFields()) {
            List<FacetValue> favetValues = new ArrayList<>();
            if (ff.getValues()!=null) {
                for (final FacetField.Count count : ff.getValues()) {
                    favetValues.add(new FacetValue(count.getName(),count.getCount()));
                }
            }
            facetValues.put(ff.getName(), favetValues);
        }
        sjr.setFacetValues(facetValues);
        sjr.setResults(sr.getResponse().getResults());
        sjr.setNumFound(sr.getResponse().getResults().getNumFound());
        return sjr;
    }

    @Override
    public Workspace getDefaultWorkspace() {
        return dao.getDefaultWorkspace(AccessManager.getSubjectKey());
    }

    @Override
    public Collection<Workspace> getWorkspaces() {
        return dao.getWorkspaces(AccessManager.getSubjectKey());
    }

    @Override
    public Collection<Ontology> getOntologies() {
        return dao.getOntologies(AccessManager.getSubjectKey());
    }

    @Override
    public Collection<DataSet> getDataSets() {
        return dao.getDataSets(AccessManager.getSubjectKey());
    }

    @Override
    public Collection<LSMImage> getLsmsForSample(Long sampleId) {
        return dao.getLsmsBySampleId(AccessManager.getSubjectKey(), sampleId);
    }

    @Override
    public List<Reference> getContainerReferences(DomainObject object) throws Exception {
        return dao.getContainerReferences(object);
    }

    @Override
    public Ontology create(Ontology ontology) throws Exception {
        return (Ontology)updateIndex(dao.save(AccessManager.getSubjectKey(), ontology));
    }

    @Override
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        return (Ontology)updateIndex(dao.reorderTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, order));
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        return (Ontology)updateIndex(dao.addTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, terms, index));
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        return (Ontology)updateIndex(dao.removeTerm(AccessManager.getSubjectKey(), ontologyId, parentTermId, termId));
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Ontology ontology = dao.getDomainObject(AccessManager.getSubjectKey(), Ontology.class, ontologyId);
        removeFromIndex(ontology.getId());
        dao.remove(AccessManager.getSubjectKey(), ontology);
    }

    @Override
    public Annotation create(Annotation annotation) throws Exception {
        return (Annotation)updateIndex (dao.save(AccessManager.getSubjectKey(), annotation));
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        return (Annotation)updateIndex(dao.save(AccessManager.getSubjectKey(), annotation));
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        removeFromIndex(annotation.getId());
        dao.remove(AccessManager.getSubjectKey(), annotation);
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        removeFromIndex(dataSet.getId());
        dao.remove(AccessManager.getSubjectKey(), dataSet);
    }


    @Override
    public ObjectSet create(ObjectSet objectSet) throws Exception {
        return (ObjectSet) updateIndex(dao.save(AccessManager.getSubjectKey(), objectSet));
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        return (DataSet) updateIndex(dao.save(AccessManager.getSubjectKey(), dataSet));
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        return (DataSet) updateIndex(dao.save(AccessManager.getSubjectKey(), dataSet));
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        return (Filter) updateIndex(dao.save(AccessManager.getSubjectKey(), filter));
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        return (Filter) updateIndex(dao.save(AccessManager.getSubjectKey(), filter));
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        return (TreeNode) updateIndex (dao.save(AccessManager.getSubjectKey(), treeNode));

    }

    @Override
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        return dao.reorderChildren(AccessManager.getSubjectKey(), treeNode, order);
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception {
        TreeNode updatedNode = dao.addChildren(AccessManager.getSubjectKey(), treeNode, references, index);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            ModelMgr.getModelMgr().addAncestorToIndex(child.getId(), updatedNode.getId());
        }
        return updatedNode;
    }

    @Override
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        TreeNode updatedNode = dao.removeChildren(AccessManager.getSubjectKey(), treeNode, references);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            updateIndex(child);
        }
        return updatedNode;
    }

    @Override
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        ObjectSet updatedNode = dao.addMembers(AccessManager.getSubjectKey(), objectSet, references);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            ModelMgr.getModelMgr().addAncestorToIndex(child.getId(), updatedNode.getId());
        }
        return updatedNode;
    }

    @Override
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        ObjectSet updatedNode = dao.removeMembers(AccessManager.getSubjectKey(), objectSet, references);
        List<DomainObject> children = dao.getDomainObjects(AccessManager.getSubjectKey(), new ArrayList<>(references));
        for (DomainObject child: children) {
            updateIndex(child);
        }
        return updatedNode;
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) throws Exception {
        return updateIndex(dao.updateProperty(AccessManager.getSubjectKey(), domainObject.getClass().getName(),
                    domainObject.getId(), propName, propValue));
    }

    @Override
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        dao.changePermissions(AccessManager.getSubjectKey(), domainObject.getClass().getName(), Arrays.asList(domainObject.getId()), granteeKey, rights, grant);
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainObject);
    }

    private DomainObject updateIndex(DomainObject obj) throws Exception {
        ModelMgr.getModelMgr().updateIndex(obj);
        return obj;
    }

    private void removeFromIndex(Long domainObjId) throws Exception {
        ModelMgr.getModelMgr().removeFromIndex(domainObjId);
    }

    @Override
    public void remove(List<Reference> deleteObjectRefs) throws Exception {
        for (Reference objectRef : deleteObjectRefs) {
            // first check that it is an objectset or treeNode
            Class<? extends DomainObject> objClass = DomainUtils.getObjectClassByName(objectRef.getTargetClassName());
            if (objClass.equals("org.janelia.it.jacs.model.domain.workspace.TreeNode") ||
                    objClass.equals("org.janelia.it.jacs.model.domain.workspace.ObjectSet")) {
                String subjectKey = AccessManager.getSubjectKey();
                DomainObject domainObj = dao.getDomainObject(subjectKey, objectRef);
                // check whether this subject has permissions to write to this object
                if (domainObj.getWriters().contains(subjectKey)) {
                    dao.remove(subjectKey, domainObj);
                }
            }
        }
    }
}
