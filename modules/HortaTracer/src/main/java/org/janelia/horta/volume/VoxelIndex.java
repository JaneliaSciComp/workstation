package org.janelia.horta.volume;

/**
 *
 * @author Christopher Bruns
 */
public class VoxelIndex {
    private int[] data;
    
    public VoxelIndex(int x, int y, int z) {
        data = new int[] {x, y, z};
    }
    
    public VoxelIndex(int[] xyz) {
        data = xyz;
    }
}
