package org.janelia.it.FlyWorkstation.tracing;

import java.util.List;

import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;

public class TracedPathSegment {
    private PathTraceRequest request;
    private List<ZoomedVoxelIndex> path;
    private List<Integer> intensities;

    public TracedPathSegment(PathTraceRequest request, List<ZoomedVoxelIndex> path, List<Integer> intensities)
    {
        this.request = request;
        this.path = path;
        this.intensities = intensities;
    }
    
    public PathTraceRequest getRequest() {return request;}
    public List<ZoomedVoxelIndex> getPath() {return path;}
    public List<Integer> getIntensities() {return intensities;}
}
