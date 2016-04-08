package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;

/**
 * Implementations provide access to ontologies and annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface OntologyFacade {

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
     * Remove the ontology with the given GUID. 
     * @param ontologyId GUID of the ontology to remove
     * @throws Exception something went wrong
     */
    public void removeOntology(Long ontologyId) throws Exception;
    
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
     * Returns all the annotations associated with all of the domain objects given by the given references.
     * @param targetIds collection of GUIDs
     * @return list of annotations
     */
    public List<Annotation> getAnnotations(Collection<Reference> references);
    
    /**
     * Create an annotation against the given target object.
     * @param target reference to the domain object that needs to be annotated
     * @param ontologyTermReference reference to the ontology term to be used as the key term
     * @param value the value of the annotation
     * @return
     * @throws Exception
     */
    public Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, Object value) throws Exception;
    
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
    
}
