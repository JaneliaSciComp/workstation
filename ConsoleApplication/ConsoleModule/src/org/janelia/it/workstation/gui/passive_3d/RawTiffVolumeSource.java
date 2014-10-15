/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.io.File;
import javax.media.opengl.GL2;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.viewer3d.loader.TifFileLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
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
    public static final int USER_SUGGESTED_DEPTH = 128;//TEMP 64
    private static final Double[] SPIN_ABOUT_Z = new Double[] {
        -1.0,    0.0,    0.0,
        0.0,    -1.0,    0.0,
        0.0,     0.0,    1.0,
    };
    private static final int MATRIX_SQUARE_DIM = 3; //5, if from yaml

    private Camera3d camera;
    private String baseDirectoryPath;
    private IndeterminateNoteProgressMonitor progressMonitor;
    private final Logger logger = LoggerFactory.getLogger( RawTiffVolumeSource.class );
    private int maxDrawPlanes = -1;

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
        
        progressMonitor.setNote("Fetching tiff file paths.");
        RawFileInfo rawFileInfo =
                ModelMgr.getModelMgr().getNearestFileInfo(baseDirectoryPath, viewerCoord);
        if ( rawFileInfo == null ) {
            throw new Exception("Failed to find any tiff files in " + baseDirectoryPath + "." );
        }
        progressMonitor.setNote("Loading volume data.");
        TifFileLoader tifFileLoader = new TifFileLoader();
        if ( maxDrawPlanes > -1 ) {
            tifFileLoader.setCubicOutputDimension( maxDrawPlanes );
            int[] cameraToCentroidDistance = new int[3];
            for ( int i = 0; i < 3; i++ ) {
                cameraToCentroidDistance[i] = (int)((rawFileInfo.getQueryMicroscopeCoords().get(i) - rawFileInfo.getCentroid().get(i)) / rawFileInfo.getScale()[ i ]);
            }
            tifFileLoader.setCameraToCentroidDistance( cameraToCentroidDistance );
        }
        FileResolver resolver = new CacheFileResolver();

        progressMonitor.setNote("Starting data load...");
        Double[] spinAboutZTransform = SPIN_ABOUT_Z;
        loadChannel(1, resolver, rawFileInfo.getChannel0(), spinAboutZTransform, /*rawFileInfo.getTransformMatrix(),*/ tifFileLoader, volumeListener);
        loadChannel(2, resolver, rawFileInfo.getChannel1(), spinAboutZTransform, /*rawFileInfo.getTransformMatrix(),*/ tifFileLoader, volumeListener);

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
    
    public void setMaxDrawPlanes( int drawPlanes ) {
        this.maxDrawPlanes = drawPlanes;
    }

    private void loadChannel(int displayNum, FileResolver resolver, File rawFile, Double[] transformMatrix, TifFileLoader tifFileLoader, VolumeAcceptor volumeListener) throws Exception {
        TextureDataI textureData;
        progressMonitor.setNote("Caching channel " + displayNum);
        String resolvedFilename = resolver.getResolvedFilename(rawFile.getAbsolutePath());
        progressMonitor.setNote("Loading channel " + displayNum);
        tifFileLoader.loadVolumeFile(resolvedFilename);
        progressMonitor.setNote("Building texture data from channel " + displayNum);
        logger.info("Loading" + rawFile + " as " + resolvedFilename);
        textureData = tifFileLoader.buildTextureData(true);
        // Presets known to work with this data type.
        textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE16);
        textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
        textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_SHORT);
        textureData.setPixelByteCount(2);
        if ( transformMatrix != null ) {
            setTransformMatrix(transformMatrix, MATRIX_SQUARE_DIM, textureData);
        }
        volumeListener.accept(textureData);
        progressMonitor.setNote("Ending load for raw tiff " + displayNum);
    }

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
