package org.janelia.it.FlyWorkstation.octree;

// Does NOT extend VoxelIndex, because the types should not be confused
public class ZoomedVoxelIndex 
{
    private int x, y, z;
    private ZoomLevel zoomLevel;
    
    public ZoomedVoxelIndex(ZoomLevel zoomLevel, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.zoomLevel = zoomLevel;
    }
    
    public ZoomLevel getZoomLevel() {return zoomLevel;}
    public int getX() {return x;}
    public int getY() {return y;}
    public int getZ() {return z;}
}
