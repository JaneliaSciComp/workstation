package org.janelia.it.FlyWorkstation.tracing;

import java.util.List;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.tracing.PathTraceRequest.SegmentIndex;

public interface AnchoredVoxelPath {
    SegmentIndex getSegmentIndex();
    List<ZoomedVoxelIndex> getPath();
}
