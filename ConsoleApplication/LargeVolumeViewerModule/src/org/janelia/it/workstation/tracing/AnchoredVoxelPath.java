package org.janelia.it.workstation.tracing;

import java.util.List;

public interface AnchoredVoxelPath {
    SegmentIndex getSegmentIndex();
    List<VoxelPosition> getPath();
}
