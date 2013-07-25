package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/25/13
 * Time: 2:18 PM
 *
 * Collects information for a "passive 3D cube" of data, beginning with input starting point.
 */
public class SnapshotCollector {
    private Camera3d camera;
    private Viewport viewport;
    private CoordinateAxis sliceAxis;
    private Rotation3d viewerInGround;

    public SnapshotCollector( Camera3d camera, Viewport viewport,
                              CoordinateAxis sliceAxis, Rotation3d viewerInGround ) {
        this.camera = camera;
        this.viewport = viewport;
        this.viewerInGround = viewerInGround;
        this.sliceAxis = sliceAxis;
    }

    public TextureDataI createVolume( ViewTileManager viewTileManager ) throws Exception {
        byte[] dataVolume = new byte[ 512 * 512 * 512 * 4 ];
        // Strategy: iterate through the tile sets in the z direction, updating depth as we do.
        for ( int i = 0; i < 512; i++ ) {
            TileSet tileset = viewTileManager.createLatestTiles(camera, viewport, sliceAxis, viewerInGround);
            // Now use the tile set to add to the burgeoning volume.
            for ( Tile2d tile: tileset ) {
                TileTexture tileTex = tile.getBestTexture();
                TileIndex tileIndex = tileTex.getIndex();
                PyramidTexture pyramidTexture = tileTex.getTexture();
            }
            camera.incrementFocusPixels( 0.0, 0.0, 1.0 );
        }
        TextureDataBean rtnVal = new TextureDataBean( dataVolume, 512, 512, 512 );
        return rtnVal;
    }

}
