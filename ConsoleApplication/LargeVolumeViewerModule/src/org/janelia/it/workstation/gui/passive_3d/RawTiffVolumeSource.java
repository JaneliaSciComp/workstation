/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.util.Map;
import javax.media.opengl.GL2;

import org.janelia.it.jacs.shared.img_3d_loader.AbstractVolumeFileLoader;
import org.janelia.it.jacs.shared.img_3d_loader.ByteArrayLoader;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.loader.TifTextureBuilder;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses raw tiff location, to supply viewer volume info.
 * @author fosterl
 */
public class RawTiffVolumeSource implements MonitoredVolumeSource {
    public static final int DEPT_STD_GRAPH_CARD_MAX_DEPTH = 172;
    private static final Double[] SPIN_ABOUT_Z = new Double[] {
        -1.0,    0.0,    0.0,
        0.0,    -1.0,    0.0,
        0.0,     0.0,    1.0,
    };
    private static final int MATRIX_SQUARE_DIM = 3; //5, if from yaml

    private final Camera3d camera;
    private final String baseDirectoryPath;
    private IndeterminateNoteProgressMonitor progressMonitor;
    private final Logger logger = LoggerFactory.getLogger( RawTiffVolumeSource.class );
    private int cubicDimension = -1;
    
    /**
     * Seed this object with all it needs to fetch the volume.
     * 
     * @param camera tells user's chosen point.
     * @param baseDirectoryPath info for finding specific data set.
     */
    public RawTiffVolumeSource(
            Camera3d camera,
            String baseDirectoryPath
    ) {
        // Cloning the camera, to leave the original as was found.
        Camera3d iterationCamera = new BasicObservableCamera3d();
        iterationCamera.setFocus(camera.getFocus());
        iterationCamera.setPixelsPerSceneUnit(camera.getPixelsPerSceneUnit());
        iterationCamera.setRotation(camera.getRotation());
        
        this.camera = iterationCamera;
        this.baseDirectoryPath = baseDirectoryPath;
    }
    
    @Override
    public void setProgressMonitor(IndeterminateNoteProgressMonitor monitor) {
        this.progressMonitor = monitor;
    }

    @Override
    public void getVolume(VolumeAcceptor volumeListener) throws Exception {
        int[] viewerCoord = {
            (int)camera.getFocus().getX(),
            (int)camera.getFocus().getY(),
            (int)camera.getFocus().getZ()
        };
        
//        progressMonitor.setNote("Fetching raw file paths.");
//        RawFileInfo rawFileInfo =
//                ModelMgr.getModelMgr().getNearestFileInfo(baseDirectoryPath, viewerCoord);
//        if ( rawFileInfo == null ) {
//            throw new Exception("Failed to find any tiff files in " + baseDirectoryPath + "." );
//        }
        progressMonitor.setNote("Reading volume data.");
        Map<Integer,byte[]> byteBuffers = ModelMgr.getModelMgr().getTextureBytes(baseDirectoryPath, viewerCoord, cubicDimension);
        
        progressMonitor.setNote("Loading volume data.");        
        TifTextureBuilder tifTextureBuilder = new TifTextureBuilder();
//        final TifVolumeFileLoader tifVolumeFileLoader = new TifVolumeFileLoader();
//        if ( cubicDimension > -1 ) {
//            tifVolumeFileLoader.setCubicOutputDimension( cubicDimension );
//        }
//        tifVolumeFileLoader.setConversionCharacteristics( rawFileInfo.getTransformMatrix(), rawFileInfo.getInvertedTransform(), rawFileInfo.getMinCorner(), rawFileInfo.getExtent(), rawFileInfo.getQueryMicroscopeCoords() );

        progressMonitor.setNote("Starting data load...");
        Double[] spinAboutZTransform = SPIN_ABOUT_Z;
        ByteArrayLoader loader = new ByteArrayLoader();
        AbstractVolumeFileLoader baseLoader = (AbstractVolumeFileLoader)loader;
        baseLoader.setSx(cubicDimension);
        baseLoader.setSy(cubicDimension);
        baseLoader.setSz(cubicDimension);
        baseLoader.setChannelCount(1);
        baseLoader.setPixelBytes(2);
        tifTextureBuilder.setVolumeFileLoader( loader );
        loadChannel(1, byteBuffers.get( 0 ), spinAboutZTransform, tifTextureBuilder, loader, volumeListener);
        loadChannel(2, byteBuffers.get( 1 ), spinAboutZTransform, tifTextureBuilder, loader, volumeListener);

        progressMonitor.setNote("Launching viewer.");
    }

    @Override
    public String getInfo() {
        return String.format(COORDS_FORMAT,
                camera.getFocus().getX(),
                camera.getFocus().getY(),
                camera.getFocus().getZ()
            );
    }
    
    public void setCubicDimension( int cubicDimension ) {
        this.cubicDimension = cubicDimension;
    }

    private void loadChannel(int displayNum, byte[] inputBuffer, Double[] transformMatrix, TifTextureBuilder textureBuilder, ByteArrayLoader loader, VolumeAcceptor volumeListener) throws Exception {
        TextureDataI textureData;
        textureBuilder.loadVolumeFile("Byte-Array-" + displayNum);        
        progressMonitor.setNote("Building texture data from channel " + displayNum);
        loader.setTextureByteArray(inputBuffer);
        textureData = textureBuilder.buildTextureData(true);        
        // Presets known to work with this data type.
        textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE16);
        textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
        textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_SHORT);
        textureData.setPixelByteCount(2);
        if ( transformMatrix != null ) {
            setTransformMatrix(transformMatrix, MATRIX_SQUARE_DIM, textureData);
        }
        volumeListener.accept(textureData);
        progressMonitor.setNote("Ending load for raw file " + displayNum);
    }

//    private void loadChannel(int displayNum, FileResolver resolver, File rawFile, Double[] transformMatrix, TifTextureBuilder textureBuilder, VolumeAcceptor volumeListener) throws Exception {
//        TextureDataI textureData;
//        progressMonitor.setNote("Caching channel " + displayNum);
//        String resolvedFilename = resolver.getResolvedFilename(rawFile.getAbsolutePath());
//        progressMonitor.setNote("Loading channel " + displayNum);
//        textureBuilder.loadVolumeFile(resolvedFilename);
//        progressMonitor.setNote("Building texture data from channel " + displayNum);
//        logger.info("Loading" + rawFile + " as " + resolvedFilename);
//        textureData = textureBuilder.buildTextureData(true);        
//        // Presets known to work with this data type.
//        textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE16);
//        textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
//        textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_SHORT);
//        textureData.setPixelByteCount(2);
//        if ( transformMatrix != null ) {
//            setTransformMatrix(transformMatrix, MATRIX_SQUARE_DIM, textureData);
//        }
//        volumeListener.accept(textureData);
//        progressMonitor.setNote("Ending load for raw file " + displayNum);
//    }

    /**
     * If this has been set, it will be used to transform every point in the coordinate set.
     * 
     * @param transformMatrix applied to all points.
     * @param squareDim the transform matrix must be of size squareDim^2.
     */
    private void setTransformMatrix(Double[] transformMatrix, int squareDim, TextureDataI textureData) {        
        if ( transformMatrix.length != squareDim * squareDim ) {
            String message = String.format("The transform matrix must be of size %1$d * %1$d. Matrix size is %2$d. Transform not applied.", squareDim, transformMatrix.length);
            logger.error( message );
        }
        else {
            float[] coordTransformMatrix = new float[ transformMatrix.length ];
            for ( int i = 0; i < transformMatrix.length; i++ ) {
                coordTransformMatrix[ i ] = transformMatrix[ i ].floatValue();
            }
            textureData.setTransformMatrix(coordTransformMatrix);
        }
    }
    
}
