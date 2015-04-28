package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectSelectionModel implements SelectionModel<DomainObject,DomainObjectId> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectSelectionModel.class);
    
    private final SelectionType selectionType;
    private final List<DomainObjectId> selected = new ArrayList<>();
    
    public DomainObjectSelectionModel(SelectionType selectionType) {
        this.selectionType = selectionType;
    }
    
    @Override
    public void select(DomainObject domainObject, boolean clearAll) {
        DomainObjectId id = getId(domainObject);
        if (clearAll && selected.contains(id)) {
            // Already selected
            return;
        }
        if (clearAll) {
            selected.clear();
        }
        selected.add(id);
        
        if (domainObject instanceof ObjectSet) {
            ObjectSet objectSet = (ObjectSet)domainObject;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(id, objectSet));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(id, domainObject, selectionType, clearAll));
        }
    }

    @Override
    public void deselect(DomainObject domainObject) {
        DomainObjectId identifier = getId(domainObject);
        if (!selected.contains(identifier)) {
            return;
        }
        selected.remove(identifier);
    }
    
    @Override
    public DomainObjectId getId(DomainObject domainObject) {
        return DomainObjectId.createFor(domainObject);
    }

    @Override
    public List<DomainObjectId> getSelectedIds() {
        return selected;
    }
    

    @Override
    public boolean isSelected(DomainObjectId identifier) {
        return selected.contains(identifier);
    }
    
}
