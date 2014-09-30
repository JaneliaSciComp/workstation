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
    
    private int applyZoomFactor(int zoomFactor) {
        return brickCubicDimension / (int)Math.pow(2.0, zoomFactor);
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
