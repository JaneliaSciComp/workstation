package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

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
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;

/**
 * Interface for client implementations providing domain object access. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainFacade {

    /**
     * Returns all the subjects (i.e. users and groups) in the system.
     * @return list of Subject objects
     */
    public List<Subject> getSubjects();

    /**
     * Returns all the subjects (i.e. users and groups) in the system.
     * @return list of Subject objects
     */
    public Subject getSubjectByKey(String subjectKey);


    /**
     * Returns the current subject's preferences.
     * @param subjectId
     * @return
     */
    public List<Preference> getPreferences();

    /**
     * Saves the given preferences.
     * @param preference
     */
    public Preference savePreference(Preference preference) throws Exception;

    /**
     * Returns the domain object of a given class with the given GUID. 
     * @param domainClass class of domain object
     * @param id GUID of domain object
     * @return the domain object
     */
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id);

    /**
     * Returns the domain object specified by the given reference.
     * @param reference to a domain object
     * @return the domain object
     */
    public DomainObject getDomainObject(Reference reference);

    /**
     * Returns the domain objects specified by the given list of references. 
     * @param references list of references
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(List<Reference> references);

    /**
     * Returns the domain objects specified by the given reverse reference. 
     * @param reference reverse reference
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(ReverseReference reference);

    /**
     * Returns the domain objects of a particular type, given by the list of GUIDs. 
     * @param className class name
     * @param ids collection of GUIDs
     * @return list of domain objects
     */
    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids);

    /**
     * Returns all of the datasets that the current user owns.
     * @return list of datasets
     */
    public Collection<DataSet> getDataSets();


    /**
     * Returns all the annotations associated with all of the domain objects given by the given references.
     * @param targetIds collection of GUIDs
     * @return list of annotations
     */
    public List<Annotation> getAnnotations(Collection<Reference> references);

    /**
     * Return the current user's default workspace.
     * @return workspace
     */
    public Workspace getDefaultWorkspace();

    /**
     * Return all of the workspaces that the current user can access. 
     * @return list of workspaces
     */
    public Collection<Workspace> getWorkspaces();

    /**
     * Returns all of the ontologies that the current user can access. 
     * @return list of ontologies
     */
    public Collection<Ontology> getOntologies();

    /**
     * Creates and returns a new ontology, owned by the current user.
     * @param ontology
     * @return the ontology that was created
     * @throws Exception something went wrong
     */
    public Ontology create(Ontology ontology) throws Exception;

    /**
     * Reorder the child terms of a given parent term in a given ontology. 
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated ontology object
     * @throws Exception something went wrong
     */
    public Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception;

    /**
     * Add the given terms as children of the specified parent term, at some index. 
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param terms the new terms to add 
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated ontology object
     * @throws Exception something went wrong
     */
    public Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception;

    /**
     * Remove the term with the given id from the given parent in the given ontology.
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param termId the GUID of the term to remove
     * @return the updated ontology object 
     * @throws Exception something went wrong
     */
    public Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception;

    /**
     * Remove the ontology with the given GUID. 
     * @param ontologyId GUID of the ontology to remove
     * @throws Exception something went wrong
     */
    public void removeOntology(Long ontologyId) throws Exception;

    /**
     * Create and return a new annotation.
     * @param annotation annotation to create, with null GUID
     * @return the saved annotation
     * @throws Exception something went wrong
     */
    public Annotation create(Annotation annotation) throws Exception;

    /**
     * Update and return the given annotation. 
     * @param annotation the new annotation, with an existing GUID
     * @return the updated annotation
     * @throws Exception something went wrong
     */
    public Annotation update(Annotation annotation) throws Exception;

    /**
     * Remove the given annotation.
     * @param annotation annotation to remove
     * @throws Exception something went wrong
     */
    public void remove(Annotation annotation) throws Exception;

    /**
     * Remove the given domainobject.
     * @param dataSet domainobject to remove
     * @throws Exception something went wrong
     */
    public void remove(DataSet dataSet) throws Exception;


    /**
     * Create a new object set. 
     * @param objectSet the object set to create, with null GUID
     * @return the saved object set
     * @throws Exception
     */
    public ObjectSet create(ObjectSet objectSet) throws Exception;

    /**
     * Create a new dataset set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    public DataSet create(DataSet dataSet) throws Exception;

    /**
     * Update and return the given dataset set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    public DataSet update(DataSet dataSet) throws Exception;


    /**
     * Create and return a new filter. 
     * @param filter the filter to create
     * @return the saved filter
     * @throws Exception something went wrong
     */
    public Filter create(Filter filter) throws Exception;

    /**
     * Update and return the given filter.
     * @param filter the filter to update, with an existing GUID
     * @return the saved filter
     * @throws Exception something went wrong
     */
    public Filter update(Filter filter) throws Exception;

    /**
     * Create and return a new tree node. 
     * @param treeNode the tree node to create
     * @return the saved tree node
     * @throws Exception something went wrong
     */
    public TreeNode create(TreeNode treeNode) throws Exception;

    /**
     * Reorder the children of the given tree node. 
     * @param treeNode the tree node
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated tree node object
     * @throws Exception something went wrong
     */
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception;

    /**
     * Add the given references as children of the specified tree node, at some index. 
     * @param treeNode the tree node 
     * @param references collection of references to add
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated tree node
     * @throws Exception something went wrong
     */
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references, Integer index) throws Exception;

    /**
     * Remove the given children from the given tree node. 
     * @param treeNode the tree node
     * @param references collection of references to remove
     * @return the updated tree node
     * @throws Exception something went wrong
     */
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception;

    /**
     * Add the specified domain objects as members of the given object set. 
     * @param objectSet the object set
     * @param references collection of references to domain objects
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;

    /**
     * Remove the specified domain objects from the given object set. 
     * @param objectSet the object set
     * @param references collection of references to domain objects
     * @return the updated object set
     * @throws Exception something went wrong
     */
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception;

    /**
     * Update a property on the given domain object.
     * @param domainObject domain object to update
     * @param propName name of property to update
     * @param propValue new property value
     * @return the updated domain object
     */
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue);

    /**
     * Update the permissions on the given domain object to grant or revoke rights to some subject. 
     * @param domainObject the domain object for which to change permissions 
     * @param granteeKey the subject key being granted or revoked permission
     * @param rights list of access rights, e.g. "rw"
     * @param grant grant or revoke?
     * @throws Exception something went wrong
     */
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception;


}
