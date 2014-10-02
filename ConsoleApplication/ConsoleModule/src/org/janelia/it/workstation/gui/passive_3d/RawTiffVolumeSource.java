/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import java.util.List;
import javax.media.opengl.GL2;
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

    private Camera3d camera;
    private String baseDirectoryPath;
    private IndeterminateNoteProgressMonitor progressMonitor;
    private final Logger logger = LoggerFactory.getLogger( RawTiffVolumeSource.class );

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
        List<String> tiffFiles =
                ModelMgr.getModelMgr().getTiffTilePaths(baseDirectoryPath, viewerCoord);
        if ( tiffFiles == null ) {
            throw new Exception("Failed to find any tiff files in " + baseDirectoryPath + "." );
        }
        if ( tiffFiles.size() < 2 ) {
            throw new Exception("Failed to find enough tiff files in " + baseDirectoryPath + ".  Found only " + tiffFiles.size());
        }
        progressMonitor.setNote("Loading volume data.");
        TifFileLoader tifFileLoader = new TifFileLoader();
        FileResolver resolver = new CacheFileResolver();

        for ( int i = 0; i < 2; i++ ) {
            TextureDataI textureData;
            tifFileLoader.loadVolumeFile( resolver.getResolvedFilename(tiffFiles.get( i )) );
            System.out.println("Loading " + tiffFiles.get(i));
            textureData = tifFileLoader.buildTextureData(true);
            // Presets known to work with this data type.
//            textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE16);
//            textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE_ALPHA);
            textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE16);
            textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
            textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_SHORT);
            textureData.setPixelByteCount(2);
            volumeListener.accept(textureData);
        }
        
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

}
