package org.janelia.workstation.browser.nodes;

import java.util.Collections;
import java.util.List;

import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_COLOR_DEPTH_LIBRARIES;

/**
 * Adds the color depth libraries node to the Data Explorer.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class ColorDepthLibrariesNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 22;

    public ColorDepthLibrariesNodeProvider() {
    }

    public List<NodeGenerator> getNodeGenerators() {
        if (!isShowMenuItem()) return Collections.emptyList();
        return Collections.singletonList(new NodeGenerator() {

            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }

            @Override
            public Node createNode() {
                return new ColorDepthLibrariesNode();
            }
        });
    }
    
    public static boolean isShowMenuItem() {
        return FrameworkAccess.getModelProperty(SHOW_COLOR_DEPTH_LIBRARIES, true);
    }
    
}
