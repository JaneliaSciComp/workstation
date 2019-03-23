package org.janelia.it.workstation.browser.events.selection;

import java.util.List;

import org.janelia.it.workstation.browser.events.Events;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * A selection model implementation which tracks the selection of domain objects for editing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditSelectionModel extends ChildSelectionModel<DomainObject,Reference> {

    @Override
    protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        Events.getInstance().postOnEventBus(new DomainObjectEditSelectionEvent(getSource(), domainObjects, select, clearAll, isUserDriven));
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
