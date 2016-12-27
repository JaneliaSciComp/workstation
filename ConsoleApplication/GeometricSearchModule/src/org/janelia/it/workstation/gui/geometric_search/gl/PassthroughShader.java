package org.janelia.it.workstation.gui.geometric_search.gl;

import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 4/28/15.
 */
public class PassthroughShader extends GL4Shader {
    @Override
    public String getVertexShaderResourceName() {
        return "PassthroughVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "PassthroughFragment.glsl";
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
    }

    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
    }

}
