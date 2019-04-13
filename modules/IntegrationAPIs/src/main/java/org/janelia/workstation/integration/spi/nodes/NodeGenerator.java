package org.janelia.workstation.integration.spi.nodes;

import org.openide.nodes.Node;

/**
 * A node generator creates a node when requested. It also specifies a suggested
 * index for ordering nodes in the Data Explorer.
 */
public interface NodeGenerator {

    Integer getIndex();

    Node createNode();

}