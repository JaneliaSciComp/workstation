package org.janelia.it.workstation.gui.browser.model;

import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;

/**
 * An ordered list of domain objects with associated annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotatedDomainObjectList {

    public List<DomainObject> getDomainObjects();
    
    public List<Annotation> getAnnotations(Long domainObjectId);
    
    public DomainObject getDomainObject(Long domainObjectId);
    
}
