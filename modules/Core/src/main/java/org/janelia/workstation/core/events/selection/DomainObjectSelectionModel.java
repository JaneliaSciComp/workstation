package org.janelia.workstation.core.events.selection;

import java.util.List;

import org.janelia.workstation.core.events.Events;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * A selection model implementation which tracks the selection of domain objects, including
 * a single parent object which contains multiple child objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel extends ChildSelectionModel<DomainObject,Reference> {

    @Override
    protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjects, select, clearAll, isUserDriven));
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
