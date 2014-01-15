package org.janelia.it.FlyWorkstation.tracing;

import java.util.List;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;

public interface AnchoredVoxelPath {
    SegmentIndex getSegmentIndex();
    List<ZoomedVoxelIndex> getPath();
}
