package org.janelia.workstation.browser.nodes;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import java.util.Collections;
import java.util.List;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_DATA_SETS;

/**
 * Adds the data sets node to the Data Explorer.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class DataSetsNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 20;
    private static DataSetsNode NODE_INSTANCE;

    public List<NodeGenerator> getNodeGenerators() {
        return Collections.singletonList(new NodeGenerator() {
            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }
            @Override
            public Node createNode() {
                synchronized (DataSetsNodeProvider.class) {
                    if (NODE_INSTANCE != null) {
                        Events.getInstance().unregisterOnEventBus(NODE_INSTANCE);
                    }
                    NODE_INSTANCE = new DataSetsNode();
                    Events.getInstance().registerOnEventBus(NODE_INSTANCE);
                    return NODE_INSTANCE;
                }
            }
            @Override
            public NodePreference getNodePreference() {
                return new NodePreference() {
                    @Override
                    public String getNodeName() {
                        return DataSetsNode.NODE_NAME;
                    }
                    @Override
                    public boolean isNodeShown() {
                        return FrameworkAccess.getModelProperty(SHOW_DATA_SETS, true);
                    }
                    @Override
                    public void setNodeShown(boolean value) {
                        FrameworkAccess.setModelProperty(SHOW_DATA_SETS, value);
                    }
                };
            }
        });
    }
}
