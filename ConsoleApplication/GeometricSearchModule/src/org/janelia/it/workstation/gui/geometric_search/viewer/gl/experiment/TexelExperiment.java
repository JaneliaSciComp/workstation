package org.janelia.it.workstation.gui.geometric_search.viewer.gl.experiment;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.TexelActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.TexelShader;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 7/29/2015.
 */
public class TexelExperiment  implements GL4Experiment {

    public void setup(final VoxelViewerGLPanel viewer) {

        GL4ShaderActionSequence actionSequence = new GL4ShaderActionSequence("Texel Experiment");

        final TexelShader shader = new TexelShader();

        shader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                int uniformLoc = gl.glGetUniformLocation(shader.getShaderProgram(), "tex");
                gl.glUniform1i(uniformLoc, 0);
            }
        });

        final TexelActor texelActor = new TexelActor();

        actionSequence.setShader(shader);
        actionSequence.getActorSequence().add(texelActor);

        viewer.addShaderAction(actionSequence);
    }
}
