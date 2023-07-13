package org.janelia.horta.blocks;

import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OmeZarrBlockChooser implements BlockChooser<OmeZarrBlockTileKey, OmeZarrBlockTileSource> {
    private static final Logger LOG = LoggerFactory.getLogger(OmeZarrBlockChooser.class);

    private List<Float> zoomLevels = new ArrayList<>();
    private double BLOCK_WIDTH_ACROSS_VIEWPORT = 2.35;
    private int MAX_SIMULTANEOUS_BLOCKS = 14;

    @Override
    public List<OmeZarrBlockTileKey> chooseBlocks(OmeZarrBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus, Vantage vantage) {
        return chooseBlocks(source, focus, previousFocus, vantage, 8);
    }

    public List<OmeZarrBlockTileKey> chooseBlocks(OmeZarrBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus, Vantage vantage, int maxCount) {
        if (zoomLevels.size() == 0) {
            initBlockSizes(source);
        }

        ArrayList<OmeZarrBlockResolution> resolutions = source.getResolutions();

        // Find up to eight closest blocks adjacent to focus
        int zoomIndex = resolutions.size() - 1;

        float screenHeight = vantage.getSceneUnitsPerViewportHeight();

        while (zoomIndex >= 0) {
            if (screenHeight < zoomLevels.get(zoomIndex)) {
                break;
            }

            zoomIndex--;
        }

        if (zoomIndex < 0) {
            zoomIndex = 0;
        }

        OmeZarrBlockResolution resolution = resolutions.get(zoomIndex);

        ConstVector3 blockSize = source.getBlockSize(resolution);

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

        List<OmeZarrBlockTileKey> neighboringBlocks = new ArrayList<>();

        // Enumerate all 27 nearby blocks
        for (float dx : dxa) {
            for (float dy : dya) {
                for (float dz : dza) {
                    ConstVector3 location = focus.plus(new Vector3(dx, dy, dz));
                    OmeZarrBlockTileKey tileKey = source.getBlockKeyAt(location, resolution);
                    if (tileKey == null) {
                        continue;
                    }
                    if (!neighboringBlocks.contains(tileKey)) {
                        neighboringBlocks.add(tileKey);
                    }
                }
            }
        }

        // Sort the blocks strictly by distance to focus
        Collections.sort(neighboringBlocks, new OmeZarrBlockChooser.BlockComparator(focus));

        // Return only the closest 8 blocks
        List<OmeZarrBlockTileKey> result = new ArrayList<>();
        int listLen = Math.min(maxCount, neighboringBlocks.size());
        for (int i = 0; i < listLen; ++i) {
            result.add(neighboringBlocks.get(i));
        }

        return result;
    }

    @Override
    public Map<BlockTileKey, BlockTileData> chooseObsoleteTiles(Map<BlockTileKey, BlockTileData> currentTiles, Map<BlockTileKey, BlockTileData> desiredTiles, BlockTileKey finishedTile) {

        Map<BlockTileKey, BlockTileData> obsoleteTiles = new HashMap<>();
        SortedMap<Float, BlockTileKey> sortedCurrZoomTiles = new TreeMap<>();
        ConstVector3 currFocus = finishedTile.getCentroid();
        int zoomLevel = ((OmeZarrBlockTileKey) finishedTile).getKeyDepth();

        Iterator<BlockTileKey> iter = currentTiles.keySet().iterator();
        boolean blockAtZoomLoaded = false;
        while (iter.hasNext()) {
            OmeZarrBlockTileKey tileKey = (OmeZarrBlockTileKey) iter.next();

            // look at finishedTile and remove tiles at different zoom level that are overlapping finishedTile
            float distance = tileKey.getCentroid().distance(currFocus);
            if (tileKey.getKeyDepth() != zoomLevel) {
                obsoleteTiles.put(tileKey, currentTiles.get(tileKey));
            } else {
                blockAtZoomLoaded = true;
                if (!desiredTiles.containsKey(tileKey))
                    sortedCurrZoomTiles.put(distance, tileKey);
            }
        }
        int i = 0;
        Iterator<Float> tileIter = sortedCurrZoomTiles.keySet().iterator();
        while (tileIter.hasNext()) {
            BlockTileKey tileKey = sortedCurrZoomTiles.get(tileIter.next());
            if (i > MAX_SIMULTANEOUS_BLOCKS) {
                obsoleteTiles.put(tileKey, currentTiles.get(tileKey));
            }
            i++;
        }

        if (!blockAtZoomLoaded) {
            obsoleteTiles.clear();
        }

        return obsoleteTiles;
    }

    private void initBlockSizes(OmeZarrBlockTileSource source) {
        ArrayList<OmeZarrBlockResolution> resolutions = source.getResolutions();

        resolutions.forEach(resolution -> {
            ConstVector3 blockSize = source.getBlockSize(resolution);
            LOG.info("block size [{}, {}, {}] for resolution {}", blockSize.getX(), blockSize.getY(), blockSize.getZ(), resolution);
            float blockHeight = blockSize.getY();
            zoomLevels.add((float) (blockHeight * BLOCK_WIDTH_ACROSS_VIEWPORT));
            ///LOG.info("ZOOM LEVEL {} is {}", resolution.getResolution(), (float) (blockHeight * BLOCK_WIDTH_ACROSS_VIEWPORT));
        });
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
