package org.janelia.horta.blocks;

import java.io.IOException;
import java.net.URL;
import org.janelia.geometry3d.ConstVector3;

/**
 *
 * @author brunsc
 */
public interface BlockTileSource<K extends BlockTileKey> {
    BlockTileResolution getMaximumResolution();
    K getBlockKeyAt(ConstVector3 focus, BlockTileResolution resolution);
    ConstVector3 getBlockCentroid(BlockTileKey centerBlock);
    BlockTileData loadBlock(K key) throws IOException, InterruptedException;
    URL getOriginatingSampleURL();
}
