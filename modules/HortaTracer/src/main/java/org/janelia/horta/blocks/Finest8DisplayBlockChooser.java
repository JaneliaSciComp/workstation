package org.janelia.horta.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    /*
     Choose the eight closest maximum resolution blocks to the current focus point.
     */
    @Override
    public List<KtxOctreeBlockTileKey> chooseBlocks(KtxOctreeBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus,
                                                    Vantage vantage) {
        // Find up to eight closest blocks adjacent to focus
        int zoomLevel = 0;
        if (vantage.getSceneUnitsPerViewportHeight()<200) {
            zoomLevel = 6;
        } else if (vantage.getSceneUnitsPerViewportHeight()<400) {
            zoomLevel = 5;
        } else if (vantage.getSceneUnitsPerViewportHeight()<1000) {
            zoomLevel = 4;
        } else if (vantage.getSceneUnitsPerViewportHeight()<2000) {
            zoomLevel = 3;
        } else if (vantage.getSceneUnitsPerViewportHeight()<5000) {
            zoomLevel = 2;
        }

        LOG.info("ZOOM LEVEL  IS {}",zoomLevel);
        BlockTileResolution blockResoluion = new KtxOctreeResolution(zoomLevel);
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
                    KtxOctreeBlockTileKey tileKey = source.getBlockKeyAt(location, blockResoluion);
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
