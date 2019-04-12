package org.janelia.workstation.core.events.model;

import org.janelia.model.domain.DomainObject;

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
