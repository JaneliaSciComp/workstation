package org.janelia.workstation.common.nodes;

import java.util.Collections;
import java.util.List;

import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_RECENTLY_OPENED_ITEMS;

/**
 * Adds the recently opened items node to the Data Explorer.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class RecentlyOpenedNodeProvider implements NodeProvider  {
    
    private static final int NODE_ORDER = 10;
    
    public RecentlyOpenedNodeProvider() {
    }

    public List<NodeGenerator> getNodeGenerators() {
        if (!isShowRecentMenuItems()) return Collections.emptyList(); 
        return Collections.singletonList(new NodeGenerator() {

            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }

            @Override
            public Node createNode() {
                return new RecentlyOpenedItemsNode();
            }
        });
    }
    
    public static boolean isShowRecentMenuItems() {
        return FrameworkAccess.getModelProperty(SHOW_RECENTLY_OPENED_ITEMS, true);
    }
    
}
