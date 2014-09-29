package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.BlockTiffOctreeLoadAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.TextureData2dGL;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.media.opengl.GL2;
import org.janelia.it.workstation.gui.large_volume_viewer.Subvolume;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

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
public class ViewTileManagerVolumeSource implements MonitoredVolumeSource {
    private static final int DEFAULT_BRICK_CUBIC_DIMENSION = 512;
    
    private Camera3d camera;
    private CoordinateAxis sliceAxis;   //@deprecated
    private SubvolumeProvider subvolumeProvider;
    private URL dataUrl;
    private byte[] dataVolume;
    
    private int absoluteReqVolStartX;
    private int absoluteReqVolEndX;
    private int absoluteReqVolStartY;
    private int absoluteReqVolEndY;
    private int absoluteReqVolStartZ;
    private int absoluteReqVolEndZ;
    
    private int brickCubicDimension = DEFAULT_BRICK_CUBIC_DIMENSION;

    private VolumeAcceptor volumeAcceptor;
    private TextureDataI textureDataFor3D;
    private IndeterminateNoteProgressMonitor progressMonitor;

    private BlockTiffOctreeLoadAdapter dataAdapter;

    private final Logger logger = LoggerFactory.getLogger( ViewTileManagerVolumeSource.class );

    public ViewTileManagerVolumeSource(Camera3d camera,
                                       CoordinateAxis sliceAxis,
                                       int cubicDimension,
                                       SubvolumeProvider subvolumeProvider,
                                       URL dataUrl) throws Exception {
        
        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation(camera.getRotation());
        
        this.camera = iterationCamera;
        this.sliceAxis = sliceAxis;
        this.subvolumeProvider = subvolumeProvider; 
        if ( cubicDimension > 0 ) {
            brickCubicDimension = cubicDimension;
        }

        dataAdapter = new BlockTiffOctreeLoadAdapter();
        this.dataUrl = dataUrl;
    }

    @Override
    public void getVolume(VolumeAcceptor volumeListener) throws Exception {
        this.volumeAcceptor = volumeListener;
        requestTextureData();
        volumeAcceptor.accept(textureDataFor3D);
    }
    
    @Override
    public String getInfo() {
        return String.format(COORDS_FORMAT,
                camera.getFocus().getX(),
                camera.getFocus().getY(),
                camera.getFocus().getZ()
            );
    }
    
    @Override
    public void setProgressMonitor( IndeterminateNoteProgressMonitor monitor ) {
        this.progressMonitor = monitor;
    }

    /**
     * @return the dataUrl
     */
    public URL getDataUrl() {
        return dataUrl;
    }

    private void requestTextureData() throws Exception {
        StandardizedValues stdVals = new StandardizedValues();
        //fetchTextureData(stdVals, false);
        dataVolume = fetchTextureData(stdVals);

        // Now build the data volume.  The data volume bytes will be filled in later.
        textureDataFor3D = new TextureDataBean(
                new VolumeDataBean( dataVolume, brickCubicDimension, brickCubicDimension, brickCubicDimension ), brickCubicDimension, brickCubicDimension, brickCubicDimension
        );
        textureDataFor3D.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        textureDataFor3D.setChannelCount(stdVals.stdChannelCount);
        textureDataFor3D.setExplicitInternalFormat(stdVals.stdInternalFormat);
        textureDataFor3D.setExplicitVoxelComponentOrder(stdVals.stdFormat);
        textureDataFor3D.setExplicitVoxelComponentType(stdVals.stdType);
        textureDataFor3D.setPixelByteCount(stdVals.stdByteCount);
        textureDataFor3D.setInterpolationMethod( GL2.GL_LINEAR );
        textureDataFor3D.setColorSpace(VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_LINEAR);

    }
    
    private int getEndTileNum(int zoomFactor, int startTileNum, TileFormat tileFormat, int index) {
        return (int)Math.ceil( startTileNum + ( ((applyZoomFactor(zoomFactor)) - 1) / tileFormat.getVoxelMicrometers()[ index ] / tileFormat.getTileSize()[ index ] ) );
    }

    private int applyZoomFactor(int zoomFactor) {
        return brickCubicDimension / (int)Math.pow(2.0, zoomFactor);
    }

    private int getTileNum(int zoomFactor, TileFormat tileFormat, int axis) {
        return (int)Math.floor((camera.getFocus().get(axis) - applyZoomFactor(zoomFactor)) / tileFormat.getVoxelMicrometers()[ axis ] / tileFormat.getTileSize()[ axis ] );
    }

    private void acceptTileData(ByteBuffer pixels, TileIndex tileIndex, int byteCount, int channelCount) {
        progressMonitor.setNote( "Assembling volume..." );
        int stdTileSize = brickCubicDimension * brickCubicDimension;
        TileFormat tileFormat = dataAdapter.getTileFormat();

        // Dealing with tile indices, need to be able to convert tile index back to absolute coordinates,
        // to figure out where the tile is placed within the slice.
        Vec3[] corners = tileFormat.cornersForTileIndex( tileIndex );

        // Adjust for the inversion of Y between tile set and screen.
        FragmentLoadParmBean paramBean = new FragmentLoadParmBean();
        paramBean.yTileAbsStart = (int)corners[ 0 ].y();
        paramBean.yTileAbsEnd = (int)corners[ 3 ].y();

        paramBean.xTileAbsStart = (int)corners[ 0 ].x();
        paramBean.xTileAbsEnd = (int)corners[ 3 ].x();
        paramBean.corners = corners;

        int zTile = (int)corners[ 0 ].z();

        if ( paramBean.yTileAbsStart > paramBean.yTileAbsEnd ) {
            logger.error("Start must be < end. Ignoring this tile [{},{}].", tileIndex.getX(), tileIndex.getY() );
            logger.info("Ignored tile top-left={}.  bottom-right={}." + corners[0], corners[3]);
            return;
        }

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

        paramBean.inVolEndX = calcInVolEnd( absoluteReqVolEndX, absoluteReqVolStartX, paramBean.xTileAbsEnd, brickCubicDimension );
        paramBean.inVolEndY = calcInVolEnd( absoluteReqVolEndY, absoluteReqVolStartY, paramBean.yTileAbsStart, brickCubicDimension );

        paramBean.inTileStartX = calcInTileStart( paramBean.xTileAbsStart, absoluteReqVolStartX );
        paramBean.inTileStartY = calcInTileStart( paramBean.yTileAbsStart, absoluteReqVolStartY );

        logger.info("absolute request X=(" + absoluteReqVolStartX +
                "," + absoluteReqVolEndX +
                "), absolute tile X=(" + paramBean.xTileAbsStart +
                "," + paramBean.xTileAbsEnd +
                "), extent of volume, in X=" + tileFormat.getVolumeSize()[ 0 ] +
                "; in-tile coords X start=" + paramBean.inTileStartX
        );
        logger.info("in-vol-X=(" + paramBean.inVolStartX + "," + paramBean.inVolEndX + ")");

        logger.info("absolute request Y=(" + absoluteReqVolStartY +
                "," + absoluteReqVolEndY + "), absolute tile Y=(" + paramBean.yTileAbsStart +
                "," + paramBean.yTileAbsEnd + "), extent of volume, in Y=" + tileFormat.getVolumeSize()[ 1 ] +
                "; in-tile coords Y start=" + paramBean.inTileStartY
        );
        logger.info("in-vol-Y=(" + paramBean.inVolStartY + "," + paramBean.inVolEndY + ")");

        // Now add to the growing volume.
        int zOutputOffset = (zTile - absoluteReqVolStartZ) * stdTileSize * byteCount * channelCount;
        paramBean.tileWidth = paramBean.xTileAbsEnd - paramBean.xTileAbsStart;
        for ( int yCounter = 0; yCounter < (paramBean.inVolEndY - paramBean.inVolStartY) - 1; yCounter++ ) {

            Runnable sliceLoadRunnable = new LineFragmentLoader(byteCount, channelCount, pixelArr, paramBean, zOutputOffset, yCounter);
            sliceLoadRunnable.run(); // TEMP: avoid clutter in output messages.

        }
    }

    /**
     * Fetching against a sub-volume.
     * 
     * @param stdVals checked for consistency.
     */
    private byte[] fetchTextureData(StandardizedValues stdVals) throws URISyntaxException, IOException {
        progressMonitor.setNote("Fetching texture data...");
        dataAdapter.setTopFolder( new File( dataUrl.toURI() ) ); //        
        TileFormat tileFormat = dataAdapter.getTileFormat();
        //TEMP int zoomFactor = tileFormat.zoomLevelForCameraZoom( camera.getPixelsPerSceneUnit());
        int zoomFactor = 0; // TEMP
        double lowX = getCameraLowerBound(0);
        double lowY = getCameraLowerBound(1);
        double lowZ = getCameraLowerBound(2);
        Vec3 corner1 = new Vec3( lowX, lowY, lowZ );
        Vec3 corner2 = new Vec3( 
                SubvolumeProvider.findUpperBound(lowX, brickCubicDimension),
                SubvolumeProvider.findUpperBound(lowY, brickCubicDimension),
                SubvolumeProvider.findUpperBound(lowZ, brickCubicDimension)
        );

        logger.info( "Fetching centered at {}, at zoom {}.", camera.getFocus(), zoomFactor );
        int[] extent = new int[] {
            brickCubicDimension,
            brickCubicDimension,
            brickCubicDimension
        };
        Subvolume fetchedSubvolume = subvolumeProvider.getSubvolume( camera.getFocus(), extent, zoomFactor, progressMonitor );
//        Subvolume fetchedSubvolume = subvolumeProvider.getSubvolume( corner1, corner2, zoomFactor, progressMonitor );
        stdVals.stdChannelCount = fetchedSubvolume.getChannelCount();
        stdVals.stdInternalFormat = GL2.GL_LUMINANCE16_ALPHA16;
        stdVals.stdType = GL2.GL_UNSIGNED_SHORT;
        stdVals.stdFormat = GL2.GL_LUMINANCE_ALPHA;
        stdVals.stdByteCount = fetchedSubvolume.getBytesPerIntensity();

        final ByteBuffer byteBuffer = fetchedSubvolume.getByteBuffer();
        byteBuffer.rewind();
        byte[] transferredBytes = new byte[ byteBuffer.capacity() ];
        byteBuffer.get(transferredBytes);
        return transferredBytes;
    }

    private double getCameraLowerBound( int index ) {
        double focusVal = camera.getFocus().get( index );
        return SubvolumeProvider.findLowerBound( focusVal, brickCubicDimension );
    }
    
    /**
     * Fetching by acting like the main viewer.
     * @deprecated using new mechanism.
     * @param stdVals checked for consistency across all tiles.
     * @throws IOException throws by called
     * @throws URISyntaxException thrown by called.
     */
    private void fetchTextureData(StandardizedValues stdVals, boolean doNotUse) throws IOException, URISyntaxException {
        dataAdapter.setTopFolder( new File( dataUrl.toURI() ) );
        
        TileFormat tileFormat = dataAdapter.getTileFormat();
        
        // NOTE: need to take zoom level into account for the expected return values.
        int zoomFactor = (int)Math.pow( 2.0, tileFormat.zoomLevelForCameraZoom( camera.getPixelsPerSceneUnit()) );
        int startTileNumX = getTileNum(zoomFactor, tileFormat, 0);
        int startTileNumY = getTileNum(zoomFactor, tileFormat, 1);
        int startTileNumZ = (int)Math.floor((camera.getFocus().getZ()) / tileFormat.getVoxelMicrometers()[ 2 ] / tileFormat.getTileSize()[ 2 ] );
        logger.info("Starting tile coords=(" + startTileNumX + "," + startTileNumY + "," + startTileNumZ + ")");

        // Cover leftward/bottomward/inward by half brick dimension including 0.  Cover rightward/upward/outward
        // half brick dimension beyond start point.
        //  Total coverage full brick dimension.
        //    Dimensions expected to be divisible by 2.
        startTileNumX = Math.max( 0, startTileNumX );
        startTileNumY = Math.max( 0, startTileNumY );
        startTileNumZ = Math.max( 0, startTileNumZ );
        int endTileNumX = getEndTileNum(zoomFactor, startTileNumX, tileFormat, 0);
        int endTileNumY = getEndTileNum(zoomFactor, startTileNumY, tileFormat, 1);
        // Subtracting 1 from ending-tile Z since (N-m)..(N+m) inclusive, covers 2m+1 distinct values.
        int endTileNumZ = (startTileNumZ + brickCubicDimension / (int)tileFormat.getVoxelMicrometers()[ 2 ] / tileFormat.getTileSize()[ 2 ]) - 1;
        logger.info("Ending tile coords=(" + endTileNumX + "," + endTileNumY + "," + endTileNumZ + ")");

        absoluteReqVolStartX = zoomFactor *
                (int) Math.floor((camera.getFocus().getX() - applyZoomFactor(zoomFactor)) / tileFormat.getVoxelMicrometers()[ 0 ]);
        absoluteReqVolStartY = zoomFactor *
                (int) Math.floor((camera.getFocus().getY() - applyZoomFactor(zoomFactor)) / tileFormat.getVoxelMicrometers()[ 1 ]);
        absoluteReqVolStartZ = zoomFactor *
                (int) Math.floor((camera.getFocus().getZ() - applyZoomFactor(zoomFactor)) / tileFormat.getVoxelMicrometers()[ 2 ]);

        if ( absoluteReqVolStartZ < 0 )
            absoluteReqVolStartZ = 0;

        absoluteReqVolEndX = absoluteReqVolStartX + brickCubicDimension - 1;
        absoluteReqVolEndY = absoluteReqVolStartY + brickCubicDimension - 1;
        absoluteReqVolEndZ = absoluteReqVolStartZ + brickCubicDimension - 1;

        logger.info("Absolute start/end X are {}..{}.", absoluteReqVolStartX, absoluteReqVolEndX );
        logger.info("Absolute start/end Y are {}..{}.", absoluteReqVolStartY, absoluteReqVolEndY );
        logger.info("Absolute start/end Z are {}..{}.", absoluteReqVolStartZ, absoluteReqVolEndY );

        TileIndexFinder indexFinder = new TileIndexFinder( tileFormat, camera, sliceAxis );
        Collection<TileIndex> indices = indexFinder.executeForTileCoords(
                startTileNumX, startTileNumY, startTileNumZ,
                endTileNumX, endTileNumY, endTileNumZ // Assume: takes care of maxima on its own.
        );

        // Executing loads through a queue. Await queue rundown, then iterate over the results to standardize values.
        ExecutorService threadPool = Executors.newFixedThreadPool( 5 );
        Map<TileIndex, TextureData2dGL> dataCollection = new HashMap<>();
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
            stdVals.stdInternalFormat = setAndStandardize( stdVals.stdInternalFormat, data.getInternalFormat(), "Internal Format" );
            int typeMultiplier = stdVals.stdInternalFormat == GL2.GL_LUMINANCE16_ALPHA16 ? 2 : 1;
            stdVals.stdByteCount = setAndStandardize( stdVals.stdByteCount, data.getBitDepth()/8 * typeMultiplier, "Byte Count" );
            stdVals.stdChannelCount = setAndStandardize( stdVals.stdChannelCount, data.getChannelCount(), "Channel Count" );
            stdVals.stdType = setAndStandardize( stdVals.stdType, data.getType(), "Type" );

            if ( dataVolume == null ) {
                dataVolume = new byte[ brickCubicDimension * brickCubicDimension * brickCubicDimension * stdVals.stdByteCount * stdVals.stdChannelCount ];
            }
            acceptTileData( data.getPixels(), index, stdVals.stdByteCount, stdVals.stdChannelCount );

        }
    }

    private class StandardizedValues {
        public int stdByteCount;
        public int stdChannelCount;
        public int stdInternalFormat;
        public int stdFormat;
        public int stdType;
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
        private Map<TileIndex, TextureData2dGL> indexToTexData;
        public LoadToRamRunnable( TileIndex index, Map<TileIndex, TextureData2dGL> indexToTexData ) {
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
            int yOutputOffset = (yCounter + paramBean.inVolStartY) * brickCubicDimension * bytesPerVoxel * channelsPerVoxel;
            int yInputOffset = (yCounter + paramBean.inTileStartY) * paramBean.tileWidth * bytesPerVoxel * channelsPerVoxel;

            int sourceStart = yInputOffset + (paramBean.inTileStartX * bytesPerVoxel * channelsPerVoxel);
            int destStart = zOutputOffset + yOutputOffset + (paramBean.inVolStartX * bytesPerVoxel * channelsPerVoxel);
            int copyLength = (paramBean.inVolEndX - paramBean.inVolStartX) * bytesPerVoxel * channelsPerVoxel;

            try {
            System.arraycopy(
                    pixelArr,
                    sourceStart,
                    dataVolume,
                    destStart,
                    copyLength
            );
            } catch ( ArrayIndexOutOfBoundsException aioobe )  {
                logger.error( "Out of bounds: " + sourceStart + ", source size=" + pixelArr.length + ".  Over " + copyLength + ". Dest start={};  Dest size={}.  zOutputOffset=" + zOutputOffset, destStart, dataVolume.length );
                logger.error( "Out of bounds(2): yInputOffset=" + yInputOffset + ", sourceStart=" + sourceStart );
            }
        }
    }
}
