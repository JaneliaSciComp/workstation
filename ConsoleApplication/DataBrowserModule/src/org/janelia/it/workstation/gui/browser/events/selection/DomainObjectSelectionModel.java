package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.events.Events;

/**
 * A selection model implementation which tracks the selection of domain objects, including
 * a single parent object which contains multiple child objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel extends SelectionModel<DomainObject,Reference> {

    private IsParent parentObject;
    
    public IsParent getParentObject() {
        return parentObject;
    }

    public void setParentObject(IsParent parentObject) {
        this.parentObject = parentObject;
    }

    @Override
    protected void notify(DomainObject domainObject, Reference id, boolean select, boolean clearAll) {
        if (domainObject instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)domainObject;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), id, select, objectSet));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), id, domainObject, select, clearAll));
        }
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
