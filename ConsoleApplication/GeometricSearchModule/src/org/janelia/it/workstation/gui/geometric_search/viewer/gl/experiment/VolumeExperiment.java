package org.janelia.it.workstation.gui.geometric_search.viewer.gl.experiment;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.VolumeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.VolumeShader;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;

/**
 * Created by murphys on 7/29/2015.
 */
public class VolumeExperiment implements GL4Experiment {

    private final Logger logger = LoggerFactory.getLogger(VolumeExperiment.class);

    public void setup(final VoxelViewerGLPanel viewer) {
        GL4ShaderActionSequence volumeSequence = new GL4ShaderActionSequence("Volume");
        final VolumeShader volumeShader = new VolumeShader();

        volumeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                volumeShader.setView(gl, viewMatrix);
                logger.info("View Matrix:\n"+viewMatrix.toString()+"\n");
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                volumeShader.setProjection(gl, projMatrix);
                logger.info("Projection Matrix:\n"+projMatrix.toString()+"\n");
            }
        });

        volumeSequence.setShader(volumeShader);

        //final VolumeActor volumeActor = new VolumeActor(new File("U:\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"));

        final VolumeActor volumeActor = new VolumeActor(new File("C:\\cygwin64\\home\\murphys\\volumes\\GMR_40B09_AE_01_06-fA01b_C091216_20100427171414198.reg.local.v3dpbd"));

        volumeSequence.getActorSequence().add(volumeActor);

        viewer.addShaderAction(volumeSequence);
    }



}
