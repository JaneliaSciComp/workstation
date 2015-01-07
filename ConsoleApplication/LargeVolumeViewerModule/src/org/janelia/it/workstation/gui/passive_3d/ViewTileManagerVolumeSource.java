package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import javax.media.opengl.GL2;
import org.janelia.it.workstation.gui.large_volume_viewer.Subvolume;
import org.janelia.it.workstation.gui.large_volume_viewer.SubvolumeProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
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
    private BoundingBox3d bb;
    private SubvolumeProvider subvolumeProvider;
    private URL dataUrl;
    private byte[] dataVolume;
    private double[] voxelMicrometers;
    
    private int[] brickDimensions = new int[] { DEFAULT_BRICK_CUBIC_DIMENSION, DEFAULT_BRICK_CUBIC_DIMENSION, DEFAULT_BRICK_CUBIC_DIMENSION,  };

    private VolumeAcceptor volumeAcceptor;
    private TextureDataI textureDataFor3D;
    private IndeterminateNoteProgressMonitor progressMonitor;

    private final Logger logger = LoggerFactory.getLogger( ViewTileManagerVolumeSource.class );

    public ViewTileManagerVolumeSource(Camera3d camera,
                                       BoundingBox3d bb,
                                       int[] dimensions,
                                       SubvolumeProvider subvolumeProvider,
                                       double[] voxelMicrometers,
                                       URL dataUrl) throws Exception {

        init(camera, bb, subvolumeProvider, dimensions, voxelMicrometers, dataUrl);
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

    private void init(
            Camera3d camera, 
            BoundingBox3d bb, 
            SubvolumeProvider subvolumeProvider, 
            int[] dimensions, 
            double[] voxelMicrometers,
            URL dataUrl) {
        
        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation(camera.getRotation());
        
        this.camera = iterationCamera;
        this.bb = bb;
        this.subvolumeProvider = subvolumeProvider;
        this.voxelMicrometers = voxelMicrometers;
        this.brickDimensions = dimensions;
        
        this.dataUrl = dataUrl;
    }
        
    private void requestTextureData() throws Exception {
        StandardizedValues stdVals = new StandardizedValues();
        //fetchTextureData(stdVals, false);
        dataVolume = fetchTextureData(stdVals, bb);

        // Now build the data volume.  The data volume bytes will be filled in later.
        textureDataFor3D = new TextureDataBean(
                new VolumeDataBean(
                        dataVolume, brickDimensions[0], brickDimensions[1], brickDimensions[2] 
                ), 
                brickDimensions[0], brickDimensions[1], brickDimensions[2]
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
    
    /**
     * Fetching against a sub-volume.
     * 
     * @param stdVals checked for consistency.
     */
    private byte[] fetchTextureData(StandardizedValues stdVals, BoundingBox3d bb) throws URISyntaxException, IOException, DataSourceInitializeException {
        progressMonitor.setNote("Fetching texture data...");
        int zoomFactor = 0; // TEMP

        logger.info( "Fetching centered at {}, at zoom {}.", camera.getFocus(), zoomFactor );
        double minVoxelMicron = Double.MAX_VALUE;
        for ( double voxelMicrometer: voxelMicrometers ) {
            if ( voxelMicrometer < minVoxelMicron ) {
                minVoxelMicron = voxelMicrometer;
            }
        }
        Subvolume fetchedSubvolume = subvolumeProvider.getSubvolumeFor3D( camera.getFocus(), 1.0 / minVoxelMicron, bb, brickDimensions, zoomFactor, progressMonitor );
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

    private class StandardizedValues {
        public int stdByteCount;
        public int stdChannelCount;
        public int stdInternalFormat;
        public int stdFormat;
        public int stdType;
    }
    
}
