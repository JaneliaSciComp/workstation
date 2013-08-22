package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/7/13
 * Time: 2:08 PM
 *
 * Use this to find all tiles to cover a given range in three dimensions.
 */
public class TileIndexFinder {

    private TileFormat format;
    private Camera3d camera3d;
    private CoordinateAxis sliceAxis;

    public TileIndexFinder(TileFormat format, Camera3d camera3d, CoordinateAxis sliceAxis) {
        this.format = format;
        this.camera3d = camera3d;
        this.sliceAxis = sliceAxis;
    }

    public Collection<TileIndex> executeForTileCoords(
            int startVoxX, int startVoxY, int startVoxZ, int endVoxX, int endVoxY, int endVoxZ
    ) {

        Collection<Vec3> tileCoords = new HashSet<Vec3>();
        for ( int i = startVoxX; i <= endVoxX; i++ ) {
            for ( int j = startVoxY; j <= endVoxY; j++ ) {
                for ( int k = startVoxZ; k <= endVoxZ; k++ ) {
                    tileCoords.add( new Vec3( i, j, k ) );
                }
            }
        }

        // Note: using set for collection, so that duplicates may be automatically avoided.  The current
        // "exploring" algorithm for finding slices hits every voxel for its contribution, and this will
        // be prone to duplication, as a tile covers far more than a single voxel.
        Collection<TileIndex> sliceIndexSet = new HashSet<TileIndex>();
        addSliceTileIndices( sliceIndexSet, tileCoords );
        return sliceIndexSet;
    }

    private void addSliceTileIndices(Collection<TileIndex> rtnVal, Collection<Vec3> sliceCoverage) {
        int zoomLevel = format.zoomLevelForCameraZoom(camera3d.getPixelsPerSceneUnit());
        for ( Vec3 protoVec: sliceCoverage ) {
            // Make an index around this vector.
            TileIndex nextTileInx = new TileIndex(
                    (int)protoVec.getX(),
                    (int)protoVec.getY(),
                    (int)protoVec.getZ(),
                    zoomLevel, zoomLevel, // Zoom and max zoom at same value.
                    format.getIndexStyle(),
                    sliceAxis
            ); //xyz zoom maxzoom syle coordaxis
            rtnVal.add( nextTileInx );
        }
    }

    private TileIndex getIndexForLocation( TileFormat format, Vec3 location, Camera3d camera3d, CoordinateAxis sliceAxis ) {
        return format.tileIndexForXyz( location, 1/*(int)camera3d.getFocus().getZ()*/, sliceAxis );
    }

    private Vec3 getLeastXYZOfIndex(TileFormat format, TileIndex baseIndex) {
        Vec3[] baseCorners = format.cornersForTileIndex( baseIndex );
        Vec3 bottomLeft = baseCorners[ 0 ];
        return bottomLeft;
    }

    /**
     * This gets a full list of tiles for a single "prototypical" slice.  These values can be tweaked for each
     * slice to complete the full depth.
     *
     * @param format what size of tiles do we mean?
     * @param leastXYZOfStartingTile starting point, to grow from.
     * @return list of enough tile-coords, to get full coverage of the target area.
     */
    private List<Vec3> getSliceCoverage(
            TileFormat format, Vec3 leastXYZOfStartingTile, Vec3 leastXYZOfTarget, Vec3 greatestXYZOfTarget
    ) {
        int targetWidth = (int)(greatestXYZOfTarget.x() - leastXYZOfTarget.x());
        int targetHeight = (int)(greatestXYZOfTarget.y() - leastXYZOfTarget.y());

        int remainingX = Math.max( 0, targetWidth - ((int)leastXYZOfTarget.x() - (int)leastXYZOfStartingTile.x()) );
        int tileWidth = format.getTileSize()[ 0 ];
        int xTileCount = (int)Math.ceil(remainingX / tileWidth);
        if ( xTileCount == 0 )
            xTileCount = 1;

        int remainingY = Math.max( 0, targetHeight - ((int)leastXYZOfTarget.y() - (int)leastXYZOfStartingTile.y()) );
        int tileHeight = format.getTileSize()[ 1 ];
        int yTileCount = (int)Math.ceil(remainingY / tileHeight);
        if ( yTileCount == 0 )
            yTileCount = 1;

        int lowXInx = (int)leastXYZOfStartingTile.x();
        int lowYInx = (int)leastXYZOfStartingTile.y();
        int sliceZInx = (int)leastXYZOfStartingTile.z();

        List<Vec3> rtnVal = new ArrayList<Vec3>();
        for ( int i = 0; i < xTileCount; i++ ) {
            int xInx = lowXInx + i;
            for ( int j = 0; j < yTileCount; j++ ) {
                Vec3 nextVec = new Vec3( j + lowYInx, xInx, sliceZInx );
                rtnVal.add( nextVec );
            }
        }

        return rtnVal;
    }
}
