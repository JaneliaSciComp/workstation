package org.janelia.workstation.site.jrc.nodes;

import java.util.Collections;
import java.util.List;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

import static org.janelia.workstation.core.options.OptionConstants.SHOW_FLY_LINE_RELEASES;

/**
 * Adds the fly line releases node to the Data Explorer.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class FlyLineReleasesNodeProvider implements NodeProvider  {

    private static final int NODE_ORDER = 40;
    private static FlyLineReleasesNode NODE_INSTANCE;

    public FlyLineReleasesNodeProvider() {
    }

    public List<NodeGenerator> getNodeGenerators() {
        if (!isShow()) return Collections.emptyList();
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
        });
    }
    
    private static boolean isShow() {
        return FrameworkAccess.getModelProperty(SHOW_FLY_LINE_RELEASES, true);
    }
    
}
