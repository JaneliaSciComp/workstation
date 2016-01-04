package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;

/**
 * A selection model implementation which tracks the selection of domain object nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectNodeSelectionModel extends SelectionModel<DomainObjectNode,Reference> {

    @Override
    protected void selectionChanged(DomainObjectNode domainObjectNode, Reference id, boolean select, boolean clearAll) {
        if (domainObjectNode instanceof ObjectSetNode) {
            ObjectSetNode objectSetNode = (ObjectSetNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new ObjectSetSelectionEvent(getSource(), select, objectSetNode));
        }
        else if (domainObjectNode instanceof FilterNode) {
            FilterNode filterNode = (FilterNode)domainObjectNode;
            Events.getInstance().postOnEventBus(new FilterSelectionEvent(getSource(), select, filterNode));
        }
        else {
            Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjectNode, select, clearAll));
        }
    }
    
    @Override
    public Reference getId(DomainObjectNode domainObjectNode) {
        DomainObject domainObject = domainObjectNode.getDomainObject();
        return Reference.createFor(domainObject);
    }
}
