package org.janelia.horta.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.janelia.geometry3d.ConstVector3;
import org.python.google.common.base.Joiner;

public class KtxOctreeBlockTileKey implements BlockTileKey {
    private final BlockTileSource source;
    private final List<Integer> octreePath;

    KtxOctreeBlockTileKey(List<Integer> octreePath, BlockTileSource source) {
        this.source = source;
        this.octreePath = octreePath;
    }

    List<Integer> getOctreePath() {
        return octreePath;
    }

    int getKeyDepth() {
        return octreePath.size();
    }

    String getKeyPath() {
        return octreePath.stream()
                .map(octant -> octant.toString())
                .reduce((o1, o2) -> o1 + "/" + o2)
                .orElse("");
    }

    String getKeyBlockName(String compressionScheme) {
        String block = octreePath.stream()
                .map(octant -> octant.toString())
                .reduce((o1, o2) -> o1 + "" + o2)
                .orElse("");
        return "block" + compressionScheme + block + ".ktx";
    }

    @Override
    public ConstVector3 getCentroid() {
        return source.getBlockCentroid(this);
    }

    @Override
    public BlockTileSource getSource() {
        return source;
    }

    @Override
    public String toString() {
        List<String> steps = new ArrayList<>();
        for (Integer s : octreePath) {
            steps.add(s.toString());
        }
        String subfolderStr = Joiner.on("/").join(steps);
        return subfolderStr;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.source);
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
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.octreePath, other.octreePath)) {
            return false;
        }
        return true;
    }
}
