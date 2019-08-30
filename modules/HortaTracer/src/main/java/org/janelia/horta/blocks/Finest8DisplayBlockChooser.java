package org.janelia.horta.blocks;

import java.util.*;

import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate sorted list of up to eight max resolution blocks near current focus
 *
 * @author brunsc
 */
public class Finest8DisplayBlockChooser implements BlockChooser<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> {

    private static final Logger LOG = LoggerFactory.getLogger(Finest8DisplayBlockChooser.class);
    private List<Float> zoomLevels = new ArrayList<>();
    private int blocksAcrossViewport = 3;

    private void initBlockSizes(KtxOctreeBlockTileSource source, Vantage vantage) {
        int numLevels = (int)source.getZoomLevels();
        for (int i=2; i<numLevels; i++) {
            float blockHeight = source.getBlockSize(new KtxOctreeResolution(i)).getY();
            LOG.info("blockHeight is {} ",  blockHeight);

            zoomLevels.add(blockHeight * blocksAcrossViewport);
            LOG.info("Zoom level {} is {} ", i, blockHeight * 2);
        }
    }
    /*
     Choose the eight closest maximum resolution blocks to the current focus point.
     */
    @Override
    public List<KtxOctreeBlockTileKey> chooseBlocks(KtxOctreeBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus,
                                                    Vantage vantage) {
        // Find up to eight closest blocks adjacent to focus
        int zoomIndex = zoomLevels.size()-1;
        int zoomLevel = 2;  // default to coarsest block
        if (zoomIndex<=0) {
            initBlockSizes(source, vantage);
        }
        boolean foundZoom = false;
        float screenHeight = vantage.getSceneUnitsPerViewportHeight();

        LOG.info("SCREEN HEIGHT IS {}",screenHeight);
        while (!foundZoom && zoomIndex>1) {

            if (screenHeight<zoomLevels.get(zoomIndex)) {
                zoomLevel = zoomIndex+2;
                foundZoom = true;
            }

            LOG.info("zoom Comparison is {} less than {}",screenHeight,zoomLevels.get(zoomIndex));
            zoomIndex--;
        }

        LOG.info("ZOOM LEVEL  IS {}",zoomLevel);
        BlockTileResolution blockResolution = new KtxOctreeResolution(zoomLevel);
        //         ConstVector3 blockSize = source.getMaximumResolutionBlockSize();
        ConstVector3 blockSize = source.getBlockSize(new KtxOctreeResolution(zoomLevel));
        float dxa[] = new float[]{
            0f,
            -blockSize.getX(),
            +blockSize.getX()};
        float dya[] = new float[]{
            0f,
            -blockSize.getY(),
            +blockSize.getY()};
        float dza[] = new float[]{
            0f,
            -blockSize.getZ(),
            +blockSize.getZ()};

        List<KtxOctreeBlockTileKey> neighboringBlocks = new ArrayList<>();

        // Enumerate all 27 nearby blocks
        for (float dx : dxa) {
            for (float dy : dya) {
                for (float dz : dza) {
                    ConstVector3 location = focus.plus(new Vector3(dx, dy, dz));
                    KtxOctreeBlockTileKey tileKey = source.getBlockKeyAt(location, blockResolution);
                    if (tileKey == null) {
                        continue;
                    }
                    neighboringBlocks.add(tileKey);
                }
            }
        }
        LOG.info("number of possible tiles  IS {}",neighboringBlocks.size());

        // Sort the blocks strictly by distance to focus
        Collections.sort(neighboringBlocks, new BlockComparator(focus));

        // Return only the closest 8 blocks
        List<KtxOctreeBlockTileKey> result = new ArrayList<>();
        int listLen = Math.min(8, neighboringBlocks.size());
        for (int i = 0; i < listLen; ++i) {
            result.add(neighboringBlocks.get(i));
        }

        return result;
    }

    @Override
    public Map chooseObsoleteTiles(Map<BlockTileKey, BlockTileData> currentTiles, Map<BlockTileKey, BlockTileData> desiredTiles, BlockTileKey finishedTile) {
        // Remove obsolete tiles from loading and loaded
        Map<BlockTileKey, BlockTileData> obsoleteTiles = new HashMap<>();
        Iterator<BlockTileKey> iter = currentTiles.keySet().iterator();
        while (iter.hasNext()) {
            BlockTileKey key = iter.next();
            if (!desiredTiles.containsKey(key)) {
                obsoleteTiles.put(key, currentTiles.get(key));
            }
        }
        return obsoleteTiles;
    }

    // Sort blocks by distance from focus to block centroid
    private static class BlockComparator<K extends BlockTileKey> implements Comparator<K> {
    
        private final ConstVector3 focus;

        BlockComparator(ConstVector3 focus) {
            this.focus = focus;
        }

        @Override
        public int compare(K block1, K block2) {
            ConstVector3 c1 = block1.getCentroid().minus(focus);
            ConstVector3 c2 = block2.getCentroid().minus(focus);
            float d1 = c1.dot(c1); // distance squared
            float d2 = c2.dot(c2);
            return d1 < d2 ? -1 : d1 > d2 ? 1 : 0;
        }
    }

}
