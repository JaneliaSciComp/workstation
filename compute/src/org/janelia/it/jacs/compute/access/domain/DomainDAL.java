package org.janelia.it.jacs.compute.access.domain;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by schauderd on 5/28/16.
 * Data Abstraction Layer to merge domain db access with SOLR updates.  For now merging this into one class to reduce object bloat.
 * Will refactor based off usage patterns.  Both REST and pipelines services will talk to this layer to coordinate CRUD operations
 */
public class DomainDAL {

    public static final String FILE_STORE_CENTRAL_DIR_PROP = "FileStore.CentralDir";
    
    private static DomainDAL instance;
    protected DomainDAO dao;
    protected SolrConnector solr;

    private DomainDAL() {
        this.dao = DomainDAOManager.getInstance().getDao();
        this.solr = new SolrConnector(dao, false, false);
    }

    public static DomainDAL getInstance() {
        if (instance==null) {
            instance = new DomainDAL();
        }
        return instance;
    }


    // General DomainObject CRUD

    public DomainObject getDomainObject(String subjectKey, Reference reference) {
        return dao.getDomainObject(subjectKey, reference);
    }

    public DomainObject getDomainObject(String subjectKey, String className, Long domainObjectId) {
        return dao.getDomainObject(subjectKey, DomainUtils.getObjectClassByName(className), domainObjectId);
    }

    public <T extends DomainObject> T getDomainObject(String subjectKey, Class<T> clazz, Long id) {
        return dao.getDomainObject(subjectKey, clazz, id);
    }

    public List<DomainObject> getDomainObjects(String subjectKey, List<Reference> references) {
        return dao.getDomainObjects(subjectKey, references);
    }

    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, Class<T> domainClass) {
        return dao.getDomainObjects(subjectKey, domainClass, null);
    }

    public <T extends DomainObject> List<T> getDomainObjects(String subjectKey, String domainClass, List<Long> ids)  {
        return dao.getDomainObjects(subjectKey, domainClass, ids);
    }

    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass) {
        return dao.getUserDomainObjects(subjectKey, domainClass);
    }

    public <T extends DomainObject> List<T> getUserDomainObjects(String subjectKey, Class<T> domainClass, Collection<Long> ids) {
        return dao.getUserDomainObjects(subjectKey, domainClass, null);
    }

    public List<Reference> getContainerReferences(DomainObject domainObject) throws Exception {
        return dao.getContainerReferences(domainObject);
    }

    public List<DomainObject> getDomainObjects(String subjectKey, ReverseReference reverseRef) {
        return dao.getDomainObjects(subjectKey, reverseRef);
    }

    public <T extends DomainObject> List<T> getDomainObjectsByName(String subjectKey, Class<T> clazz,  String name) throws Exception {
        return dao.getDomainObjectsByName(subjectKey, clazz, name);
    }

    public <T extends DomainObject> List<T> getUserDomainObjectsByName(String subjectKey, Class<T> clazz,  String name) throws Exception {
        return dao.getUserDomainObjectsByName(subjectKey, clazz, name);
    }
    
    public <T extends DomainObject> List<T> getDomainObjectsAs(List<Reference> references, Class<T> clazz) throws Exception {
        return getDomainObjectsAs(null, references, clazz);
    }

    public <T extends DomainObject> List<T> getDomainObjectsAs(String subjectKey, List<Reference> references, Class<T> clazz) {
        return dao.getDomainObjectsAs(subjectKey, references, clazz);
    }

    public void deleteDomainObjects(String subjectKey, List<Reference> references) throws Exception {
        List<DomainObject> domainObjList = dao.getDomainObjects(null, references);
        for (DomainObject domainObj: domainObjList) {
            IndexingHelper.sendRemoveFromIndexMessage(domainObj);
            dao.remove(subjectKey, domainObj);
        }
    }

    public void deleteDomainObject(String subjectKey, DomainObject domainObject) throws Exception {
        dao.remove(subjectKey, domainObject);
        IndexingHelper.sendRemoveFromIndexMessage(domainObject);
        if (domainObject instanceof Annotation) {
            Annotation annotation = (Annotation)domainObject;
            DomainObject targetObject = dao.getDomainObject(null, annotation.getTarget());
            if (targetObject!=null) {
                IndexingHelper.sendReindexingMessage(targetObject);
            }
        }
    }

    public <T extends DomainObject> T save(String subjectKey, T domainObject) throws Exception {
        T savedObj =  dao.save(subjectKey, domainObject);
        IndexingHelper.sendReindexingMessage(savedObj);
        if (domainObject instanceof Annotation) {
            Annotation annotation = (Annotation)domainObject;
            DomainObject targetObject = dao.getDomainObject(null, annotation.getTarget());
            if (targetObject!=null) {
                IndexingHelper.sendReindexingMessage(targetObject);
            }
        }
        return savedObj;
    }

    public <T extends DomainObject> T createWithPrepopulatedId(String subjectKey, T domainObject) throws Exception {
        T savedObj =  dao.createWithPrepopulatedId(subjectKey, domainObject);
        IndexingHelper.sendReindexingMessage(savedObj);
        if (domainObject instanceof Annotation) {
            Annotation annotation = (Annotation)domainObject;
            DomainObject targetObject = dao.getDomainObject(null, annotation.getTarget());
            if (targetObject!=null) {
                IndexingHelper.sendReindexingMessage(targetObject);
            }
        }
        return savedObj;
    }

    public DomainObject updateProperty(String subjectKey, DomainObject domainObj, String property, String value) throws Exception {
        DomainObject updateObj = dao.updateProperty(subjectKey, domainObj.getClass(), domainObj.getId(), property, value);
        IndexingHelper.sendReindexingMessage(updateObj);
        return updateObj;
    }

    public DomainObject updateProperty(String subjectKey, String domainClassName, Long id, String property, String value) throws Exception {
        DomainObject updateObj = dao.updateProperty(subjectKey, domainClassName, id, property, value);
        IndexingHelper.sendReindexingMessage(updateObj);
        return updateObj;
    }

    public <T extends DomainObject> T updateProperty(String subjectKey, Class<T> clazz, Long id, String property, Object value) throws Exception {
        T updateObj = dao.updateProperty(subjectKey, clazz, id, property, value);
        IndexingHelper.sendReindexingMessage(updateObj);
        return updateObj;
    }
    

    public <T extends DomainObject> List<T> getDomainObjectsWithProperty(String subjectKey, Class<T> domainClass, String propName, String propValue) {
        return dao.getDomainObjectsWithProperty(subjectKey, domainClass, propName, propValue);
    }
    
    // specific custom DomainObject operations

    public Subject save(Subject user) throws Exception {
        return dao.save(user);
    }

    public Preference save(String subjectKey, Preference preference) throws Exception {
        return dao.save(subjectKey, preference);
    }

    public List<Sample> getActiveSamplesForDataSet(String subjectKey, String identifier) throws Exception {
        return dao.getActiveSamplesForDataSet(subjectKey, identifier);
    }

    public DataSet getDataSetByIdentifier(String subjectKey, String identifier) throws Exception {
        return dao.getDataSetByIdentifier(subjectKey, identifier);
    }

    public List<LSMImage> getActiveLsmsBySampleId(String subjectKey, Long sampleId) {
        return dao.getActiveLsmsBySampleId(subjectKey, sampleId);
    }

    public List<LSMImage> getActiveLsmsForDataSet(String subjectKey, String identifier) {
        return dao.getActiveLsmsForDataSet(subjectKey, identifier);
    }

    public LSMImage getActiveLsmBySageId(String subjectKey, Integer sageId) {
        return dao.getActiveLsmBySageId(subjectKey, sageId);
    }

    public List<LSMImage> getUserLsmsBySageId(String subjectKey, Integer sageId) {
        return dao.getUserLsmsBySageId(subjectKey, sageId);
    }

    public Sample getActiveSampleBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {
        return dao.getActiveSampleBySlideCode(subjectKey, dataSetIdentifier, slideCode);
    }

    public List<Sample> getUserSamplesBySlideCode(String subjectKey, String dataSetIdentifier, String slideCode) {
        return dao.getUserSamplesBySlideCode(subjectKey, dataSetIdentifier, slideCode);
    }

    public List<NeuronFragment> getNeuronFragmentsBySeparationId(String subjectKey, Long id) throws Exception {
        return dao.getNeuronFragmentsBySeparationId(subjectKey, id);
    }

    public Sample getSampleBySeparationId(String subjectKey, Long id) throws Exception {
        return dao.getSampleBySeparationId(subjectKey, id);
    }

    /**
     * Returns the neuron separation with the given id. Note that this is fine for reading neuron separation information,
     * but this method cannot be used to update a neuron separation, because there may be multiple instances of an
     * neuron separation with a given GUID, due to denormalization in the Sample model.
     * @param subjectKey
     * @param id
     * @return
     * @throws Exception
     */
    public NeuronSeparation getNeuronSeparation(String subjectKey, Long id) throws Exception {
        return dao.getNeuronSeparation(subjectKey, id);
    }

    public Long getNewId() {
        return TimebasedIdentifierGenerator.generateIdList(1).get(0);
    }

    public Annotation createAnnotation(String subjectKey, Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception {
        return dao.createAnnotation(subjectKey, target, ontologyTermReference, value);
    }

    public List<Annotation> getAnnotations(String subjectKey, Reference reference) {
        return getAnnotations(subjectKey, Arrays.asList(reference));
    }

    public List<Annotation> getAnnotations(String subjectKey, List<Reference> references) {
        return dao.getAnnotations(subjectKey, references);
    }

    public List<DataSet> getDataSets(String subjectKey) throws Exception {
        return dao.getDataSets(subjectKey);
    }

    public List<Ontology> getOntologies(String subjectKey) throws Exception {
        return dao.getOntologies(subjectKey);
    }

    public Ontology addOntologyTerms(String subjectKey, Long ontologyId, Long parentId, List<OntologyTerm> terms, Integer index) throws Exception {
        Ontology updateOntology = dao.addTerms(subjectKey, ontologyId, parentId, terms, index);
        IndexingHelper.sendReindexingMessage(updateOntology);
        return updateOntology;
    }

    public Ontology reorderOntologyTerms(String subjectKey, Long ontologyId, Long parentId, int[] order) throws Exception {
        return dao.reorderTerms(subjectKey, ontologyId, parentId, order);
    }

    public Ontology removeOntologyTerm(String subjectKey, Long ontologyId, Long parentId, Long termId) throws Exception {
        Ontology updateOntology = dao.removeTerm(subjectKey, ontologyId, parentId, termId);
        IndexingHelper.sendReindexingMessage(updateOntology);
        return updateOntology;
    }

    public TreeNode getOrCreateDefaultFolder(String subjectKey, String folderName) throws Exception {
        return dao.getOrCreateDefaultFolder(subjectKey, folderName);
    }
    
    public TreeNode reorderChildren (String subjectKey, TreeNode treeNode, int[] order) throws Exception {
        TreeNode updatedNode = dao.reorderChildren(subjectKey, treeNode, order);
        return updatedNode;
    }

    public List<LineRelease> getLineReleases (String subjectKey) throws Exception {
        return dao.getLineReleases(subjectKey);
    }

    public LineRelease createLineRelease(String subjectKey, String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception {
        return dao.createLineRelease(subjectKey, name, releaseDate, lagTimeMonths, dataSets);
    }

    public List<DomainObject> getChildren(String subjectKey, TreeNode treeNode) throws Exception {
        return dao.getChildren(subjectKey, treeNode);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNode, List<Reference> references) throws Exception {
        return addChildren(subjectKey, treeNode, references, null);
    }

    public TreeNode addChildren(String subjectKey, TreeNode treeNode, List<Reference> references, Integer index) throws Exception {
        TreeNode updatedNode = dao.addChildren(subjectKey, treeNode, references, index);
        List<DomainObject> children = dao.getDomainObjects(subjectKey,references);
        for (DomainObject child: children) {
            IndexingHelper.sendAddAncestorMessage(child, updatedNode);
        }
        return updatedNode;
    }

    public TreeNode removeChildren(String subjectKey, TreeNode treeNode, List<Reference> references) throws Exception {
        TreeNode updatedNode = dao.removeChildren(subjectKey, treeNode, references);
        List<DomainObject> children = dao.getDomainObjects(subjectKey,references);
        for (DomainObject child: children) {
            IndexingHelper.sendReindexingMessage(child);
        }
        return updatedNode;
    }
    
    // Users and Groups
	
    public List<Subject> getSubjects() throws Exception {
        return dao.getSubjects();
    }

    public List<Subject> getUsers() throws Exception {
        return dao.getUsers();
    }
    
    public List<Subject> getGroups() throws Exception {
        return dao.getGroups();
    }
    
	public Set<String> getSubjectSet(String subjectKey) {
		return dao.getReaderSet(subjectKey);
	}

    public Subject getSubjectByKey(String subjectKey) {
        return dao.getSubjectByKey(subjectKey);
    }

    public Subject getSubjectByName(String subjectName) {
        return dao.getSubjectByName(subjectName);
    }
    
    public Subject getSubjectByNameOrKey(String subjectNameOrKey) {
        return dao.getSubjectByNameOrKey(subjectNameOrKey);
    }

    public Subject getUserByNameOrKey(String subjectNameOrKey) throws Exception {
        return dao.getUserByNameOrKey(subjectNameOrKey);
    }
    
    public Subject getGroupByNameOrKey(String subjectNameOrKey) throws Exception {
        return dao.getGroupByNameOrKey(subjectNameOrKey);
    }
    
    public Subject createUser(String newUserName, String newFullName, String email) throws Exception {
        Subject user = dao.createUser(newUserName, newFullName, email);
        // Create user's directory on the file system
        String fileNodeStorePath = SystemConfigurationProperties.getString(FILE_STORE_CENTRAL_DIR_PROP);
        File tmpUserDir = FileUtil.ensureDirExists(fileNodeStorePath + File.separator + newUserName);
        if (null!=user && null!=tmpUserDir && tmpUserDir.exists()) {
            return user;
        }
        return null;
    }
    
    public Subject createGroup(String userLogin, String groupName) throws Exception {
        return dao.createGroup(userLogin, groupName);
    }

    public void removeGroup(String groupNameOrKey) throws Exception {
        dao.removeGroup(groupNameOrKey);
    }
    
    public void addUserToGroup(String userNameOrKey, String groupNameOrKey, GroupRole role) throws Exception {
        dao.addUserToGroup(userNameOrKey, groupNameOrKey, role);
    }

    public void removeUserFromGroup(String userNameOrKey, String groupNameOrKey) throws Exception {
        dao.removeUserFromGroup(userNameOrKey, groupNameOrKey);
    }
    
    public void createWorkspace(String ownerKey) throws Exception {
        dao.createWorkspace(ownerKey);
    }
    
    public List<Workspace> getUserWorkspaces(String subjectKey) throws Exception {
        return dao.getWorkspaces(subjectKey);
    }

    public Workspace getDefaultWorkspace(String subjectKey) throws Exception {
        return dao.getDefaultWorkspace(subjectKey);
    }

    // User Preferences

    public List<Preference> getPreferences(String subjectKey) throws Exception {
        return dao.getPreferences(subjectKey);
    }
    
    public List<Preference> getPreferences(String subjectKey, String category) throws Exception {
        return dao.getPreferences(subjectKey, category);
    }

    public Object getPreferenceValue(String subjectKey, String category, String name) throws Exception {
        return dao.getPreferenceValue(subjectKey, category, name);
    }

    public void setPreferenceValue(String subjectKey, String category, String name, Object value) throws Exception {
        dao.setPreferenceValue(subjectKey, category, name, value);
    }
    
    public void setPermissions(String subjectKey, String className, Long id, String granteeKey, boolean read, boolean write) throws Exception {
        dao.setPermissions(subjectKey, className, id, granteeKey, read, write, true);
        DomainObject domainObj = dao.getDomainObject(subjectKey, Reference.createFor(className, id));
        // TODO: this is broken because it doesn't reindex related objects that may be shared by syncPermissions!
        IndexingHelper.sendReindexingMessage(domainObj);
    }
    
    public void addPermissions(String subjectKey, String className, Long id, DomainObject permissionTemplate) throws Exception {
        dao.addPermissions(subjectKey, className, id, permissionTemplate, true);
        DomainObject domainObj = dao.getDomainObject(subjectKey, Reference.createFor(className, id));
        // TODO: this is broken because it doesn't reindex related objects that may be shared by syncPermissions!
        IndexingHelper.sendReindexingMessage(domainObj);
    }


    public void addPipelineStatusTransition(Long sampleId, PipelineStatus source, PipelineStatus target, String orderNo,
                                            String process, Map<String,Object> parameters) throws Exception {
        dao.addPipelineStatusTransition(sampleId, source, target, orderNo, process, parameters);
    }

    public void addIntakeOrder(String orderNo, String owner) throws Exception {
        dao.addIntakeOrder(orderNo, owner);
    }

    public void updateIntakeOrder(IntakeOrder order) throws Exception {
        dao.updateIntakeOrder(order);
    }

    public List<IntakeOrder> getIntakeOrder(String orderNo) throws Exception {
        return dao.getIntakeOrder(orderNo);
    }

    public void addSampleToIntakeOrder(String orderNo, Long sampleId) throws Exception {
        dao.addSampleToOrder(orderNo, sampleId);
    }

    public List<StatusTransition> getPipelineStatusTransitionsBySampleId(Long sampleId) throws Exception {
        return dao.getPipelineStatusTransitionsBySampleId(sampleId);
    }
    
    // For other DAL's in this package only
    DomainDAO getDAO() {
        return dao;
    }
}
