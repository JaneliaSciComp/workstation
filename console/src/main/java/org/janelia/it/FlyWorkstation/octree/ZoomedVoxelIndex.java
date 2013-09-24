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
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        result = prime * result
                + ((zoomLevel == null) ? 0 : zoomLevel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ZoomedVoxelIndex other = (ZoomedVoxelIndex) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (z != other.z)
            return false;
        if (zoomLevel == null) {
            if (other.zoomLevel != null)
                return false;
        } else if (!zoomLevel.equals(other.zoomLevel))
            return false;
        return true;
    }

    public ZoomLevel getZoomLevel() {return zoomLevel;}
    public int getX() {return x;}
    public int getY() {return y;}
    public int getZ() {return z;}
}
