package org.janelia.workstation.colordepth.gui;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_COLOR_DEPTH_LIBRARIES;

/**
 * Adds the color depth libraries and searches nodes to the Data Explorer.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class ColorDepthNodesProvider implements NodeProvider  {

    private static final int NODE_ORDER = 30;
    private static ColorDepthLibrariesNode LIBRARIES_NODE_INSTANCE;
    private static ColorDepthSearchesNode SEARCHES_NODE_INSTANCE;

    List<NodeGenerator> generators = new ArrayList<>();

    public ColorDepthNodesProvider() {
        generators.add(new NodeGenerator() {
            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }
            @Override
            public Node createNode() {
                synchronized (ColorDepthNodesProvider.class) {
                    if (LIBRARIES_NODE_INSTANCE != null) {
                        Events.getInstance().unregisterOnEventBus(LIBRARIES_NODE_INSTANCE);
                    }
                    LIBRARIES_NODE_INSTANCE = new ColorDepthLibrariesNode();
                    Events.getInstance().registerOnEventBus(LIBRARIES_NODE_INSTANCE);
                    return LIBRARIES_NODE_INSTANCE;
                }
            }
            @Override
            public NodePreference getNodePreference() {
                return new NodePreference() {
                    @Override
                    public String getNodeName() {
                        return ColorDepthLibrariesNode.NODE_NAME;
                    }
                    @Override
                    public boolean isNodeShown() {
                        return FrameworkAccess.getModelProperty(SHOW_COLOR_DEPTH_LIBRARIES, true);
                    }
                    @Override
                    public void setNodeShown(boolean value) {
                        FrameworkAccess.setModelProperty(SHOW_COLOR_DEPTH_LIBRARIES, value);
                    }
                };
            }
        });
        generators.add(new NodeGenerator() {
            @Override
            public Integer getIndex() {
                return NODE_ORDER+1;
            }
            @Override
            public Node createNode() {
                synchronized (ColorDepthNodesProvider.class) {
                    if (SEARCHES_NODE_INSTANCE != null) {
                        Events.getInstance().unregisterOnEventBus(SEARCHES_NODE_INSTANCE);
                    }
                    SEARCHES_NODE_INSTANCE = new ColorDepthSearchesNode();
                    Events.getInstance().registerOnEventBus(SEARCHES_NODE_INSTANCE);
                    return SEARCHES_NODE_INSTANCE;
                }
            }
        });
    }

    public List<NodeGenerator> getNodeGenerators() {
        return generators;
    }
}
