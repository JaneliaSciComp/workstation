package org.janelia.it.workstation.tracing;

import java.util.List;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;

public interface AnchoredVoxelPath {
    SegmentIndex getSegmentIndex();
    List<ZoomedVoxelIndex> getPath();
}
