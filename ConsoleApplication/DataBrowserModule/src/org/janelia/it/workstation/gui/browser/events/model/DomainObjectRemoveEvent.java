package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * A domain object has been removed from a container.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectRemoveEvent extends DomainObjectEvent {
    public DomainObjectRemoveEvent(DomainObject domainObject) {
        super(domainObject);
    }
}
