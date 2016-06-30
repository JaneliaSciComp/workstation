package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
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
    protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjects, select, clearAll, isUserDriven));
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
