package org.janelia.it.workstation.gui.browser.api.facade.impl;

import java.lang.Override;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.jacs.shared.security.LDAPProvider;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.jacs.shared.security.LDAPProvider;
import org.janelia.it.jacs.shared.security.BasicAuthToken;

/**
 * Implementation of the DomainFacade using a direct MongoDB connection.
 *
 * NOT FOR PRODUCTION USE!
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDomainFacade implements DomainFacade {

    protected static final String MONGO_SERVER_URL = "mongodb1";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "flyportal";
    protected static final String MONGO_PASSWORD = "flyportal";

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
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id) {
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainClass, id);
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
    public Ontology create(Ontology ontology) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), ontology);
    }

    @Override
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception {
        return dao.reorderTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, order);
    }

    @Override
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception {
        return dao.addTerms(AccessManager.getSubjectKey(), ontologyId, parentTermId, terms, index);
    }

    @Override
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception {
        return dao.removeTerm(AccessManager.getSubjectKey(), ontologyId, parentTermId, termId);
    }

    @Override
    public void removeOntology(Long ontologyId) throws Exception {
        Ontology ontology = dao.getDomainObject(AccessManager.getSubjectKey(), Ontology.class, ontologyId);
        dao.remove(AccessManager.getSubjectKey(), ontology);
    }

    @Override
    public Annotation create(Annotation annotation) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), annotation);
    }

    @Override
    public Annotation update(Annotation annotation) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), annotation);
    }

    @Override
    public void remove(Annotation annotation) throws Exception {
        dao.remove(AccessManager.getSubjectKey(), annotation);
    }

    @Override
    public void remove(DataSet dataSet) throws Exception {
        dao.remove(AccessManager.getSubjectKey(), dataSet);
    }


    @Override
    public ObjectSet create(ObjectSet objectSet) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), objectSet);
    }

    @Override
    public DataSet create(DataSet dataSet) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), dataSet);
    }

    @Override
    public DataSet update(DataSet dataSet) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), dataSet);
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), filter);
    }

    @Override
    public Filter update(Filter filter) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), filter);
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), treeNode);
    }

    @Override
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        return dao.reorderChildren(AccessManager.getSubjectKey(), treeNode, order);
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception {
        return dao.addChildren(AccessManager.getSubjectKey(), treeNode, references, index);
    }

    @Override
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        return dao.removeChildren(AccessManager.getSubjectKey(), treeNode, references);
    }

    @Override
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        return dao.addMembers(AccessManager.getSubjectKey(), objectSet, references);
    }

    @Override
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        return dao.removeMembers(AccessManager.getSubjectKey(), objectSet, references);
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        return dao.updateProperty(AccessManager.getSubjectKey(), domainObject.getClass().getName(), domainObject.getId(), propName, propValue);
    }

    @Override
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        dao.changePermissions(AccessManager.getSubjectKey(), domainObject.getClass().getName(), Arrays.asList(domainObject.getId()), granteeKey, rights, grant);
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainObject);
    }
}
