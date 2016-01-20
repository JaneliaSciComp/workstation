package org.janelia.it.workstation.gui.browser.model;

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

    public Class<? extends DomainObject> getDomainClass();
    
    public List<DomainObject> getDomainObjects();
    
    public List<Annotation> getAnnotations(Long domainObjectId);
    
    public DomainObject getDomainObject(Long domainObjectId);
    
    public void updateAnnotations(Long domainObjectId, List<Annotation> annotations);
    
}
