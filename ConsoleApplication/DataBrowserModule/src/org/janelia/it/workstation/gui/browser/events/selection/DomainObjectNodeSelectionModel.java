package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeSelectionModel implements SelectionModel<DomainObjectNode,DomainObjectId> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectNodeSelectionModel.class);
    
    private final SelectionType selectionType;
    private final List<DomainObjectId> selected = new ArrayList<>();
    
    public DomainObjectNodeSelectionModel(SelectionType selectionType) {
        this.selectionType = selectionType;
    }
    
    @Override
    public void select(DomainObjectNode domainObjectNode, boolean clearAll) {
        DomainObjectId id = getId(domainObjectNode);
        if (clearAll && selected.contains(id)) {
            // Already selected
            return;
        }
        if (clearAll) {
            selected.clear();
        }
        selected.add(id);
        
        if (domainObjectNode instanceof ObjectSetNode) {
            ObjectSetNode objectSetNode = (ObjectSetNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(id, objectSetNode));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(id, domainObjectNode, selectionType, clearAll));
        }
    }

    @Override
    public void deselect(DomainObjectNode domainObjectNode) {
        DomainObjectId identifier = getId(domainObjectNode);
        if (!selected.contains(identifier)) {
            return;
        }
        selected.remove(identifier);
    }
    
    @Override
    public DomainObjectId getId(DomainObjectNode domainObjectNode) {
        DomainObject domainObject = domainObjectNode.getDomainObject();
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
