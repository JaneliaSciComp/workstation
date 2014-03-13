package org.janelia.it.FlyWorkstation.gui.slice_viewer.shader;

import java.nio.IntBuffer;

// import javax.media.opengl.GL2;


import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughTextureShader extends AbstractShader 
{
    protected int previousShader = 0;
	protected static GLU glu = new GLU();
	private static Logger logger = LoggerFactory.getLogger(PassThroughTextureShader.class);

	static public void checkGlError(GL gl, String message) 
	{
        int errorNum = gl.glGetError();
        if (errorNum == GL.GL_NO_ERROR)
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
        gl.glGetIntegerv( GL2GL3.GL_CURRENT_PROGRAM, buffer );
        previousShader = buffer.get();
        int shaderProgram = getShaderProgram();
        gl.glUseProgram(shaderProgram);
	}

	@Override
    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

}
