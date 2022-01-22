package org.janelia.workstation.site.jrc.nodes;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import java.util.Collections;
import java.util.List;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_FLY_LINE_RELEASES;

/**
 * Adds the fly line releases node to the Data Explorer.
 */
@SuppressWarnings("unused")
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class FlyLineReleasesNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 40;
    private static FlyLineReleasesNode NODE_INSTANCE;

    public List<NodeGenerator> getNodeGenerators() {
        return Collections.singletonList(new NodeGenerator() {

            @Override
            public Integer getIndex() {
                return NODE_ORDER;
            }
            @Override
            public Node createNode() {
                synchronized (FlyLineReleasesNodeProvider.class) {
                    if (NODE_INSTANCE != null) {
                        Events.getInstance().unregisterOnEventBus(NODE_INSTANCE);
                    }
                    NODE_INSTANCE = new FlyLineReleasesNode();
                    Events.getInstance().registerOnEventBus(NODE_INSTANCE);
                    return NODE_INSTANCE;
                }
            }
            @Override
            public NodePreference getNodePreference() {
                return new NodePreference() {
                    @Override
                    public String getNodeName() {
                        return FlyLineReleasesNode.NODE_NAME;
                    }
                    @Override
                    public boolean isNodeShown() {
                        return FrameworkAccess.getModelProperty(SHOW_FLY_LINE_RELEASES, true);
                    }
                    @Override
                    public void setNodeShown(boolean value) {
                        FrameworkAccess.setModelProperty(SHOW_FLY_LINE_RELEASES, value);
                    }
                };
            }
        });
    }
}
