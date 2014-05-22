package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/7/13
 * Time: 2:08 PM
 *
 * Use this to find all tiles to cover a given range in three dimensions.
 */
public class TileIndexFinder {

    private org.janelia.it.workstation.gui.slice_viewer.TileFormat format;
    private org.janelia.it.workstation.gui.camera.Camera3d camera3d;
    private CoordinateAxis sliceAxis;

    public TileIndexFinder(org.janelia.it.workstation.gui.slice_viewer.TileFormat format, org.janelia.it.workstation.gui.camera.Camera3d camera3d, CoordinateAxis sliceAxis) {
        this.format = format;
        this.camera3d = camera3d;
        this.sliceAxis = sliceAxis;
    }

    public Collection<org.janelia.it.workstation.gui.slice_viewer.TileIndex> executeForTileCoords(
            int startTileNumX, int startTileNumY, int startTileNumZ, int endTileNumX, int endTileNumY, int endTileNumZ
    ) {

        Collection<Vec3> tileCoords = new HashSet<Vec3>();
        for ( int i = startTileNumX; i <= endTileNumX; i++ ) {
            for ( int j = startTileNumY; j <= endTileNumY; j++ ) {
                for ( int k = startTileNumZ; k <= endTileNumZ; k++ ) {
                    tileCoords.add( new Vec3( i, j, k ) );
                }
            }
        }

        // Note: using set for collection, so that duplicates may be automatically avoided.  The current
        // "exploring" algorithm for finding slices hits every voxel for its contribution, and this will
        // be prone to duplication, as a tile covers far more than a single voxel.
        Collection<org.janelia.it.workstation.gui.slice_viewer.TileIndex> sliceIndexSet = new HashSet<org.janelia.it.workstation.gui.slice_viewer.TileIndex>();
        addSliceTileIndices( sliceIndexSet, tileCoords );
        return sliceIndexSet;
    }

    private void addSliceTileIndices(Collection<org.janelia.it.workstation.gui.slice_viewer.TileIndex> rtnVal, Collection<Vec3> sliceCoverage) {
        int zoomLevel = format.zoomLevelForCameraZoom(camera3d.getPixelsPerSceneUnit());
        for ( Vec3 protoVec: sliceCoverage ) {
            // Make an index around this vector.
            org.janelia.it.workstation.gui.slice_viewer.TileIndex nextTileInx = new org.janelia.it.workstation.gui.slice_viewer.TileIndex(
                    (int)protoVec.getX(),
                    (int)protoVec.getY(),
                    (int)protoVec.getZ(),
                    zoomLevel, zoomLevel, // Zoom and max zoom at same value.
                    format.getIndexStyle(),
                    sliceAxis
            ); //xyz zoom maxzoom syle coordaxis
            rtnVal.add( nextTileInx );
            Vec3[] corners = format.cornersForTileIndex( nextTileInx );
            System.out.println("Corners found for " + nextTileInx + " are " + corners[ 0 ] + " and " + corners[ 3 ]);
        }
    }

}
