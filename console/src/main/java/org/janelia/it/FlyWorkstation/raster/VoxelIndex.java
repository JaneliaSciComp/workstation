package org.janelia.it.FlyWorkstation.raster;

public class VoxelIndex implements RasterIndex3d
{
    private int x, y, z;
    
    public VoxelIndex(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }
    
}
