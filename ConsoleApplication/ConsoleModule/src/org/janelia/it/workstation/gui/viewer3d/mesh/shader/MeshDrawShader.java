package org.janelia.it.workstation.gui.viewer3d.mesh.shader;

import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

/**
 * A shader for drawing a computed mesh "cage".
 * Created by fosterl on 4/14/14.
 */
public class MeshDrawShader extends AbstractShader {
    public static final String COLOR_UNIFORM_NAME = "color";
    public static final String COLOR_STRATEGY_UNIFORM = "colorStrategy";
    public static final String VERTEX_ATTRIBUTE_NAME = "vertexAttribute";
    public static final String NORMAL_ATTRIBUTE_NAME = "normalAttribute";
    public static final String COLOR_ATTRIBUTE_NAME = "colorAttribute";
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

    public boolean setUniform4v(GL2GL3 gl, String varName, int vecCount, float[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException("Null data pushed to uniform.");
        }
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform4fv( uniformLoc, vecCount, data, 0);
        return true;
    }

    public boolean setUniformMatrix2fv(GL2GL3 gl, String varName, boolean transpose, float[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException("Null data pushed to uniform.");
        }
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniformMatrix2fv( uniformLoc, 1, transpose, data, 0);
        return true;
    }

    public boolean setUniformMatrix4v(GL2GL3 gl, String varName, boolean transpose, float[] data) {
        if ( data == null ) {
            throw new IllegalArgumentException("Null data pushed to uniform.");
        }
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniformMatrix4fv( uniformLoc, 1, transpose, data, 0 );
        return true;
    }
    
    /** By default, coloring will be done via a uniform. */
    public void setColorByAttribute(GL2GL3 gl, boolean flag) {
        int uniformLoc = gl.glGetUniformLocation(getShaderProgram(), COLOR_STRATEGY_UNIFORM);
        if (uniformLoc < 0) {
            throw new RuntimeException("Failed to set color-by-attribute to " + flag);
        }
        gl.glUniform1i(uniformLoc, flag? 1 : 0);
    }

    @Override
    public void load(GL2 gl) {

    }

    @Override
    public void unload(GL2 gl) {

    }
}
