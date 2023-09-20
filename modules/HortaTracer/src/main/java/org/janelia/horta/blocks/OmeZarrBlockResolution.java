package org.janelia.horta.blocks;

public class OmeZarrBlockResolution implements BlockTileResolution {
    private final int depth;

    private final int[] chunkSizeXYZ;

    private final double resolutionMicrometers;

    private final double[] voxelSize;

    private final OmeZarrBlockInfoSet blockInfoSet;

    public OmeZarrBlockResolution(int depth, int[] chunkSizeXYZ, double[] voxelSize, double resolutionMicrometers) {
        this.depth = depth;
        this.chunkSizeXYZ = chunkSizeXYZ;
        this.resolutionMicrometers = resolutionMicrometers;
        this.voxelSize = voxelSize;

        blockInfoSet = new OmeZarrBlockInfoSet(voxelSize, chunkSizeXYZ);
    }

    @Override
    public int getResolution() {
        return depth;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public int hashCode() {
        return depth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OmeZarrBlockResolution other = (OmeZarrBlockResolution) obj;
        if (this.depth != other.depth) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(BlockTileResolution o) {
        OmeZarrBlockResolution rhs = (OmeZarrBlockResolution) o;
        return Integer.compare(depth, rhs.depth);
    }

    @Override
    public String toString() {
        return String.format("depth: %d; um/voxel: %.1f", depth, resolutionMicrometers);
    }

    public double getResolutionMicrometers() {
        return resolutionMicrometers;
    }

    public OmeZarrBlockInfoSet getBlockInfoSet() {
        return blockInfoSet;
    }

    public int[] getChunkSize() {
        return chunkSizeXYZ;
    }
}
