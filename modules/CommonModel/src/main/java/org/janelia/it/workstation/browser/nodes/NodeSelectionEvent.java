package org.janelia.it.workstation.browser.nodes;

import org.janelia.it.workstation.browser.nodes.IdentifiableNode;

/**
 * Event indicating that a node's selection has changed.
 *
 * TODO: rename this to IdentifiableNodeSelectionEvent to reduce confusion
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeSelectionEvent {

    private final Object source;
    private final IdentifiableNode node;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;

    public NodeSelectionEvent(Object source, IdentifiableNode node, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.node = node;
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }
    
    public Object getSource() {
        return source;
    }
    
    public IdentifiableNode getNode() {
        return node;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "NodeSelectionEvent[source=" + source.getClass().getSimpleName() + ", node=" + node 
                + ", select=" + select + ", clearAll=" + clearAll + ", isUserDriven=" + isUserDriven + "]";
    }
}
