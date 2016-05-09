package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * An event in the domain object model affecting the given domain object in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectEvent {

    private DomainObject domainObject;
    
    public DomainObjectEvent(DomainObject domainObject) {
        this.domainObject = domainObject;
    }
           
    public DomainObject getDomainObject() {
        return domainObject;
    }
}
