package org.janelia.it.workstation.gui.browser.events.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * A domain object or part of its object graph has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectChangeEvent extends DomainObjectEvent {
    public DomainObjectChangeEvent(DomainObject domainObject) {
        super(domainObject);
    }
}
