package org.janelia.workstation.tracing;

import org.janelia.workstation.controller.tileimagery.VoxelPosition;

import java.util.List;

public interface AnchoredVoxelPath {
    Long getNeuronID();
    SegmentIndex getSegmentIndex();
    List<VoxelPosition> getPath();
}
