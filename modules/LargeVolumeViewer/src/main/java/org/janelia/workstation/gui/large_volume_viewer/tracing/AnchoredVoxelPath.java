package org.janelia.workstation.gui.large_volume_viewer.tracing;

import org.janelia.workstation.controller.tileimagery.VoxelPosition;

import java.util.List;

public interface AnchoredVoxelPath {
    Long getNeuronID();
    SegmentIndex getSegmentIndex();
    List<VoxelPosition> getPath();
}
