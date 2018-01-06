package org.janelia.it.workstation.browser.gui.colordepth;

import java.util.Arrays;
import java.util.List;

import org.janelia.it.jacs.integration.framework.nodes.NodeGenerator;
import org.janelia.it.jacs.integration.framework.nodes.NodeProvider;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * Adds the color depth nodes to the Data Explorer at indexes 50 and 51.
 */
@ServiceProvider(service = NodeProvider.class, path=NodeProvider.LOOKUP_PATH)
public class ColorDepthNodeProvider implements NodeProvider  {
    
    private static final int NODE_ORDER = 50;
    
    public ColorDepthNodeProvider() {
    }

    public List<NodeGenerator> getNodeGenerators() {
        return Arrays.asList(
                new NodeGenerator() {
                    
                    @Override
                    public Integer getIndex() {
                        return NODE_ORDER;
                    }
                    
                    @Override
                    public Node createNode() {
                        return new ColorDepthMasksNode();
                    }
                },
                new NodeGenerator() {
                    
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
}
