package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.nio.IntBuffer;
import javax.media.opengl.GL2;

public class VoxelRayShader extends AbstractShader {
    public static final String VERTEX_SHADER = "shaders/VoxelRayVtx.glsl";
    public static final String FRAGMENT_SHADER = "shaders/VoxelRayFrg.glsl";
    int shaderProgram = 0;
    int previousShader = 0;
    private int[] textureVoxels;
    private double[] voxelMicrometers;

    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    public void load(GL2 gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, buffer);
        previousShader = buffer.get();
        gl.glUseProgram(shaderProgram);
        // The default texture unit is 0.  Pass through shader works without setting volumeTexture.
        gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "volumeTexture"), 0);
        gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "textureVoxels"),
                (float)textureVoxels[0],
                (float)textureVoxels[1],
                (float)textureVoxels[2]);
        gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "voxelMicrometers"),
                (float)voxelMicrometers[0],
                (float)voxelMicrometers[1],
                (float)voxelMicrometers[2]);
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    public void setUniforms(int[] textureVoxels, double[] voxelMicrometers) {
        this.textureVoxels = textureVoxels;
        this.voxelMicrometers = voxelMicrometers;
    }

}
