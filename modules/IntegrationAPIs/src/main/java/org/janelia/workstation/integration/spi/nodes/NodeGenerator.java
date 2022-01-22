package org.janelia.workstation.integration.spi.nodes;

import org.openide.nodes.Node;

/**
 * A node generator creates a node when requested. It also specifies a suggested
 * index for ordering nodes in the Data Explorer.
 */
public interface NodeGenerator {

    /**
     * Global index for ordering the node within the top-level of the Data Explorer tree.
     * @return index for order the node
     */
    Integer getIndex();

    /**
     * Generate and return the node.
     * @return new Node
     */
    Node createNode();

    /**
     * Returns the user preference controller for this node. Null by default, it can be implemented
     * to allow the user to toggle the node on and off from the Data Explorer config menu.
     * @return NodePreference or null if node is not user controllable
     */
    default NodePreference getNodePreference() {
        return null;
    };

}