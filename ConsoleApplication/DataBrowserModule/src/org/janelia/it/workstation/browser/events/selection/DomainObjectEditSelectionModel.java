package org.janelia.it.workstation.browser.events.selection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * A selection model implementation which tracks the selection of domain objects for editing.
 * 
 * Unlike the DomainObjectSelectionModel, this does not notify the event bus. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditSelectionModel extends ChildSelectionModel<DomainObject,Reference> {
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
