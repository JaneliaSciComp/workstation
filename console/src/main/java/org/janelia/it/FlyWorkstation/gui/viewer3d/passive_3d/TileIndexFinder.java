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
        Vec3 lowerLeftLocation = new Vec3(startVoxX, startVoxY, startVoxZ);
        Vec3 upperRightLocation = new Vec3(endVoxX, endVoxY, endVoxZ);
        TileIndex baseTileIndex = getIndexForLocation(
                format,
                lowerLeftLocation,
                camera3d,
                sliceAxis
        );

        // Next, get Z-stack extents, and use them to accumulate each slice in the stack.
        Vec3 lowerLeftOfIndex = getLowerLeftOfIndex(format, baseTileIndex);
        List<Vec3> sliceCoverage = getSliceCoverage( format, lowerLeftOfIndex, lowerLeftLocation, upperRightLocation );
        int targetDepth = endVoxZ - startVoxZ;
        int remainingZ = targetDepth - ((int)lowerLeftOfIndex.z() - startVoxZ);
        for ( int i = 0; i < remainingZ; i++ ) {
            addSliceTileIndices(rtnVal, sliceCoverage, i);
        }
        return rtnVal;
    }

    private void addSliceTileIndices(List<TileIndex> rtnVal, List<Vec3> sliceCoverage, int i) {
        for ( Vec3 protoVec: sliceCoverage ) {
            // Make a tile around this vector...
            TileIndex nextTileInx = new TileIndex(
                    (int)protoVec.getX(),
                    (int)protoVec.getY(),
                    (int)(i + protoVec.getZ()),
                    (int)camera3d.getPixelsPerSceneUnit(),
                    (int)camera3d.getPixelsPerSceneUnit(),
                    format.getIndexStyle(),
                    sliceAxis
            ); //xyz zoom maxzoom syle coordaxis
            rtnVal.add( nextTileInx );
        }
    }

    private TileIndex getIndexForLocation( TileFormat format, Vec3 location, Camera3d camera3d, CoordinateAxis sliceAxis ) {
        return format.tileIndexForXyz( location, (int)camera3d.getFocus().getZ(), sliceAxis );
    }

    private Vec3 getLowerLeftOfIndex(TileFormat format, TileIndex baseIndex) {
        Vec3[] baseCorners = format.cornersForTileIndex( baseIndex );
        Vec3 bottomLeft = baseCorners[ 0 ];
        return bottomLeft;
    }

    /**
     * This gets a full list of tiles for a single "prototypical" slice.  These values can be tweaked for each
     * slice to complete the full depth.
     *
     * @param format what size of tiles do we mean?
     * @param lowerLeftOfStartingTile starting point, to grow from.
     * @return list of enough tile-coords, to get full coverage of the target area.
     */
    private List<Vec3> getSliceCoverage(
            TileFormat format, Vec3 lowerLeftOfStartingTile, Vec3 lowerLeftTarget, Vec3 upperRightTarget
    ) {
        int targetWidth = (int)(upperRightTarget.x() - lowerLeftTarget.x());
        int targetHeight = (int)(upperRightTarget.y() - lowerLeftTarget.y());

        int remainingX = targetWidth - ((int)lowerLeftOfStartingTile.x() - (int)lowerLeftTarget.x());
        int tileWidth = format.getTileSize()[ 0 ];
        int xTileCount = (int)Math.ceil(remainingX / tileWidth);

        int remainingY = targetHeight - ((int)lowerLeftOfStartingTile.y() - (int)lowerLeftTarget.y());
        int tileHeight = format.getTileSize()[ 1 ];
        int yTileCount = (int)Math.ceil(remainingY / tileHeight);

        // TODO: wonder why it's double?          Wonder if I'm missing a conversion.
        int lowYInx = (int)lowerLeftOfStartingTile.y();
        int lowXInx = (int)lowerLeftOfStartingTile.x();
        int sliceZInx = (int)lowerLeftOfStartingTile.z();

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
