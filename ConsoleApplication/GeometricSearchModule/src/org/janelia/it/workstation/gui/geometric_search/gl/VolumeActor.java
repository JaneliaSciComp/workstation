/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.gl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author murphys
 */
public class VolumeActor extends GL4SimpleActor implements VolumeDataAcceptor {
    private final Logger logger = LoggerFactory.getLogger(VolumeActor.class);
        
    File volumeFile;
    boolean loaded=false;
    boolean loadError=false;
    boolean drawLines=false;
    IntBuffer textureId=IntBuffer.allocate(1);
    TextureDataI textureData;

    public VolumeActor(File volumeFile) {
        this.volumeFile=volumeFile;
    }

    public void setDrawLines(boolean drawLines) {
        this.drawLines=drawLines;
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
    }

    @Override
    public void init(GL4 gl) {
        if (!loaded) {
            try {
                loadVolumeFile();
            } catch (Exception ex) {
                logger.error("Could not load file "+volumeFile.getAbsolutePath());
                ex.printStackTrace();
                loadError=true;
                return;
            }
            loaded=true;
        }


    }

    @Override
    public void dispose(GL4 gl) {
    }

    private void loadVolumeFile() throws Exception {
        FileResolver resolver = new TrivialFileResolver();
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        volumeLoader.loadVolume(volumeFile.getAbsolutePath());
        volumeLoader.populateVolumeAcceptor(this);
    }

    @Override
    public void setPrimaryTextureData(TextureDataI textureData) {
        this.textureData=textureData;
    }

    @Override
    public void addTextureData(TextureDataI textureData) {
        // do nothing
    }


}
