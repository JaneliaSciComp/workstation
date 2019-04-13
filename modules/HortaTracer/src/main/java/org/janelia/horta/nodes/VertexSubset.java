package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.Map;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 *
 * @author Christopher Bruns
 */
public class VertexSubset
{
    private final Collection<NeuronVertex> vertices;
    private final Map<NeuronVertex, Collection<NeuronVertex>> neighborMap;
    private final String name;
    private final int branchCount;
    
    VertexSubset(Collection<NeuronVertex> vertices, 
                    String name, Map<NeuronVertex,
                    Collection<NeuronVertex>> neighborMap,
                    int branchCount) 
    {
        this.vertices = vertices;
        this.name = name;
        this.neighborMap = neighborMap;
        this.branchCount = branchCount;
    }

    public Collection<NeuronVertex> getVertices()
    {
        return vertices;
    }

    public String getName()
    {
        return name;
    }

    public Map<NeuronVertex, Collection<NeuronVertex>> getNeighborMap()
    {
        return neighborMap;
    }

    public int getBranchCount()
    {
        return branchCount;
    }

}
