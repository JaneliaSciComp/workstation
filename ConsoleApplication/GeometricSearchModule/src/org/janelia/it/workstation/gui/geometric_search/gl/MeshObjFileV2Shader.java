package org.janelia.it.workstation.gui.geometric_search.gl;

import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL3;

/**
 * Created by murphys on 4/20/15.
 */
public class MeshObjFileV2Shader extends GL3Shader {
    @Override
    public String getVertexShaderResourceName() {
        return "MeshObjFileV2Vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "MeshObjFileV2Fragment.glsl";
    }

    public void setProjection(GL3 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
    }

    public void setView(GL3 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
    }

    public void setModel(GL3 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
    }

}
