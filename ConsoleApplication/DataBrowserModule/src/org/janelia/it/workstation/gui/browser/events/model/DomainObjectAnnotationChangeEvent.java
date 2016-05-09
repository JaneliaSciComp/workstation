package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * The annotations on a domain object have changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectAnnotationChangeEvent extends DomainObjectEvent {
    public DomainObjectAnnotationChangeEvent(DomainObject domainObject) {
        super(domainObject);
    }
}
