package org.janelia.it.FlyWorkstation.gui.opengl;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

/**
 * Should work for both GL2 and GL3
 * @author brunsc
 *
 */
public class SolidBackgroundActor 
implements GL3Actor
{
	public enum Method {
		CLEAR_COLOR,
		QUAD,
	};
	private Method method = Method.CLEAR_COLOR;
	
    private Color color;

    // Paint a quad to cover the screen
    private float positions[] = { // order for triangle strip
    	 1,  1, 0,
    	-1,  1, 0,
    	 1, -1, 0,
    	-1, -1, 0
    };
    private float colors[] = {
    	0,0,0,
    	0,0,0,
    	0,0,0,
    	0,0,0,
    };
    
    private final int posLocation = 0; // nvidia
    private final int colLocation = 3; // nvidia
    private int vao = 0;
    private int posVbo = 0;
    private int colVbo = 0;
    
    public SolidBackgroundActor(Color color) {
        this.color = color;
        for (int p = 0; p < 4; ++p) {
        	for (int rgb = 0; rgb < 3; ++rgb) {
        		int i = 3*p + rgb;
        		switch (rgb) {
        		case 0:
        			colors[i] = color.getRed()/255.0f;
        			break;
        		case 1:
        			colors[i] = color.getGreen()/255.0f;
        			break;
        		case 2:
        			colors[i] = color.getBlue()/255.0f;
        			break;
        		}
        	}
        }
    }

    @Override
    public void display(GLActorContext context) {
    	switch (method) {
    	case CLEAR_COLOR:
    		displayClearColor(context);
    		break;
    	case QUAD:
    		displayQuad(context);
    		break;
    	}
    }

    public void displayClearColor(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    public void displayQuad(GLActorContext context) {
    	// TODO - does not work
    	GL2Adapter gl2Adapter = context.getGL2Adapter();
    	
    	gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
    	gl2Adapter.glPushMatrix();
    	gl2Adapter.glLoadIdentity();
    	gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
    	gl2Adapter.glPushMatrix();
    	gl2Adapter.glLoadIdentity();
    	
    	GL gl = context.getGLAutoDrawable().getGL();
    	GL2GL3 gl2gl3 = gl.getGL2GL3();
    	gl2gl3.glBindVertexArray(vao);
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, posVbo);
    	gl2gl3.glEnableVertexAttribArray(posLocation);
    	gl2gl3.glVertexAttribPointer(posLocation, 3, GL.GL_FLOAT, false, 0, 0);
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colVbo);
    	gl2gl3.glEnableVertexAttribArray(colLocation);
    	gl2gl3.glVertexAttribPointer(colLocation, 3, GL.GL_FLOAT, false, 0, 0);
    	gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    	
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    	gl2gl3.glBindVertexArray(0);
    	
    	gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
    	gl2Adapter.glPopMatrix();
    	gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
    	gl2Adapter.glPopMatrix();
    	
    	GLError.checkGlError(gl, "solid background");
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return null;
    }

    @Override
    public void init(GLActorContext context) {
    	switch (method) {
    	case CLEAR_COLOR:
    		initClearColor(context);
    		break;
    	case QUAD:
    		initQuad(context);
    		break;
    	}
    }
    
    public void initQuad(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        
        final int positionCount = 4;
        final int bufferByteCount = Float.SIZE/8 * 3 * positionCount;
        ByteBuffer positionByteBuffer = ByteBuffer.allocateDirect(bufferByteCount);
        FloatBuffer positionFloatBuffer = positionByteBuffer.asFloatBuffer();
        positionFloatBuffer.rewind();
        positionFloatBuffer.put(positions);
        positionFloatBuffer.flip();
        ByteBuffer colorByteBuffer = ByteBuffer.allocateDirect(bufferByteCount);
        FloatBuffer colorFloatBuffer = colorByteBuffer.asFloatBuffer();
        colorFloatBuffer.rewind();
        colorFloatBuffer.put(colors);
        colorFloatBuffer.flip();
        int ix[] = {0,0};
        gl2gl3.glGenVertexArrays(1, ix, 0);
        vao = ix[0];
        gl2gl3.glBindVertexArray(vao);
        gl.glGenBuffers(2, ix, 0);
        posVbo = ix[0];
        colVbo = ix[1];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferByteCount, positionFloatBuffer, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferByteCount, colorFloatBuffer, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl2gl3.glBindVertexArray(0);
    }

    public void initClearColor(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClearColor(
                color.getRed()/255.0f,
                color.getGreen()/255.0f,
                color.getBlue()/255.0f,
                color.getAlpha()/255.0f);
    }

    @Override
    public void dispose(GLActorContext context) 
    {
        GL gl = context.getGLAutoDrawable().getGL();
        int ix[] = {posVbo, colVbo};
        if (posVbo > 0)
        	gl.glDeleteBuffers(2, ix, 0);
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        ix[0] = vao;
        if (vao > 0)
        	gl2gl3.glDeleteBuffers(1, ix, 0);
    }

}
