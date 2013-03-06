package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader;

import java.nio.IntBuffer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;

public class PassThroughTextureShader extends AbstractShader 
{
    protected int previousShader = 0;

	@Override
	public String getVertexShader() {
		return "PassThroughTextureVrtx.glsl";
	}

	@Override
	public String getFragmentShader() {
		return "PassThroughTextureFrag.glsl";
	}

	@Override
	public void load(GL2 gl) {
        IntBuffer buffer = IntBuffer.allocate( 1 );
        gl.glGetIntegerv( GL2.GL_CURRENT_PROGRAM, buffer );
        previousShader = buffer.get();
        int shaderProgram = getShaderProgram();
        gl.glUseProgram(shaderProgram);
	}

	protected boolean setUniform(GL2 gl, String varName, int value) 
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform1i( uniformLoc, value );
        return true;
	}
	
	protected boolean setUniform3v(GL2 gl, String varName, int vecCount, float[] data)
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform3fv( uniformLoc, vecCount, data, 0);
        return true;
	}
	
	protected boolean setUniform4v(GL2 gl, String varName, int vecCount, float[] data)
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform4fv( uniformLoc, vecCount, data, 0);
        return true;
	}
	
	@Override
    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

}
