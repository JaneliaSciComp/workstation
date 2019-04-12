package org.janelia.workstation.browser.nodes;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_RECENTLY_OPENED_ITEMS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.nodes.NodeGenerator;
import org.janelia.it.jacs.integration.framework.nodes.NodeProvider;
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
                return new RecentOpenedItemsNode();
            }
        });
    }
    
    public static boolean isShowRecentMenuItems() {
        Boolean navigate = (Boolean) FrameworkImplProvider.getModelProperty(SHOW_RECENTLY_OPENED_ITEMS);
        return navigate==null || navigate;
    }
    
}
