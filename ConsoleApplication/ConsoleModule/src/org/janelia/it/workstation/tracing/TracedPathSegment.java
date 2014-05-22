package org.janelia.it.workstation.tracing;

import java.util.List;

import org.janelia.it.workstation.octree.ZoomedVoxelIndex;

public class TracedPathSegment 
implements AnchoredVoxelPath
{
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
    public org.janelia.it.workstation.tracing.SegmentIndex getSegmentIndex() {return request.getSegmentIndex();}
}
