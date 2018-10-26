package org.janelia.horta.blocks;

public class KtxOctreeResolution implements BlockTileResolution {

    private final int octreeLevel; // zero-based level; zero means tip of pyramid

    public KtxOctreeResolution(int octreeLevel) {
        this.octreeLevel = octreeLevel;
    }

    @Override
    public int getResolution() {
        return octreeLevel;
    }
    
    @Override
    public int hashCode() {
        return octreeLevel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KtxOctreeResolution other = (KtxOctreeResolution) obj;
        if (this.octreeLevel != other.octreeLevel) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(BlockTileResolution o) {
        KtxOctreeResolution rhs = (KtxOctreeResolution) o;
        return octreeLevel < rhs.octreeLevel ? -1 : octreeLevel > rhs.octreeLevel ? 1 : 0;
    }
}
