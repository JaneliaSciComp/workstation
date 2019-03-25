package org.janelia.it.workstation.browser.events.selection;

import java.util.List;

import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.FilterNode;
import org.janelia.it.workstation.browser.nodes.IdentifiableNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A selection model implementation which tracks the selection of domain object nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IdentifiableNodeSelectionModel extends SelectionModel<Node,Long> {

    private static final Logger log = LoggerFactory.getLogger(IdentifiableNodeSelectionModel.class);
    
    @Override
    protected void selectionChanged(List<Node> nodes, boolean select, boolean clearAll, boolean isUserDriven) {
        log.debug((select?"select":"deselect")+" {}, clearAll={}",nodes,clearAll);
        if (nodes.size()==1) {
            Node node = nodes.get(0);
            if (node instanceof TreeNodeNode) {
                TreeNodeNode treeNodeNode = (TreeNodeNode)node;
                Events.getInstance().postOnEventBus(new TreeNodeSelectionEvent(getSource(), treeNodeNode, select, clearAll, isUserDriven));
            }
            else if (node instanceof FilterNode) {
                FilterNode filterNode = (FilterNode)node;
                Events.getInstance().postOnEventBus(new FilterSelectionEvent(getSource(), filterNode, select, clearAll, isUserDriven));
            }
            else if (node instanceof AbstractDomainObjectNode) {
                AbstractDomainObjectNode<?> domainObjectNode = (AbstractDomainObjectNode)node;
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(getSource(), domainObjectNode, select, clearAll, isUserDriven));
            }
            else if (node instanceof IdentifiableNode) {
                IdentifiableNode identifiableNode = (IdentifiableNode)node;
                Events.getInstance().postOnEventBus(new NodeSelectionEvent(getSource(), identifiableNode, select, clearAll, isUserDriven));
            }
            else {
                log.debug("Model does not support node type: "+node);
            }
        }
    }
    
    @Override
    public Long getId(Node node) {
        HasIdentifier hasId = (HasIdentifier)node;
        return hasId.getId();
    }
}
