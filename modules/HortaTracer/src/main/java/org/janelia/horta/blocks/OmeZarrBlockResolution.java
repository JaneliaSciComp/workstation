package org.janelia.horta.blocks;

import org.aind.omezarr.OmeZarrDataset;

public class OmeZarrBlockResolution implements BlockTileResolution {
    private final int depth;

    private final int[] chunkSizeXYZ;

    private final double resolutionMicrometers;

    private final double[] voxelSize;

    private final OmeZarrDataset dataset;

    public OmeZarrBlockResolution(OmeZarrDataset dataset, int depth, int[] chunkSizeXYZ, double[] voxelSize, double resolutionMicrometers) {
        this.dataset = dataset;
        this.depth = depth;
        this.chunkSizeXYZ = chunkSizeXYZ;
        this.resolutionMicrometers = resolutionMicrometers;
        this.voxelSize = voxelSize;
    }

    @Override
    public int getResolution() {
        return depth;
    }

    public int getDepth() {
        return depth;
    }

    public OmeZarrDataset getDataset() {
        return dataset;
    }

    public double[] getVoxelSize() {
        return voxelSize;
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

        return this.depth == other.depth;
    }

    @Override
    public int compareTo(BlockTileResolution o) {
        OmeZarrBlockResolution rhs = (OmeZarrBlockResolution) o;
        return Integer.compare(depth, rhs.depth);
    }

    @Override
    public String toString() {
        return String.format("ome-zarr resolution %d (um/voxel: %.1f)", depth, resolutionMicrometers);
    }

    public double getResolutionMicrometers() {
        return resolutionMicrometers;
    }

    public int[] getChunkSize() {
        return chunkSizeXYZ;
    }
}
