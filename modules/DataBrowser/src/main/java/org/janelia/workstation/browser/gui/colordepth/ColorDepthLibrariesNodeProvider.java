package org.janelia.workstation.browser.gui.colordepth;

import java.util.ArrayList;
import java.util.List;

import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_COLOR_DEPTH_LIBRARIES;
import static org.janelia.workstation.core.options.OptionConstants.SHOW_COLOR_DEPTH_SEARCHES;

/**
 * Adds the color depth libraries and searches nodes to the Data Explorer.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class ColorDepthLibrariesNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 30;

    public List<NodeGenerator> getNodeGenerators() {

        List<NodeGenerator> generators = new ArrayList<>();
        if (FrameworkAccess.getModelProperty(SHOW_COLOR_DEPTH_LIBRARIES, true)) {
            generators.add(new NodeGenerator() {
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
        if (FrameworkAccess.getModelProperty(SHOW_COLOR_DEPTH_SEARCHES, true)) {
            generators.add(new NodeGenerator() {
                @Override
                public Integer getIndex() {
                    return NODE_ORDER+1;
                }
                @Override
                public Node createNode() {
                    return new ColorDepthSearchesNode();
                }
            });
        }
        return generators;
    }
}
