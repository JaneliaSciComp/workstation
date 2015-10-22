/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.*;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModelBean;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public abstract class GeometricNeighborhoodBuilder {

    public static final String FILE_SEPARATOR = System.getProperties().getProperty("file.separator");

    protected CompressedFileResolver resolver = new CompressedFileResolver();
    protected CompressedFileResolver.CompressedFileNamer namer;
    protected double[] tileHalfSize;
    protected int[] dimensions;
    protected File topFolder;
    protected TileFormatSource tileFormatSource;
    protected double[] focusPlusZoom = null;
    protected CacheFacadeI cacheManager;
    protected GeometricNeighborhoodListener listener;
    private static Logger log = LoggerFactory.getLogger(GeometricNeighborhoodBuilder.class);

    public static interface TileFormatSource {
        TileFormat getTileFormat();
    }

    protected static class SVITileFormatSource implements TileFormatSource {
        private SharedVolumeImage svi;
        public SVITileFormatSource(SharedVolumeImage svi) {
            this.svi = svi;
        }
        @Override
        public TileFormat getTileFormat() {
            TileFormat tileFormat = svi.getLoadAdapter().getTileFormat();
            return tileFormat;
        }

    }

    public static double[] findTileCenterInMicronsAssumedSize(TileFormat tileFormat, TileIndex tileIndex) {
        return findTileCenterInMicrons(tileFormat, tileIndex, getTileHalfSize(tileFormat));
    }

    /**
     * Convenience method for finding the center micron locations of tiles.
     * Suitable for testing.
     *
     * @param tileFormat applicable to tile
     * @param tileIndex tile of interest
     * @param tileHalfSize "shape" geometry of containing tile collection.
     * @return location of centerpoint.
     */
    public static double[] findTileCenterInMicrons(TileFormat tileFormat, TileIndex tileIndex, double[] tileHalfSize) {
        final CoordinateAxis sliceAxis = tileIndex.getSliceAxis();
        final ZoomLevel zoomLevel = new ZoomLevel(tileIndex.getZoom());
        // @todo get better center approximation in Z plane.
        final int tileZ = tileIndex.getZ();

        final int minTileZ = tileZ - (tileZ % tileFormat.getTileSize()[2]);
        final TileFormat.TileXyz tileXyz = new TileFormat.TileXyz(tileIndex.getX(), tileIndex.getY(), minTileZ);
        final ZoomedVoxelIndex zvi = tileFormat.zoomedVoxelIndexForTileXyz(tileXyz, zoomLevel, sliceAxis);
        log.trace("Found zoomed voxel index of {},{},{}.", zvi.getX(), zvi.getY(), zvi.getZ());
        final TileFormat.VoxelXyz voxXyz = tileFormat.voxelXyzForZoomedVoxelIndex(zvi, sliceAxis);
        log.trace("Found voxXYZ of {},{},{}.", voxXyz.getX(), voxXyz.getY(), voxXyz.getZ());

        final TileFormat.MicrometerXyz mxyz = tileFormat.micrometerXyzForVoxelXyz(voxXyz, sliceAxis);
        //TileFormat.MicrometerXyz mxyz = tileFormat.micrometerXyzForZoomedVoxelIndex(zvi, CoordinateAxis.Z);
        final double[] tileCenter = new double[]{
                mxyz.getX() + tileHalfSize[0], // / tileFormat.getVoxelMicrometers()[0],
                mxyz.getY() + tileHalfSize[1], // / tileFormat.getVoxelMicrometers()[1],
                mxyz.getZ() + tileHalfSize[2], // / tileFormat.getVoxelMicrometers()[2]
        };
        return tileCenter;
    }

    public final void setTileFormatSource(TileFormatSource tfs) {
        this.tileFormatSource = tfs;
    }

    public void setListener(GeometricNeighborhoodListener listener) {
        this.listener = listener;
    }

    public abstract GeometricNeighborhood buildNeighborhood(double[] focus, Double zoom, double pixelsPerSceneUnit);

    /**
     * Build up the positional models to be carried along with paths.
     *
     * @param fileToCenter
     * @param fileToTileXyz
     * @param minTiles SIDE EFFECT: these are changed by this method.
     * @param maxTiles SIDE EFFECT: these are changed by this method.
     * @return map relating paths to models of their positions and their states.
     */
    protected Map<String,PositionalStatusModel> buildPositionalModels(
            Map<String, double[]> fileToCenter, Map<String, int[]> fileToTileXyz,
            int[] minTiles, int[] maxTiles
    ) {
        double[] minCoords = new double[] {
                Double.MAX_VALUE,
                Double.MAX_VALUE,
                Double.MAX_VALUE
        };
        double[] maxCoords = new double[] {
                Double.MIN_VALUE,
                Double.MIN_VALUE,
                Double.MIN_VALUE
        };

        // The Z-tile numbers do not fall in neat sequence.  Instead,
        // we must get the unique list, and map them to ordinal positions.
        Set<Integer> uniqueZTilePos = new TreeSet<>();

        for (String tilePath: fileToCenter.keySet()) {
            double[] tileCenter = fileToCenter.get(tilePath);
            int[] tileXyz = fileToTileXyz.get(tilePath);
            for (int i = 0; i < 3; i++) {
                if (tileCenter[i] < minCoords[i]) {
                    minCoords[i] = tileCenter[i];
                }
                if (tileCenter[i] > maxCoords[i]) {
                    maxCoords[i] = tileCenter[i];
                }
                if (tileXyz[i] < minTiles[i]) {
                    minTiles[i] = tileXyz[i];
                }
                if (tileXyz[i] > maxTiles[i]) {
                    maxTiles[i] = tileXyz[i];
                }
                uniqueZTilePos.add(tileXyz[2]);
            }
        }

        Map<Integer,Integer> zTilePosOrdinal = new HashMap<>();
        int offset = 0;
        for ( Integer tilePos: uniqueZTilePos ) {
            zTilePosOrdinal.put(tilePos, offset++);
        }
        minTiles[2] = 0;
        maxTiles[2] = zTilePosOrdinal.size() - 1;

        double widthScreen = 1.0;
        double heightScreen = 1.0;
        double depthScreen = 1.0;

        double xTrans = widthScreen / (maxCoords[0] - minCoords[0]);
        double yTrans = heightScreen / (maxCoords[1] - minCoords[1]);
        double zTrans = depthScreen / (maxCoords[2] - minCoords[2]);
        int chCount = tileFormatSource.getTileFormat().getChannelCount();
        final String tiffBase = OctreeMetadataSniffer.getTiffBase(CoordinateAxis.Z);
        Map<String,PositionalStatusModel> models = new HashMap<>();
        for (String tilePath: fileToCenter.keySet()) {
            double[] tileCenter = fileToCenter.get(tilePath);
            double screenX = xTrans * (tileCenter[0] - minCoords[0]);
            double screenY = yTrans * (tileCenter[1] - minCoords[1]);
            double screenZ = zTrans * (tileCenter[2] - minCoords[2]);

            log.trace(
                    String.format("Convert %6.2f,%6.2f,%6.2f => %6.2f,%6.2f,%6.2f  for %s",
                            tileCenter[0], tileCenter[1], tileCenter[2],
                            screenX, screenY, screenZ, Utilities.trimToOctreePath(tilePath))
            );
            final int[] tileXyz = fileToTileXyz.get(tilePath);
            log.trace(
                    String.format("At Tile Location: %d,%d,%d", tileXyz[0], tileXyz[1], tileXyz[2])
            );

            for ( int i = 0; i < chCount; i++ ) {
                // from all-red swatch.
                String fullTifPath = tilePath + FILE_SEPARATOR + OctreeMetadataSniffer.getFilenameForChannel(tiffBase, i);
                PositionalStatusModelBean model = new PositionalStatusModelBean(tileCenter);
                model.setChannel(i);
                model.setTileXyz(justifyTileCoords(minTiles, tileXyz, zTilePosOrdinal));
                if (cacheManager.isReady(new File(fullTifPath))) {
                    model.setStatus(PositionalStatusModel.Status.Filled);
                }
                models.put(fullTifPath, model);
            }

        }
        return models;
    }

    /** Constrict tile range to 0-based, and interval=1 */
    protected int[] justifyTileCoords( int[] minTiles, int[] tileXyz, Map<Integer,Integer> zTilePosOrdinal ) {
        int[] rtnVal = new int[minTiles.length];
        for (int i = 0; i < rtnVal.length; i++) {
            rtnVal[i] = tileXyz[i] - minTiles[i];
        }
        rtnVal[2] = zTilePosOrdinal.get( tileXyz[2] );
        return rtnVal;
    }

    protected static double[] getTileHalfSize(TileFormat tileFormat) {
        int[] tileSize = tileFormat.getTileSize();
        log.trace("Tile Size is : {},{},{}.", tileSize[0], tileSize[1], tileSize[2]);
        return new double[] {
                tileSize[0] / 2.0,
                tileSize[1] / 2.0,
                tileSize[2] / 2.0
        };
    }

    protected double[] findTileCenterInMicrons(TileFormat tileFormat, TileIndex tileIndex) {
        return findTileCenterInMicrons(tileFormat, tileIndex, tileHalfSize);
    }

    /**
     * Find the tile set that fills the voxel dimensions of the neighborhood.
     *
     * Borrowed from Sub-volume class.
     */
    protected Set<TileIndex> getCenteredTileSet(TileFormat tileFormat, Vec3 center, double pixelsPerSceneUnit, int[] dimensions, Double zoom) {
        // Ensure dimensions are divisible evenly, by two.
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] % 2 == 1) {
                dimensions[i] ++;
            }
        }

        if (pixelsPerSceneUnit < 0.000001) {
            log.warn("Zero value in pixelsPerSceneUnit.  Resetting to 1.");
            pixelsPerSceneUnit = 1.0;
        }
        else {
            log.trace("PixelsPerSceneUnit={}.", pixelsPerSceneUnit);
        }

        int[] xyzFromWhd = new int[]{0, 1, 2};
        CoordinateAxis sliceAxis = CoordinateAxis.Z;
        ViewBoundingBox voxelBounds = tileFormat.findViewBounds(
                (int)(dimensions[0] * 2.0 * tileFormat.getVoxelMicrometers()[0]),
                (int)(dimensions[1] * 2.0 * tileFormat.getVoxelMicrometers()[1]),
                center, pixelsPerSceneUnit, xyzFromWhd
        );
        TileBoundingBox tileBoundingBox = tileFormat.viewBoundsToTileBounds(xyzFromWhd, voxelBounds, zoom.intValue());

        // Now I have the tile outline.  Can just iterate over that, and for all
        // required depth.
        TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
        int zoomMax = tileFormat.getZoomLevelCount() - 1;

        double depth = dimensions[2];
        BoundingBox3d bb = tileFormat.calcBoundingBox();
        int maxDepth = this.calcZCoord(bb, xyzFromWhd, tileFormat, (int) (center.getZ() + depth));
        int minDepth = this.calcZCoord(bb, xyzFromWhd, tileFormat, (int) (center.getZ() - depth));
        if (minDepth < 0){
            minDepth = 0;
        }

        log.debug("Tile BoundingBox span. Width: " + tileBoundingBox.getwMin() + ":"
                + tileBoundingBox.getwMax() + " Height: " + tileBoundingBox.gethMin()
                + ":" + tileBoundingBox.gethMax() + " Depth:" + minDepth + ":" + maxDepth);
        // NOTE: at dump time, these look like microns, even though the classes
        // holding them say "voxel".
        log.debug("Micron volume in cache extends from\n\t{},{},{}\nto\t\n\t{},{},{}\nin voxels.\nDifference of {},{},{}.",
                voxelBounds.getwFMin(), voxelBounds.gethFMin(), minDepth, voxelBounds.getwFMax(), voxelBounds.gethFMax(), maxDepth,
                voxelBounds.getwFMax()-voxelBounds.getwFMin(), voxelBounds.gethFMax()-voxelBounds.gethFMin(), maxDepth-minDepth
        );

        int minWidth = tileBoundingBox.getwMin();
        int maxWidth = tileBoundingBox.getwMax();
        int minHeight = tileBoundingBox.gethMin();
        int maxHeight = tileBoundingBox.gethMax();
        return createTileIndexesOverRanges(
                minDepth, maxDepth,
                minWidth, maxWidth,
                minHeight, maxHeight,
                xyzFromWhd,
                zoom.intValue(),
                zoomMax,
                indexStyle,
                sliceAxis);
    }

    protected int calcZCoord(BoundingBox3d bb, int[] xyzFromWhd, TileFormat tileFormat, int focusDepth) {
        double zVoxMicrons = tileFormat.getVoxelMicrometers()[xyzFromWhd[2]];
        int dMin = (int) (bb.getMin().get(xyzFromWhd[2]) / zVoxMicrons + 0.5);
        int dMax = (int) (bb.getMax().get(xyzFromWhd[2]) / zVoxMicrons - 0.5);
        int absoluteTileDepth = (int) Math.round(focusDepth / zVoxMicrons - 0.5);
        absoluteTileDepth = Math.max(absoluteTileDepth, dMin);
        absoluteTileDepth = Math.min(absoluteTileDepth, dMax);
        return absoluteTileDepth - tileFormat.getOrigin()[xyzFromWhd[2]];
    }

    protected Set<TileIndex> createTileIndexesOverRanges(
            int minDepth, int maxDepth,
            int minWidth, int maxWidth,
            int minHeight, int maxHeight,
            int[] xyzFromWhd,
            int zoom,
            int zoomMax,
            TileIndex.IndexStyle indexStyle,
            CoordinateAxis sliceAxis) {

        Set<TileIndex> neededTiles = new LinkedHashSet<>();
        for (int d = minDepth; d < maxDepth; d++) {
            for (int w = minWidth; w <= maxWidth; ++w) {
                for (int h = minHeight; h <= maxHeight; ++h) {
                    int whd[] = {w, h, d};
                    TileIndex key = new TileIndex(
                            whd[xyzFromWhd[0]],
                            whd[xyzFromWhd[1]],
                            whd[xyzFromWhd[2]],
                            zoom,
                            zoomMax, indexStyle, sliceAxis);
                    neededTiles.add(key);

                }
            }
        }
        // NEED: to figure out why neighborhood is so huge.
        System.out.println("Needed Tiles number " + neededTiles.size());
        return neededTiles;
    }


}
