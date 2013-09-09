package org.janelia.it.FlyWorkstation.octree;

// Does NOT extend VoxelIndex, because the types should not be confused
public interface ZoomedVoxelIndex {
    ZoomLevel getZoomLevel();
    int getX();
    int getY();
    int getZ();
}
