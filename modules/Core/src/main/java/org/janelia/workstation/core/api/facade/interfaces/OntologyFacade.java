package org.janelia.workstation.core.api.facade.interfaces;

import java.util.Collection;
import java.util.List;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;

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
    List<Ontology> getOntologies();

    /**
     * Creates and returns a new ontology, owned by the current user.
     * @param ontology
     * @return the ontology that was created
     * @throws Exception something went wrong
     */
    Ontology create(Ontology ontology) throws Exception;

    /**
     * Remove the ontology with the given GUID. 
     * @param ontologyId GUID of the ontology to remove
     * @throws Exception something went wrong
     */
    void removeOntology(Long ontologyId) throws Exception;
    
    /**
     * Add the given terms as children of the specified parent term, at some index. 
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param terms the new terms to add 
     * @param index the index at which to insert the new children, or null to add them at the end
     * @return the updated ontology object
     * @throws Exception something went wrong
     */
    Ontology addTerms(Long ontologyId, Long parentTermId, Collection<OntologyTerm> terms, Integer index) throws Exception;

    /**
     * Remove the term with the given id from the given parent in the given ontology.
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param termId the GUID of the term to remove
     * @return the updated ontology object 
     * @throws Exception something went wrong
     */
    Ontology removeTerm(Long ontologyId, Long parentTermId, Long termId) throws Exception;

    /**
     * Reorder the child terms of a given parent term in a given ontology. 
     * @param ontologyId the GUID of an ontology containing the parent term
     * @param parentTermId the GUID of the parent term
     * @param order permutation with the length of current children. The permutation lists the new positions of the original children, 
     * that is, for children <code>[A,B,C,D]</code> and permutation <code>[0,3,1,2]</code>, the final order would be <code>[A,C,D,B]</code>.
     * @return the updated ontology object
     * @throws Exception something went wrong
     */
    Ontology reorderTerms(Long ontologyId, Long parentTermId, int[] order) throws Exception;

    /**
     * Returns all the annotations associated with all of the domain objects given by the given references.
     * @param references collection of references to domain objects
     * @return list of annotations
     */
    List<Annotation> getAnnotations(Collection<Reference> references) throws Exception;
    
    /**
     * Create an annotation against the given target object.
     * @param target reference to the domain object that needs to be annotated
     * @param ontologyTermReference reference to the ontology term to be used as the key term
     * @param value the value of the annotation
     * @return
     * @throws Exception
     */
    Annotation createAnnotation(Reference target, OntologyTermReference ontologyTermReference, String value) throws Exception;

    /**
     * Update the value for the given annotation.
     * @param annotation annotation to change
     * @param newValue new value to set
     * @return
     */
    Annotation updateAnnotation(Annotation annotation, String newValue) throws Exception;


    /**
     * Remove the given annotation.
     * @param annotation annotation to remove
     * @throws Exception something went wrong
     */
    void removeAnnotation(Annotation annotation) throws Exception;
}
