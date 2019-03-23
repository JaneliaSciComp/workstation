package org.janelia.horta.blocks;

import java.util.List;
import java.util.Objects;
import org.janelia.geometry3d.ConstVector3;

public class KtxOctreeBlockTileKey implements BlockTileKey {

    private final KtxOctreeBlockTileSource tileSource;
    private final List<Integer> octreePath;

    KtxOctreeBlockTileKey(KtxOctreeBlockTileSource tileSource, List<Integer> octreePath) {
        this.tileSource = tileSource;
        this.octreePath = octreePath;
    }

    @Override
    public ConstVector3 getCentroid() {
        return tileSource.getBlockCentroid(this);
    }

    List<Integer> getOctreePath() {
        return octreePath;
    }

    int getKeyDepth() {
        return octreePath.size();
    }

    String getKeyPath() {
        String keyPath = octreeString("/");
        return keyPath.length() == 0 ? keyPath : keyPath + "/";
    }

    String getKeyBlockName(String compressionScheme) {
        return "block" + compressionScheme + octreeString("") + ".ktx";
    }

    private String octreeString(String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (Integer octant : octreePath) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(octant);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return octreeString("/");
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.tileSource);
        hash = 53 * hash + Objects.hashCode(this.octreePath);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KtxOctreeBlockTileKey other = (KtxOctreeBlockTileKey) obj;
        if (!Objects.equals(this.tileSource, other.tileSource)) {
            return false;
        }
        if (!Objects.equals(this.octreePath, other.octreePath)) {
            return false;
        }
        return true;
    }
}
