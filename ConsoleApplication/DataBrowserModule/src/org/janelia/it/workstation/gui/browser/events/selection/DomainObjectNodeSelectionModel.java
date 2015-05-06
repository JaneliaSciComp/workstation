package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeSelectionModel extends SelectionModel<DomainObjectNode,DomainObjectId> {

    @Override
    protected void notify(DomainObjectNode domainObjectNode, DomainObjectId id, boolean select, boolean clearAll) {
        if (domainObjectNode instanceof ObjectSetNode) {
            ObjectSetNode objectSetNode = (ObjectSetNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), id, select, objectSetNode));
        }
        else if (domainObjectNode instanceof FilterNode) {
            FilterNode filterNode = (FilterNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new FilterSelectionEvent(getSource(), id, select, filterNode));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), id, domainObjectNode, select, clearAll));
        }
    }
    
    @Override
    public DomainObjectId getId(DomainObjectNode domainObjectNode) {
        DomainObject domainObject = domainObjectNode.getDomainObject();
        return DomainObjectId.createFor(domainObject);
    }
}
