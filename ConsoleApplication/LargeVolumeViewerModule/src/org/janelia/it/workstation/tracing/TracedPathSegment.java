package org.janelia.it.workstation.tracing;

import java.util.List;

public class TracedPathSegment 
implements AnchoredVoxelPath
{
    private PathTraceRequest request;
    private List<VoxelPosition> path;
    private List<Integer> intensities;

    public TracedPathSegment(PathTraceRequest request, List<VoxelPosition> path, List<Integer> intensities)
    {
        this.request = request;
        this.path = path;
        this.intensities = intensities;
    }
    
    public PathTraceRequest getRequest() {return request;}
    public List<VoxelPosition> getPath() {return path;}
    public List<Integer> getIntensities() {return intensities;}
    public SegmentIndex getSegmentIndex() {return request.getSegmentIndex();}
}
