package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Camera3d camera;
    private Viewport viewport;
    private CoordinateAxis sliceAxis;
    private Rotation3d viewerInGround;
    private ViewTileManager viewTileManager;

    private VolumeAcceptor volumeListener;
    private TileServer tileServer;
    private TileSet tileset;

    private AtomicInteger tileUpdateCounter = new AtomicInteger( 0 );

    public ViewTileManagerVolumeSource(Camera3d camera,
                                       Viewport viewport,
                                       CoordinateAxis sliceAxis,
                                       Rotation3d viewerInGround,
                                       TileServer tileServer) {
        this.camera = camera;
        this.viewport = viewport;
        this.viewerInGround = viewerInGround;
        this.sliceAxis = sliceAxis;
        this.tileServer = tileServer;

        tileset = new TileSet();
        this.viewTileManager = new ViewTileManager(new TileConsumer() {
            @Override
            public Camera3d getCamera() {
                return ViewTileManagerVolumeSource.this.camera;
            }

            @Override
            public Viewport getViewport() {
                return ViewTileManagerVolumeSource.this.viewport;
            }

            @Override
            public CoordinateAxis getSliceAxis() {
                return ViewTileManagerVolumeSource.this.sliceAxis;
            }

            @Override
            public Rotation3d getViewerInGround() {
                return ViewTileManagerVolumeSource.this.viewerInGround;
            }

            @Override
            public boolean isShowing() {
                return true;
            }

            @Override
            public Slot getRepaintSlot() {
                Slot repaintSlot = new Slot() {
                    @Override
                    public void execute() {
                        // Check if all are available.
                        if ( tileUpdateCounter.intValue() == 0 ) {
                            TextureDataI textureData = getTextureDataBean( tileset );
                            volumeListener.accept( textureData );
                        }
                    }
                };
                return repaintSlot;
            }
        });
        this.viewTileManager.setVolumeImage( new SharedVolumeImage() );

        viewTileManager.onTextureLoadedSlot = new VSTextureLoadSlot( viewTileManager.onTextureLoadedSlot );

    }

    private class VSTextureLoadSlot extends Slot1<TileIndex> {
        private Slot1<TileIndex> wrappedSlot;
        public VSTextureLoadSlot( Slot1<TileIndex> wrappedSlot ) {
            this.wrappedSlot = wrappedSlot;
        }

        @Override
        public void execute(TileIndex tileIndex) {
            tileUpdateCounter.decrementAndGet();
            wrappedSlot.execute(tileIndex);
        }
    }

    @Override
    public void getVolume( VolumeAcceptor volumeListener ) throws Exception {
        this.volumeListener = volumeListener;

        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation( camera.getRotation() );
        tileServer.addViewTileManager( viewTileManager );

        // Strategy: iterate through the tile sets in the z direction, updating depth as we do.
        // Outer loop is the depth loop.
        tileUpdateCounter.set( 512 );
        for ( int i = 0; i < 512; i++ ) {
            // Drilling in/through all the tiles to get a collection.
            tileset.addAll(viewTileManager.createLatestTiles(iterationCamera, viewport, sliceAxis, viewerInGround));
            iterationCamera.incrementFocusPixels( 0.0, 0.0, 1.0 );
        }

        // Now all the tiles are added, and the vtm knows what to do, launch that part.
        tileServer.refreshCurrentTileSetSlot.execute();

    }

    private TextureDataI getTextureDataBean( TileSet tileset ) {
        byte[] dataVolume = new byte[ 512 * 512 * 512 * 4 ];

        int stdByteCount = 0;
        int stdChannelCount = 0;
        int stdInternalFormat = 0;
        int stdType = 0;
        int stdTileSize = 512 * 512;
        int dataVolumeOffset = 0;
        TextureDataI textureDataFor3D = null;

        // Outer loop is the depth loop.
        for ( int i = 0; i < 512; i++ ) {
            // Now use the tile set to add to the burgeoning volume.
            for ( Tile2d tile: tileset ) {
                TileTexture tileTex = tile.getBestTexture();
                TileIndex tileIndex = tileTex.getIndex();
                // Strenuously avoid pyramid textures.  Get something ELSE from the tile texture.
                TextureData2dGL textureData = tileTex.getTextureData();
                ByteBuffer pixels = textureData.getPixels();

                // Now: must interpret the pixels data.
                int byteCount = textureData.getBitDepth() / 8;
                stdByteCount = setAndStandardize( stdByteCount, byteCount, "byte count." );
                int channelCount = textureData.getChannelCount();
                stdChannelCount = setAndStandardize( stdChannelCount, channelCount, "channel count." );
                int internalFormat = textureData.getInternalFormat();
                stdInternalFormat = setAndStandardize( stdInternalFormat, internalFormat, "internal format." );
                int type = textureData.getType();
                stdType = setAndStandardize( stdType, type, "type." );

                dataVolumeOffset = copyPixelData(dataVolume, stdTileSize, dataVolumeOffset, textureData, pixels);
            }

            textureDataFor3D = new TextureDataBean(
                    dataVolume, 512, 512, 512
            );
            textureDataFor3D.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
            textureDataFor3D.setChannelCount(stdChannelCount);
            textureDataFor3D.setExplicitInternalFormat(stdInternalFormat);
            textureDataFor3D.setExplicitVoxelComponentType(stdType);
            textureDataFor3D.setPixelByteCount(stdByteCount);
        }
        return textureDataFor3D;
    }

    private int copyPixelData(
            byte[] dataVolume, int stdTileSize, int dataVolumeOffset, TextureData2dGL textureData, ByteBuffer pixels
    ) {
        int pixelCap = pixels.capacity();
        int yDim = textureData.getHeight();
        int xDim = textureData.getWidth();
        if ( pixelCap == stdTileSize  &&  xDim == 512  &&  yDim == 512 ) {
            // Must find center of tile size, and grab a sub-part 256 pixels either direction of axes.

            if ( yDim == 512 && xDim == 512 ) {
                // Write directly to the data volume, at latest "seek" point. This image is the latest
                // plane in Z.
                System.arraycopy( pixels.array(), 0, dataVolume, dataVolumeOffset, stdTileSize );
                dataVolumeOffset += stdTileSize;
            }
        }
        else {

        }
        return dataVolumeOffset;
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

    static class VSViewTileManager extends ViewTileManager {
        public VSViewTileManager( TileConsumer tileConsumer ) {
            super( tileConsumer );
        }

    }

}
