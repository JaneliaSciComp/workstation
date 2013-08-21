package org.janelia.it.FlyWorkstation.gui.viewer3d.passive_3d;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    public static final int BRICK_DEPTH = 2;     //512
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

    private Logger logger = LoggerFactory.getLogger( ViewTileManagerVolumeSource.class );
    private PrintWriter sliceRecorder; //*** TEMP ***

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
        sliceRecorder = new PrintWriter( new FileWriter( new File("/Users/fosterl/slice_log.txt") ) ); //*** TEMP ***
        dataAdapter.setTopFolder( new File( dataUrl.toURI() ) );

        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation(camera.getRotation());

        TextureDataI textureDataFor3D = null;
        TileFormat tileFormat = dataAdapter.getTileFormat();

        absoluteReqVolStartX = (int) Math.floor((iterationCamera.getFocus().getX() - BRICK_WIDTH/2) / tileFormat.getVoxelMicrometers()[ 0 ]);
        absoluteReqVolEndY = (int) Math.floor((iterationCamera.getFocus().getY() - BRICK_HEIGHT/2) / tileFormat.getVoxelMicrometers()[ 1 ]);
        absoluteReqVolStartZ = (int) Math.floor(iterationCamera.getFocus().getZ() - BRICK_DEPTH/2);
        if ( absoluteReqVolStartZ < 0 )
            absoluteReqVolStartZ = 0;

        absoluteReqVolEndX = absoluteReqVolStartX + BRICK_WIDTH;
        absoluteReqVolStartY = Math.max( 0, absoluteReqVolEndY - BRICK_HEIGHT );
        absoluteReqVolEndZ = absoluteReqVolStartZ + BRICK_DEPTH;

        // NOTE: camera has micrometer units.  TileIndex requires voxel units.  Hence need a conversion for x,y
        int startTileNumX = (int)((iterationCamera.getFocus().getX() - BRICK_WIDTH/2) / tileFormat.getVoxelMicrometers()[ 0 ] / tileFormat.getTileSize()[ 0 ] );
        int startTileNumY = (int)((iterationCamera.getFocus().getY() - BRICK_HEIGHT/2) / tileFormat.getVoxelMicrometers()[ 1 ] / tileFormat.getTileSize()[ 1 ] );
        int startTileNumZ = (int)((iterationCamera.getFocus().getZ()) / tileFormat.getVoxelMicrometers()[ 2 ] );

        // Cover leftward/bottomward/inward by 256 including 0.  Cover rightward/upward/outward 255 beyond start point.
        //  Total coverage 512.
        //    Dimensions expected to be divisible by 2.
        startTileNumX = Math.max( 0, startTileNumX );
        startTileNumY = Math.max( 0, startTileNumY );
        startTileNumZ = Math.max( 0, startTileNumZ );
        int endTileNumX = (int)Math.ceil( startTileNumX + ( (BRICK_WIDTH / 2 - 1) / tileFormat.getVoxelMicrometers()[ 0 ] / tileFormat.getTileSize()[ 0 ] ) );
        int endTileNumY = (int)Math.ceil( startTileNumY + (BRICK_HEIGHT / 2 - 1) / tileFormat.getVoxelMicrometers()[ 1 ] / tileFormat.getTileSize()[ 1 ] );
        int endTileNumZ = (startTileNumZ + BRICK_DEPTH / (int)tileFormat.getVoxelMicrometers()[ 2 ]) - 1;

        TileIndexFinder indexFinder = new TileIndexFinder( tileFormat, camera, sliceAxis );
        Collection<TileIndex> indices = indexFinder.executeForTileCoords(
                startTileNumX, startTileNumY, startTileNumZ,
                endTileNumX, endTileNumY, endTileNumZ // Assume: takes care of maxima on its own.
        );

        int stdByteCount = 0;
        int stdChannelCount = 0;
        int stdInternalFormat = 0;
        int stdType = 0;

        // Executing loads through a queue. Await queue rundown, then iterate over the results to standardize values.
        ExecutorService threadPool = Executors.newFixedThreadPool( 5 );
        Map<TileIndex,TextureData2dGL> dataCollection = new HashMap<TileIndex,TextureData2dGL>();
        for ( TileIndex index: indices ) {
            LoadToRamRunnable runnable = new LoadToRamRunnable( index, dataCollection );
            threadPool.execute( runnable );
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination( 300, TimeUnit.SECONDS );
        } catch ( InterruptedException ie ) {
            ie.printStackTrace();
            SessionMgr.getSessionMgr().handleException( ie );
        }

        for ( TileIndex index: dataCollection.keySet() ) {
            TextureData2dGL data = dataCollection.get( index );
            stdByteCount = setAndStandardize( stdByteCount, data.getBitDepth()/8, "Byte Count" );
            stdChannelCount = setAndStandardize( stdChannelCount, data.getChannelCount(), "Channel Count" );
            stdInternalFormat = setAndStandardize( stdInternalFormat, data.getInternalFormat(), "Internal Format" );
            stdType = setAndStandardize( stdType, data.getType(), "Type" );

            if ( dataVolume == null ) {
                dataVolume = new byte[ BRICK_WIDTH * BRICK_HEIGHT * BRICK_DEPTH * stdByteCount * stdChannelCount ];
            }
            acceptTileData( data.getPixels(), index, stdByteCount, stdChannelCount );

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

        sliceRecorder.close(); //***TEMP***

        volumeAcceptor.accept( textureDataFor3D );
    }

    private void acceptTileData(ByteBuffer pixels, TileIndex tileIndex, int byteCount, int channelCount) {
        int stdTileSize = BRICK_WIDTH * BRICK_HEIGHT;
        TileFormat tileFormat = dataAdapter.getTileFormat();

        // Dealing with tile indices, need to be able to convert tile index back to absolute coordinates,
        // to figure out where the tile is placed within the slice.
        Vec3[] corners = tileFormat.cornersForTileIndex( tileIndex );

        // Adjust for the inversion of Y between tile set and screen.
        FragmentLoadParmBean paramBean = new FragmentLoadParmBean();
        paramBean.yTileAbsStart = (int)corners[ 3 ].y();
        paramBean.yTileAbsEnd = (int)corners[ 0 ].y();

        paramBean.xTileAbsStart = (int)corners[ 0 ].x();
        paramBean.xTileAbsEnd = (int)corners[ 3 ].x();
        paramBean.corners = corners;

        int zTile = (int)corners[ 0 ].z();

        if ( paramBean.yTileAbsStart > paramBean.yTileAbsEnd ) throw new IllegalStateException("Start must be < end");

        // Check for non-overlap of ranges.  If the ranges do not overlap, assume this is not a relevant tile.
        if ( ( paramBean.yTileAbsStart > absoluteReqVolEndY  ||  paramBean.yTileAbsEnd < absoluteReqVolStartY ) ||
             ( paramBean.xTileAbsStart > absoluteReqVolEndX || paramBean.xTileAbsEnd < absoluteReqVolStartX ) ) {
            logger.warn("Tile range not relevant to request: " + paramBean.xTileAbsStart+":"+paramBean.xTileAbsEnd + "; " + paramBean.yTileAbsStart+":"+paramBean.yTileAbsEnd +
                               ", requested was " + absoluteReqVolStartX+":"+absoluteReqVolEndX + "; " + absoluteReqVolStartY+":"+absoluteReqVolEndY );
            return;
        }

        if ( absoluteReqVolStartZ > zTile || absoluteReqVolEndZ < zTile ) {
            logger.warn("Tile Z value {} not relevant to request {}.", zTile, absoluteReqVolEndZ );
            return;
        }

        byte[] pixelArr = new byte[ pixels.capacity() ];
        pixels.rewind();
        pixels.get( pixelArr );

        // First, relevant start-x, y, z within tile and within the target volume.
        //  Recall that the tile has 512-voxel lines along X, and 512 of such lines to make Y.
        //  The "in-volume" coords are really relative, rather than being based at 0 for whole world of tiles.
        // "VolStart" and "VolEnd" refer to the 1D array containing the 3D texture we are building.
        paramBean.inVolStartX = calcInVolStart( paramBean.xTileAbsStart, absoluteReqVolStartX );
        paramBean.inVolStartY = calcInVolStart( paramBean.yTileAbsStart, absoluteReqVolStartY );

        paramBean.inVolEndX = calcInVolEnd( absoluteReqVolEndX, absoluteReqVolStartX, paramBean.xTileAbsEnd, BRICK_WIDTH );
        paramBean.inVolEndY = calcInVolEnd( absoluteReqVolEndY, absoluteReqVolStartY, paramBean.yTileAbsStart, BRICK_HEIGHT );

        paramBean.inTileStartX = calcInTileStart( paramBean.xTileAbsStart, absoluteReqVolStartX );
        paramBean.inTileStartY = calcInTileStart( paramBean.yTileAbsStart, absoluteReqVolStartY );

        logger.debug("absolute request X=(" + absoluteReqVolStartX +
                "," + absoluteReqVolEndX +
                "), absolute tile X=(" + paramBean.xTileAbsStart +
                "," + paramBean.xTileAbsEnd +
                "), extent of volume, in X=" + tileFormat.getVolumeSize()[ 0 ] +
                "; in-tile coords X start=" + paramBean.inTileStartX
        );
        logger.debug("in-vol-X=(" + paramBean.inVolStartX + "," + paramBean.inVolEndX + ")");

        logger.debug("absolute request Y=(" + absoluteReqVolStartY +
                "," + absoluteReqVolEndY + "), absolute tile Y=(" + paramBean.yTileAbsStart +
                "," + paramBean.yTileAbsEnd + "), extent of volume, in Y=" + tileFormat.getVolumeSize()[ 1 ] +
                "; in-tile coords Y start=" + paramBean.inTileStartY
        );
        logger.debug("in-vol-Y=(" + paramBean.inVolStartY + "," + paramBean.inVolEndY + ")");

        // Now add to the growing volume.
        int zOutputOffset = (zTile - absoluteReqVolStartZ) * stdTileSize * byteCount * channelCount;
        paramBean.tileWidth = paramBean.xTileAbsEnd - paramBean.xTileAbsStart;
//        ExecutorService threadPool = Executors.newFixedThreadPool( 5 );
        for ( int yCounter = 0; yCounter < (paramBean.inVolEndY - paramBean.inVolStartY); yCounter++ ) {

            Runnable sliceLoadRunnable = new LineFragmentLoader(byteCount, channelCount, pixelArr, paramBean, zOutputOffset, yCounter);
            sliceLoadRunnable.run(); // TEMP: avoid clutter in output messages.
//            threadPool.execute( sliceLoadRunnable );

        }
//        threadPool.shutdown();
//        try {
//            threadPool.awaitTermination( 120, TimeUnit.SECONDS );
//        } catch ( InterruptedException ie ) {
//            SessionMgr.getSessionMgr().handleException( ie );
//            ie.printStackTrace();
//        }
    }

    /**
     * inVolStartX starting X to _put_ the 1st line fragment
     * inVolStartY starting Y to _put_ the 1st line fragment
     * inVolEndX ending X to _put_ the 1st line fragment
     * inTileStartX starting X to _get_ the line fragment
     * inTileStartY starting Y to _get_ the line fragment
     * tileWidth input tile data X axis length from the input tile.
     */
    private class FragmentLoadParmBean {
        // Volume or "output" related values.
        public int inVolStartX;
        public int inVolStartY;
        public int inVolEndX;
        public int inVolEndY;

        // Tile or "input" related values.
        public Vec3[] corners;

        public int inTileStartX;
        public int inTileStartY;
        public int tileWidth;

        // Whole universe-related offsets for the tile.
        public int xTileAbsStart;
        public int xTileAbsEnd;
        public int yTileAbsStart;
        public int yTileAbsEnd;
    }

    private int calcInTileStart( int absTileStart, int absReqStart ) {
        return Math.max( 0, absReqStart - absTileStart );
    }

    private int calcInVolEnd( int absVolEnd, int absVolStart, int absTileEnd, int maxVolSize ) {
        int tileFragment = absTileEnd - absVolStart;
        int rtnVal;
        if ( tileFragment > 0 ) {
            rtnVal = tileFragment;
        }
        else {
            rtnVal = absVolEnd - absTileEnd;
        }

        return Math.min( rtnVal, maxVolSize );
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

    //----------------------------------------------------------INNER CLASSES
    private class LoadToRamRunnable implements Runnable {
        private TileIndex index;
        private Map<TileIndex,TextureData2dGL> indexToTexData;
        public LoadToRamRunnable( TileIndex index, Map<TileIndex,TextureData2dGL> indexToTexData ) {
            this.index = index;
            this.indexToTexData = indexToTexData;
        }

        public void run() {
            try {
                TextureData2dGL data = dataAdapter.loadToRam( index );
                indexToTexData.put( index, data );
            } catch ( Exception ex ) {
                ex.printStackTrace();

            }
        }
    }

    private class LineFragmentLoader implements Runnable {
        private int bytesPerVoxel;
        private int channelsPerVoxel;
        private byte[] pixelArr;
        private FragmentLoadParmBean paramBean;
        private int zOutputOffset;
        private int yCounter;

        /**
         * Load one line-fragment of data from the pixel array, into the appropriate place in the output "data volume".
         * Inputs data (pixel array) comes from a "tile".
         *
         * @param byteCount tells how many bytes per voxel.
         * @param channelCount tells how many channels contribute to each voxel.
         * @param pixelArr data to push to output.  Note that only a short piece of that will be copied each invokation.
         * @param paramBean convenience container for many values.
         * @param zOutputOffset starting Z to _put_ the line fragment.  This is the sum of all z-planes to this point.
         * @param yCounter a counter offset past the 1st fragment position.
         */
        public LineFragmentLoader(int byteCount, int channelCount, byte[] pixelArr, FragmentLoadParmBean paramBean, int zOutputOffset, int yCounter) {
            this.bytesPerVoxel = byteCount;
            this.channelsPerVoxel = channelCount;
            this.pixelArr = pixelArr;
            this.paramBean = paramBean;
            this.zOutputOffset = zOutputOffset;
            this.yCounter = yCounter;
        }

        public void run() {
            int yOutputOffset = (yCounter + paramBean.inVolStartY) * BRICK_WIDTH * bytesPerVoxel * channelsPerVoxel;
            int yInputOffset = (yCounter + paramBean.inTileStartY) * paramBean.tileWidth * bytesPerVoxel * channelsPerVoxel;

            int sourceStart = yInputOffset + (paramBean.inTileStartX * bytesPerVoxel * channelsPerVoxel);
            int destStart = zOutputOffset + yOutputOffset + (paramBean.inVolStartX * bytesPerVoxel * channelsPerVoxel);
            int copyLength = (paramBean.inVolEndX - paramBean.inVolStartX) * bytesPerVoxel * channelsPerVoxel;

            sliceRecorder.println("X=" + paramBean.inVolStartX + ". Y=" + paramBean.inVolStartY + ". Number of voxels="
                    + (paramBean.inVolEndX - paramBean.inVolStartX) + " at array-copy. y-InputOffset=" + yInputOffset
                    + ".  y-OutputOffset=" + yOutputOffset + ".  copying IN: " + sourceStart + "to OUT:" + destStart
                    + "." + "  Output Z-Offset:" + zOutputOffset
                    + ".  Tile corner 0 is : " + paramBean.corners[ 0 ] + ", corner 3 is : " + paramBean.corners[ 3 ]
            );

            try {
            System.arraycopy(
                    pixelArr,
                    sourceStart,
                    dataVolume,
                    destStart,
                    copyLength
            );
            } catch ( ArrayIndexOutOfBoundsException aioobe )  {
                logger.error( "Out of bounds: " + sourceStart + ", source size=" + pixelArr.length + ".  Over " + copyLength + ". Dest start={};  Dest size={}.", destStart, dataVolume.length );
            }
        }
    }
}
