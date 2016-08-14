package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * A new domain object has been created.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectCreateEvent extends DomainObjectEvent {
    public DomainObjectCreateEvent(DomainObject domainObject) {
        super(domainObject);
    }
}
