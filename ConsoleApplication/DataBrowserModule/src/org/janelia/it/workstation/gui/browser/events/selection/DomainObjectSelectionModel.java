package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A selection model implementation which tracks the selection of domain objects, including
 * a single parent object which contains multiple child objects. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel extends SelectionModel<DomainObject,Reference> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectSelectionModel.class);
    
    private IsParent parentObject;
    
    public IsParent getParentObject() {
        return parentObject;
    }

    public void setParentObject(IsParent parentObject) {
        this.parentObject = parentObject;
    }

    @Override
    protected void selectionChanged(DomainObject domainObject, Reference id, boolean select, boolean clearAll) {
        log.debug((select?"select":"deselect")+" {}, clearAll={}",id,clearAll);
        if (domainObject instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)domainObject;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), select, objectSet));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObject, select, clearAll));
        }
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
