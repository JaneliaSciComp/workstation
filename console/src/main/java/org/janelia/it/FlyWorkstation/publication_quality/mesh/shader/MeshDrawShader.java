package org.janelia.it.FlyWorkstation.publication_quality.mesh.shader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;

import javax.media.opengl.GL2;

/**
 * A shader for drawing a computed mesh "cage".
 * Created by fosterl on 4/14/14.
 */
public class MeshDrawShader extends AbstractShader {
    public static final String COLOR_UNIFORM_NAME = "color";
    public static final String VERTEX_ATTRIBUTE_NAME = "vertexAttribute";
    public static final String NORMAL_ATTRIBUTE_NAME = "normalAttribute";
    public static final String VERTEX_SHADER =   "MeshDrawSpecularVtx.glsl";
    public static final String FRAGMENT_SHADER = "MeshDrawSpecularFrg.glsl";
//    public static final String VERTEX_SHADER =   "MeshDrawVtx.glsl";
//    public static final String FRAGMENT_SHADER = "MeshDrawFrg.glsl";

    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void load(GL2 gl) throws ShaderCreationException {

    }

    @Override
    public void unload(GL2 gl) {

    }
}
