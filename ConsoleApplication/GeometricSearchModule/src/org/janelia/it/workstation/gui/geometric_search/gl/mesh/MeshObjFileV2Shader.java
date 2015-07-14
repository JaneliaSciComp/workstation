package org.janelia.it.workstation.gui.geometric_search.gl.mesh;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.gui.geometric_search.gl.GL4Shader;

import javax.media.opengl.GL4;

/**
 * Created by murphys on 4/20/15.
 */
public class MeshObjFileV2Shader extends GL4Shader {
    @Override
    public String getVertexShaderResourceName() {
        return "MeshObjFileV2Vertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "MeshObjFileV2Fragment.glsl";
    }

    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
    }

}
