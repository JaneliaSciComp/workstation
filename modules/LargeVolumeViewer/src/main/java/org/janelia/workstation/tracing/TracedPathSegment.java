package org.janelia.workstation.tracing;

import org.janelia.workstation.controller.tileimagery.VoxelPosition;

import java.util.List;

public class TracedPathSegment implements AnchoredVoxelPath {
    
    private PathTraceRequest request;
    private List<VoxelPosition> path;
    private List<Integer> intensities;

    public TracedPathSegment(PathTraceRequest request, List<VoxelPosition> path, List<Integer> intensities) {
        this.request = request;
        this.path = path;
        this.intensities = intensities;
    }

    public PathTraceRequest getRequest() {
        return request;
    }

    @Override
    public List<VoxelPosition> getPath() {
        return path;
    }

    public List<Integer> getIntensities() {
        return intensities;
    }

    @Override
    public SegmentIndex getSegmentIndex() {
        return request.getSegmentIndex();
    }

    @Override
    public Long getNeuronID() {
        return request.getNeuronGuid();
    }
}
