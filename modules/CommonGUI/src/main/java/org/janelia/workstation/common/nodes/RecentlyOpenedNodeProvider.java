package org.janelia.workstation.common.nodes;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_RECENTLY_OPENED_ITEMS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * Adds the recently opened items node to the Data Explorer at index 20.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class RecentlyOpenedNodeProvider implements NodeProvider  {
    
    private static final int NODE_ORDER = 20;
    
    public RecentlyOpenedNodeProvider() {
    }

    public List<NodeGenerator> getNodeGenerators() {
        if (!isShowRecentMenuItems()) return Collections.emptyList(); 
        return Arrays.asList(new NodeGenerator() {
            
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
