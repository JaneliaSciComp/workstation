package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.events.Events;

/**
 * A selection model implementation which tracks the selection of domain objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel extends SelectionModel<DomainObject,DomainObjectId> {

    @Override
    protected void notify(DomainObject domainObject, DomainObjectId id, boolean select, boolean clearAll) {
        if (domainObject instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)domainObject;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), id, select, objectSet));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), id, domainObject, select, clearAll));
        }
    }
    
    @Override
    public DomainObjectId getId(DomainObject domainObject) {
        return DomainObjectId.createFor(domainObject);
    }
}
