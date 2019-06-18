package org.janelia.workstation.core.model;

import java.util.List;

import org.janelia.model.domain.ontology.Annotation;

/**
 * An ordered list of objects with associated annotations.
 * 
 * The component that manages this list is responsible for listening to changes to the 
 * annotations on the objects in this list (i.e. DomainObjectAnnotationChangeEvents),
 * and updating them with the updateAnnotations method.
 * 
 * T - the type of the objects
 * S - the type of the unique identifiers for the objects
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedObjectList<T,S> {
    
    public List<T> getObjects();
    
    /**
     * Returns the annotations for the given object identifier. 
     * @param objectId unique identifier for the object
     * @return annotations
     */
    public List<Annotation> getAnnotations(S objectId);
    
    /**
     * Get the object identified by the given unique id.
     * @param objectId unique object id
     * @return the object
     */
    public T getObjectById(S objectId);
    
    /**
     * Update the given object.
     * @param updatedObject the new object possessing the same id as an existing object
     * @return true if the object is found and successfully updated
     */
    public boolean updateObject(T updatedObject);
    
    /**
     * Update the annotations for a given object. 
     * @param objectId GUID for the object to update
     * @param annotations the new set of annotations for the given object
     * @return true if the object id is found
     */
    public boolean updateAnnotations(S objectId, List<Annotation> annotations);
    
}
