package org.janelia.it.workstation.browser.nodes;

import java.util.List;

import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.selection.SelectionModel;
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
            if (node instanceof IdentifiableNode) {
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
