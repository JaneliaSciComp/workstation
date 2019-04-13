package org.janelia.horta.blocks;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author brunsc
 */
public class KtxRamCache {
    private int maxBlockCount = 8;
    private Map<BlockTileKey, BlockTileData> blockMap = new HashMap<>();
    
}
