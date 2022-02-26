package org.janelia.workstation.browser.nodes;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_SYNCHED_ROOTS;

/**
 * Adds the Synchronized Folders node to the Data Explorer.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class SyncedRootsNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 30;
    private static SyncedRootsNode SYNCED_ROOTS_NODE_INSTANCE;

    List<NodeGenerator> generators = new ArrayList<>();

    public SyncedRootsNodeProvider() {
        generators.add(new NodeGenerator() {
            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }
            @Override
            public Node createNode() {
                synchronized (SyncedRootsNode.class) {
                    if (SYNCED_ROOTS_NODE_INSTANCE != null) {
                        Events.getInstance().unregisterOnEventBus(SYNCED_ROOTS_NODE_INSTANCE);
                    }
                    SYNCED_ROOTS_NODE_INSTANCE = new SyncedRootsNode();
                    Events.getInstance().registerOnEventBus(SYNCED_ROOTS_NODE_INSTANCE);
                    return SYNCED_ROOTS_NODE_INSTANCE;
                }
            }
            @Override
            public NodePreference getNodePreference() {
                return new NodePreference() {
                    @Override
                    public String getNodeName() {
                        return SyncedRootsNode.NODE_NAME;
                    }
                    @Override
                    public boolean isNodeShown() {
                        return FrameworkAccess.getModelProperty(SHOW_SYNCHED_ROOTS, true);
                    }
                    @Override
                    public void setNodeShown(boolean value) {
                        FrameworkAccess.setModelProperty(SHOW_SYNCHED_ROOTS, value);
                    }
                };
            }
        });
    }

    public List<NodeGenerator> getNodeGenerators() {
        return generators;
    }
}
