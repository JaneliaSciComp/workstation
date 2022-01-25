package org.janelia.workstation.integration.spi.nodes;

import java.util.List;

/**
 * A service for adding additional top-level nodes to the Data Explorer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface NodeProvider {
    
    public static final String LOOKUP_PATH = "Providers/NodeProvider";
    
    /**
     * Implement this method to provide a list of generators for nodes you want to add.
     */
    List<NodeGenerator> getNodeGenerators();
    
}
