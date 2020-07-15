package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.Map;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 *
 * @author Christopher Bruns
 */
public class VertexSubset
{
    private final Collection<TmGeoAnnotation> vertices;
    private final Map<TmGeoAnnotation, Collection<TmGeoAnnotation>> neighborMap;
    private final String name;
    private final int branchCount;
    
    VertexSubset(Collection<TmGeoAnnotation> vertices,
                    String name, Map<TmGeoAnnotation,
                    Collection<TmGeoAnnotation>> neighborMap,
                    int branchCount) 
    {
        this.vertices = vertices;
        this.name = name;
        this.neighborMap = neighborMap;
        this.branchCount = branchCount;
    }

    public Collection<TmGeoAnnotation> getVertices()
    {
        return vertices;
    }

    public String getName()
    {
        return name;
    }

    public Map<TmGeoAnnotation, Collection<TmGeoAnnotation>> getNeighborMap()
    {
        return neighborMap;
    }

    public int getBranchCount()
    {
        return branchCount;
    }

}
