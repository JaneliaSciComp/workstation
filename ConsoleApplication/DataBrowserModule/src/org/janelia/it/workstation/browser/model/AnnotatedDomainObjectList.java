package org.janelia.it.workstation.browser.model;

import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;

/**
 * An ordered list of domain objects with associated annotations.
 * 
 * The component that manages this list is responsible for listening to changes to the 
 * annotations on the domain objects in this list (i.e. DomainObjectAnnotationChangeEvents),
 * and updating them with the updateAnnotations method.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedDomainObjectList {
    
    public List<DomainObject> getDomainObjects();
    
    /**
     * Returns the annotations for the domain object identifier by the given GUID
     * @param domainObjectId GUID
     * @return anotations 
     */
    public List<Annotation> getAnnotations(Long domainObjectId);
    
    /**
     * Get the domain object identified by the given GUID.
     * @param domainObjectId GUID
     * @return the domain object
     */
    public DomainObject getDomainObject(Long domainObjectId);

    /**
     * Update the given domain object.
     * @param updatedObject the new object possessing the same id as an existing object
     * @return true if the domain object id is found
     */
    public boolean updateObject(DomainObject updatedObject);
    
    /**
     * Update the annotations for a given domain object. 
     * @param domainObjectId GUID for the domain object to update
     * @param annotations the new set of annotations for the given domain object
     * @return true if the domain object id is found
     */
    public boolean updateAnnotations(Long domainObjectId, List<Annotation> annotations);
    
}
