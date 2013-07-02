package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader;

import java.nio.IntBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughTextureShader extends AbstractShader 
{
    protected int previousShader = 0;
	protected static GLU glu = new GLU();
	private static Logger logger = LoggerFactory.getLogger(PassThroughTextureShader.class);

	static public void checkGlError(GL2 gl, String message) 
	{
        int errorNum = gl.glGetError();
        if (errorNum == GL2.GL_NO_ERROR)
        		return;
        String errorStr = glu.gluErrorString(errorNum);
        logger.error( "OpenGL Error " + errorNum + ": " + errorStr + ": " + message );	
	}	

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

	public boolean setUniform(GL2 gl, String varName, float value) {
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform1f( uniformLoc, value );
        return true;
	}

	public boolean setUniform(GL2 gl, String varName, int value) 
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform1i( uniformLoc, value );
        return true;
	}
	
	public boolean setUniform2fv(GL2 gl, String varName, int vecCount, float[] data)
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform2fv( uniformLoc, vecCount, data, 0);
        return true;
	}
	
	public boolean setUniform3v(GL2 gl, String varName, int vecCount, float[] data)
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform3fv( uniformLoc, vecCount, data, 0);
        return true;
	}
	
	public boolean setUniform4v(GL2 gl, String varName, int vecCount, float[] data)
	{
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniform4fv( uniformLoc, vecCount, data, 0);
        return true;
	}
	
	public boolean setUniformMatrix2fv(GL2 gl, String varName, boolean transpose, float[] data) {
        int uniformLoc = gl.glGetUniformLocation( getShaderProgram(), varName );
        if ( uniformLoc < 0 ) 
        	return false;
        gl.glUniformMatrix2fv( uniformLoc, 1, transpose, data, 0);
        return true;		
	}
	
	@Override
    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

}
