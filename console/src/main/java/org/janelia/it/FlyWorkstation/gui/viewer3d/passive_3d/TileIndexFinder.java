package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

import java.util.ArrayList;
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

    public List<TileIndex> execute( int startVoxX, int startVoxY, int startVoxZ, int endVoxX, int endVoxY, int endVoxZ ) {
        List<TileIndex> rtnVal = new ArrayList<TileIndex>();
        Vec3 leastXYZLocation = new Vec3(startVoxX, startVoxY, startVoxZ);
        Vec3 greatestXYZLocation = new Vec3(endVoxX, endVoxY, endVoxZ);
        TileIndex baseTileIndex = getIndexForLocation(
                format,
                leastXYZLocation,
                camera3d,
                sliceAxis
        );

        // Next, get Z-stack extents, and use them to accumulate each slice in the stack.
        Vec3 leastXYZOfIndex = getLeastXYZOfIndex(format, baseTileIndex);
        List<Vec3> sliceCoverage = getSliceCoverage( format, leastXYZOfIndex, leastXYZLocation, greatestXYZLocation );
        int targetDepth = endVoxZ - startVoxZ;
        int remainingZ = targetDepth - ((int)leastXYZOfIndex.z() - startVoxZ);
        for ( int i = 0; i < remainingZ; i++ ) {
            addSliceTileIndices(rtnVal, sliceCoverage, i);
        }
        return rtnVal;
    }

    public List<TileIndex> executeForTileCoords( int startTileX, int startTileY, int startTileZ, int endTileX, int endTileY, int endTileZ ) {

        List<Vec3> tileCoords = new ArrayList<Vec3>();
        for ( int i = startTileX; i <= endTileX; i++ ) {
            for ( int j = startTileY; j <= endTileY; j++ ) {
                for ( int k = startTileZ; k <= endTileZ; k++ ) {
                    tileCoords.add( new Vec3( i, j, k ) );
                }
            }
        }

        List<TileIndex> rtnVal = new ArrayList<TileIndex>();
        addSliceTileIndices( rtnVal, tileCoords, 0 );
        return rtnVal;
//        double xTileMultiplier =  format.getTileSize()[ 0 ] * format.getVoxelMicrometers()[ 0 ];
//        double yTileMultiplier = format.getTileSize()[ 1 ] * format.getVoxelMicrometers()[ 1 ];
//        double zTileMultiplier = format.getTileSize()[ 2 ];
//        Vec3 leastXYZLocation = new Vec3(
//                startTileX * xTileMultiplier,
//                startTileY * yTileMultiplier,
//                startTileZ * zTileMultiplier
//        );
//        Vec3 greatestXYZLocation = new Vec3(
//                endTileX * xTileMultiplier,
//                endTileY * yTileMultiplier,
//                endTileZ * zTileMultiplier
//        );
//
//        // Need to get the base cartesian coords of the tile containing the lowest point requested by user.
//        //  Can do that by getting whichever tile contains those coords, and returning its info.
//        TileIndex baseTileIndex = new TileIndex(
//                startTileX, startTileY, startTileZ, 0, 0, format.getIndexStyle(), sliceAxis
//        );
//
//        // Next, get Z-stack extents, and use them to accumulate each slice in the stack.
//        Vec3 leastXYZOfIndex = getLeastXYZOfIndex(format, baseTileIndex);
//        List<Vec3> sliceCoverage = getSliceCoverage( format, leastXYZOfIndex, leastXYZLocation, greatestXYZLocation );
//        int targetDepth = endTileZ - startTileZ;
//        int remainingZ = targetDepth - ((int)leastXYZOfIndex.z() - startTileZ);
//        for ( int i = 0; i < remainingZ; i++ ) {
//            addSliceTileIndices(rtnVal, sliceCoverage, i);
//        }
//        return rtnVal;
    }

    private void addSliceTileIndices(List<TileIndex> rtnVal, List<Vec3> sliceCoverage, int i) {
        for ( Vec3 protoVec: sliceCoverage ) {
            // Make a tile around this vector...
            TileIndex nextTileInx = new TileIndex(
                    (int)protoVec.getX(),
                    (int)protoVec.getY(),
                    (int)(i + protoVec.getZ()),
                    0, 0, // Zoom and max zoom at 0.
//                    (int)camera3d.getPixelsPerSceneUnit(),
//                    (int)camera3d.getPixelsPerSceneUnit(),
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
