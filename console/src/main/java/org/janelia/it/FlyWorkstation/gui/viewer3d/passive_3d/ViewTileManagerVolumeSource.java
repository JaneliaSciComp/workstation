package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/25/13
 * Time: 2:18 PM
 *
 * Collects information for a "passive 3D cube" of data, beginning with input starting point.  This volume source
 * implements by fetching data out of a view tile manager, focused on the viewport/camera/axis and relative position
 * given.
 */
public class ViewTileManagerVolumeSource implements VolumeSource {
    public static final int BRICK_WIDTH = 512;
    public static final int BRICK_HEIGHT = 512;
    public static final int BRICK_DEPTH = 512;
    private Camera3d camera;
    private Viewport viewport;
    private CoordinateAxis sliceAxis;
    private Rotation3d viewerInGround;
    private URL dataUrl;
    private byte[] dataVolume;

    private int absoluteReqVolStartX;
    private int absoluteReqVolEndX;
    private int absoluteReqVolStartY;
    private int absoluteReqVolEndY;
    private int absoluteReqVolStartZ;
    private int absoluteReqVolEndZ;

    private VolumeAcceptor volumeAcceptor;

    private BlockTiffOctreeLoadAdapter dataAdapter;

    public ViewTileManagerVolumeSource(Camera3d camera,
                                       Viewport viewport,
                                       CoordinateAxis sliceAxis,
                                       Rotation3d viewerInGround,
                                       URL dataUrl) throws Exception {
        this.camera = camera;
        this.viewport = viewport;
        this.viewerInGround = viewerInGround;
        this.sliceAxis = sliceAxis;
        this.dataUrl = dataUrl;

        dataAdapter = new BlockTiffOctreeLoadAdapter();
    }

    @Override
    public void getVolume( VolumeAcceptor volumeListener ) throws Exception {
        this.volumeAcceptor = volumeListener;
        requestTextureData();
    }

    private void requestTextureData() throws Exception {
        dataAdapter.setTopFolder( new File( dataUrl.toURI() ) );

        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation(camera.getRotation());

        TextureDataI textureDataFor3D = null;
        TileFormat tileFormat = dataAdapter.getTileFormat();

        absoluteReqVolStartX = (int) Math.floor((iterationCamera.getFocus().getX() - BRICK_WIDTH/2) / tileFormat.getVoxelMicrometers()[ 0 ]);
        absoluteReqVolStartY = (int) Math.floor((iterationCamera.getFocus().getY() - BRICK_HEIGHT/2) / tileFormat.getVoxelMicrometers()[ 1 ]);
        absoluteReqVolStartZ = (int) Math.floor(iterationCamera.getFocus().getZ() - BRICK_DEPTH/2);
        if ( absoluteReqVolStartZ < 0 )
            absoluteReqVolStartZ = 0;

        absoluteReqVolEndX = absoluteReqVolStartX + BRICK_WIDTH;
        absoluteReqVolEndY = absoluteReqVolStartY + BRICK_HEIGHT;
        absoluteReqVolEndZ = absoluteReqVolStartZ + BRICK_DEPTH;

        // NOTE: camera has micrometer units.  TileIndex requires voxel units.  Hence need a conversion for x,y
        int voxX = (int)((iterationCamera.getFocus().getX() - BRICK_WIDTH/2) / tileFormat.getVoxelMicrometers()[ 0 ] / tileFormat.getTileSize()[ 0 ] );
        int voxY = (int)((iterationCamera.getFocus().getY() - BRICK_HEIGHT/2) / tileFormat.getVoxelMicrometers()[ 1 ] / tileFormat.getTileSize()[ 1 ] );
        int voxZ = (int)((iterationCamera.getFocus().getZ()) / tileFormat.getVoxelMicrometers()[ 2 ] );

        TileIndexFinder indexFinder = new TileIndexFinder( tileFormat, camera, sliceAxis );
        // Cover leftward/bottomward/inward by 256 including 0.  Cover rightward/upward/outward 255 beyond start point.
        //  Total coverage 512.
        //    Dimensions expected to be divisible by 2.
        int volStartTileX = Math.max( 0, voxX );
        int volStartTileY = Math.max( 0, voxY );
        int volStartTileZ = Math.max( 0, voxZ );
        int volEndTileX = (int)Math.ceil( volStartTileX + ( (BRICK_WIDTH / 2 - 1) / tileFormat.getVoxelMicrometers()[ 0 ] / tileFormat.getTileSize()[ 0 ] ) );
        int volEndTileY = (int)Math.ceil( volStartTileY + (BRICK_HEIGHT / 2 - 1) / tileFormat.getVoxelMicrometers()[ 1 ] / tileFormat.getTileSize()[ 1 ] );
        int volEndTileZ = volStartTileZ + BRICK_DEPTH / tileFormat.getTileSize()[ 2 ];
        List<TileIndex> indices = indexFinder.executeForTileCoords(
                volStartTileX, volStartTileY, volStartTileZ,
                volEndTileX, volEndTileY, volEndTileZ // Assume: takes care of maxima on its own.
        );
//                absoluteReqVolStartX, absoluteReqVolStartY, absoluteReqVolStartZ,
//                absoluteReqVolEndX, absoluteReqVolEndY, absoluteReqVolEndZ

        int stdByteCount = 0;
        int stdChannelCount = 0;
        int stdInternalFormat = 0;
        int stdType = 0;

        // Outer loop is the depth loop.
        for ( TileIndex index: indices ) {
            TextureData2dGL data = dataAdapter.loadToRam( index );
            stdByteCount = setAndStandardize( stdByteCount, data.getBitDepth()/8, "Byte Count" );
            stdChannelCount = setAndStandardize( stdChannelCount, data.getChannelCount(), "Channel Count" );
            stdInternalFormat = setAndStandardize( stdInternalFormat, data.getInternalFormat(), "Internal Format" );
            stdType = setAndStandardize( stdType, data.getType(), "Type" );

            if ( dataVolume == null ) {
                dataVolume = new byte[ BRICK_WIDTH * BRICK_HEIGHT * BRICK_DEPTH * stdByteCount ];
            }
            acceptTileData( data.getPixels(), index, stdByteCount );

        }

        // Now build the data volume.  The data volume bytes will be filled in later.
        textureDataFor3D = new TextureDataBean(
                dataVolume, BRICK_WIDTH, BRICK_HEIGHT, BRICK_DEPTH
        );
        textureDataFor3D.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        textureDataFor3D.setChannelCount(stdChannelCount);
        textureDataFor3D.setExplicitInternalFormat(stdInternalFormat);
        textureDataFor3D.setExplicitVoxelComponentType(stdType);
        textureDataFor3D.setPixelByteCount(stdByteCount);

        volumeAcceptor.accept( textureDataFor3D );
    }

    private void acceptTileData(ByteBuffer pixels, TileIndex tileIndex, int byteCount) {
        int stdTileSize = BRICK_WIDTH * BRICK_HEIGHT;
        TileFormat tileFormat = dataAdapter.getTileFormat();

        // Dealing with tile indices, need to be able to convert tile index back to absolute coordinates,
        // to figure out where the tile is placed within the slice.
        Vec3[] corners = tileFormat.cornersForTileIndex( tileIndex );

        // Adjust for the inversion of Y between tile set and screen.
        // Note use of size - end for start, and - start for end.
        int yTileAbsStart = tileFormat.getVolumeSize()[ 1 ] - (int)corners[ 3 ].y();
        int yTileAbsEnd = tileFormat.getVolumeSize()[ 1 ] - (int)corners[ 0 ].y();
        int xTileAbsStart = (int)corners[ 0 ].x();
        int xTileAbsEnd = (int)corners[ 3 ].x();
        int zTile = (int)corners[ 0 ].z();

        if ( yTileAbsEnd < yTileAbsStart ) throw new IllegalStateException("Start must be > end");

        // Check for non-overlap of ranges.  If the ranges do not overlap, assume this is not a relevant tile.
        if ( ( yTileAbsStart > absoluteReqVolEndY  ||  yTileAbsEnd < absoluteReqVolStartY ) || ( xTileAbsStart > absoluteReqVolStartX || xTileAbsEnd < absoluteReqVolStartX ) ) {
            System.out.println("Tile range not relevant to request: " + xTileAbsStart + ":" + xTileAbsEnd + "; " + yTileAbsStart + ":" + yTileAbsEnd);
            return;
        }

        byte[] pixelArr = new byte[ pixels.capacity() ];
        pixels.rewind();
        pixels.get( pixelArr );

        // First, relevant start-x, y, z within tile and within the target volume.
        //  Recall that the tile has 512-voxel lines along X, and 512 of such lines to make Y.
        //  The "in-volume" coords are really relative, rather than being based at 0 for whole world of tiles.
        // "VolStart" and "VolEnd" refer to the 1D array containing the 3D texture we are building.
        int inVolStartX = calcInVolStart( xTileAbsStart, absoluteReqVolStartX );
        int inVolStartY = calcInVolStart( yTileAbsStart, absoluteReqVolStartY );
        System.out.println("VTMVS: absolute Y start of request=" + absoluteReqVolStartY +
                ", absolute Y end of request=" + absoluteReqVolEndY +
                ", absolute Y start of tile=" + yTileAbsStart +
                ", absolute Y end of tile=" + yTileAbsEnd +
                ", extend of volume, in Y=" + tileFormat.getVolumeSize()[ 1 ]
        );

        int inVolEndX = calcInVolEnd( absoluteReqVolEndX, absoluteReqVolStartX, xTileAbsEnd );
        int inVolEndY = calcInVolEnd( absoluteReqVolEndY, absoluteReqVolStartY, yTileAbsStart );

        int inTileStartX = calcInTileStart(absoluteReqVolStartX, xTileAbsStart );
        int inTileStartY = calcInTileStart(absoluteReqVolStartY, tileFormat.getVolumeSize()[1] - yTileAbsStart );

        // Now add to the growing volume.
        int zOutputOffset = zTile * stdTileSize * byteCount;
        int tileWidth = xTileAbsEnd - xTileAbsStart;
        for ( int brickY = 0; brickY < (inVolEndY - inVolStartY); brickY++ ) {

            int yOutputOffset = (brickY + inVolStartY) * BRICK_WIDTH * byteCount;
            int yInputOffset = (brickY + inTileStartY) * tileWidth * byteCount;

            System.arraycopy(
                    pixelArr,
                    yInputOffset + (inTileStartX * byteCount),
                    dataVolume,
                    zOutputOffset + yOutputOffset + (inVolStartX * byteCount),
                    (inVolEndX - inVolStartX) * byteCount
            );

        }
    }

    private int calcInTileStart( int absTileStart, int absVolStart ) {
        return Math.max( 0, absVolStart - absTileStart );
    }

    private int calcInVolEnd( int absVolEnd, int absVolStart, int absTileEnd ) {
        int tileFragment = absTileEnd - absVolStart;
        if ( tileFragment > 0 ) {
            return tileFragment;
        }
        else {
            return absVolEnd - absTileEnd;
        }
    }

    private int calcInVolStart( int absTileStart, int absVolStart ) {
        return Math.max( 0, absTileStart - absVolStart );
    }

    private int setAndStandardize(int standardValue, int latestValue, String message) {
        if ( standardValue == 0 ) {
            standardValue = latestValue;
        }
        else if ( standardValue != latestValue ) {
            throw new IllegalArgumentException( "Mis-matched " + message );
        }
        return standardValue;
    }

}
