package org.janelia.workstation.core.events.model;

import org.janelia.model.domain.DomainObject;

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
