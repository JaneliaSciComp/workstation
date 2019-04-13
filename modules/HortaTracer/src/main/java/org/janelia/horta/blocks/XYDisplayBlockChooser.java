package org.janelia.horta.blocks;

import java.util.ArrayList;
import java.util.List;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;

/**
 * generate a list of blocks in same xy plane as focus, but wider than usual;
 * this is for testing
 */
public class XYDisplayBlockChooser implements BlockChooser<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> {

    @Override
    public List<KtxOctreeBlockTileKey> chooseBlocks(KtxOctreeBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus) {
        BlockTileResolution resolution = source.getMaximumResolution();

        ConstVector3 blockSize = source.getMaximumResolutionBlockSize();

        // this gives you a (2nx + 1) by (2ny + 1) square
        int nx = 3;
        int ny = 4;

        float[] xarray = new float[2 * nx + 1];
        float[] yarray = new float[2 * ny + 1];

        for (int i = 0; i < 2 * nx + 1; i++) {
            xarray[i] = (i - nx) * blockSize.getX();
        }
        for (int j = 0; j < 2 * ny + 1; j++) {
            yarray[j] = (j - ny) * blockSize.getY();
        }

        List<KtxOctreeBlockTileKey> result = new ArrayList<>();
        for (float dx : xarray) {
            for (float dy : yarray) {
                ConstVector3 location = focus.plus(new Vector3(dx, dy, 0.0f));
                KtxOctreeBlockTileKey tileKey = source.getBlockKeyAt(location, resolution);
                if (tileKey != null) {
                    result.add(tileKey);
                }
            }
        }

        return result;
    }

}
