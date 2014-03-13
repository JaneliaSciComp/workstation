package org.janelia.it.FlyWorkstation.gui.viewer3d.shader;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;

/**
 * Axes Actor needs shading, and will leverage this class.
 *
 * Created by fosterl on 3/12/14.
 */
public class AxesShader extends AbstractShader {
    public static final String COLOR_UNIFORM_NAME = "color";
    public static final String VERTEX_ATTRIBUTE_NAME = "vertexAttribute";
    public static final String VERTEX_SHADER =   "AxesVtx.glsl";
    public static final String FRAGMENT_SHADER = "AxesFrg.glsl";

    private int shaderProgram = 0;
    private int previousShader = 0;

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
        super.init( gl );
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, buffer);
        previousShader = buffer.get();
        shaderProgram = getShaderProgram();
        gl.glUseProgram(shaderProgram);
    }

    @Override
    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

}
