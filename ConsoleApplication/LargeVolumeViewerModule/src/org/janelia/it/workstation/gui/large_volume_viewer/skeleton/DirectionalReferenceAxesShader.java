/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import org.janelia.it.workstation.gui.viewer3d.OpenGLUtils;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;

/**
 * This is the CPU-side wrapper around the shader used by the directional
 * reference axes actor.
 *
 * @author fosterl
 */
public class DirectionalReferenceAxesShader extends AbstractShader {
    public static final String MODEL_VIEW_UNIFORM_NAME = "modelView";
    public static final String PROJECTION_UNIFORM_NAME = "projection";
    public static final String VERTEX_ATTRIBUTE_NAME = "vertexAttribute";
    public static final String COLOR_UNIFORM_NAME = "color";

    private static final String VERTEX_SHADER = "DirectionalReferenceAxesVtx.glsl";
    private static final String FRAGMENT_SHADER = "DirectionalReferenceAxesFrg.glsl";

    private int vertexAttribLoc;

    @Override
    public void init(GL2 gl) throws ShaderCreationException {
        super.init(gl);
        vertexAttribLoc = gl.glGetAttribLocation(getShaderProgram(), DirectionalReferenceAxesShader.VERTEX_ATTRIBUTE_NAME);
        OpenGLUtils.reportError(gl, "Getting vertex attribute location.");
    }
    
    public int getVertexAttribLoc() {
        return vertexAttribLoc;
    }

    public boolean setUniformMatrix4v(GL2GL3 gl, String varName, boolean transpose, float[] data) {
        validateDataArray(data);
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) {
            return false;
        }
        gl.glUniformMatrix4fv( uniformLoc, 1, transpose, data, 0 );
        return true;
    }

    public boolean setUniform4fv(GL2GL3 gl, String varName, float[] data) {
        validateDataArray(data);
        if (data.length != 4) {
            throw new IllegalArgumentException("Wrong sized data pushed for vec4");
        }
        int uniformLoc = gl.glGetUniformLocation(getShaderProgram(), varName);
        if (uniformLoc < 0) {
            return false;
        }
        gl.glUniform4fv( uniformLoc, 1, data, 0 );
        return true;
    }
    
    //--------------------------OVERRIDE AbstractShader
    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void load(GL2 gl) {
    }

    @Override
    public void unload(GL2 gl) {
    }

    protected void validateDataArray(float[] data) throws IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Null data pushed to uniform.");
        }
    }

}
