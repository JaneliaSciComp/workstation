package org.janelia.workstation.common.nodes;

import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import java.util.Collections;
import java.util.List;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_RECENTLY_OPENED_ITEMS;

/**
 * Adds the recently opened items node to the Data Explorer.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class RecentlyOpenedNodeProvider implements NodeProvider  {
    
    private static final int NODE_ORDER = 10;

    public List<NodeGenerator> getNodeGenerators() {
        return Collections.singletonList(new NodeGenerator() {
            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }
            @Override
            public Node createNode() {
                return new RecentlyOpenedItemsNode();
            }
            @Override
            public NodePreference getNodePreference() {
                return new NodePreference() {
                    @Override
                    public String getNodeName() {
                        return RecentlyOpenedItemsNode.NODE_NAME;
                    }
                    @Override
                    public boolean isNodeShown() {
                        return FrameworkAccess.getModelProperty(SHOW_RECENTLY_OPENED_ITEMS, true);
                    }
                    @Override
                    public void setNodeShown(boolean value) {
                        FrameworkAccess.setModelProperty(SHOW_RECENTLY_OPENED_ITEMS, value);
                    }
                };
            }
        });
    }
}
