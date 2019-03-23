/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.tracing;

/**
 * This is a voxel-oriented vector.  It is more a marker class than anything
 * else, to encourage that only voxel values be set.
 * 
 * @author fosterl
 */
public class VoxelPosition {
    private Integer x;
    private Integer y;
    private Integer z;
    
    public VoxelPosition(Integer x, Integer y, Integer z) {
        setVoxelCoords(x, y, z);
    }
    
    public void setVoxelCoords( Integer x, Integer y, Integer z ) {
        if (z==null || y==null || x==null) {
            throw new IllegalArgumentException("No null values");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Integer getX() { return x; }
    public Integer getY() { return y; }
    public Integer getZ() { return z; }
    
    @Override
    public boolean equals(Object o) {
        boolean rtnVal = false;
        if (o instanceof VoxelPosition) {
            VoxelPosition other = (VoxelPosition)o;
            rtnVal = (other.getX().equals(getX()) && other.getY().equals(getY()) && other.getZ().equals(getZ()));
        }
        return rtnVal;
    }
    
    @Override
    public int hashCode() {
        return 31 * getX() + 17 * getY() + getZ();
    }
}
