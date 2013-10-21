package org.janelia.it.FlyWorkstation.gui.opengl;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

import org.janelia.it.FlyWorkstation.gui.opengl.shader.BasicShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.opengl.shader.SolidColor120Shader;
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
		CLEAR_COLOR, // simple, but fails with interleaved stereo modes
		QUAD_IMMEDIATE,
		QUAD_VBO,
		QUAD_GL3,
	};
	private Method method = 
			// Method.QUAD_GL3; // goal
			Method.QUAD_IMMEDIATE; // works, but GL2 only
			// Method.QUAD_VBO; // fails (no background painted)
	private boolean initialized = false;
    private float[] color = {1,0,1,1};
    private SolidColor120Shader shader = new SolidColor120Shader();

    // Paint a quad to cover the screen
    private final float dx = 1.0f; // 1.0 to exactly cover viewport
    private final int positionCount = 4;
    private float positions[] = { // order for triangle strip
    	 dx, -dx, 0.5f,
    	 dx,  dx, 0.5f,
      	-dx, -dx, 0.5f,
    	-dx,  dx, 0.5f,
    };

    private final int vertexLocation = 0; // nvidia
    private int vao = 0;
    private int posVbo = 0;
    
    public SolidBackgroundActor(Color color) {
        this.color[0] = color.getRed()/255f;
        this.color[1] = color.getGreen()/255f;
        this.color[2] = color.getBlue()/255f;
        this.color[3] = color.getAlpha()/255f;
    }

    @Override
    public void display(GLActorContext context) {
    	if (! initialized)
    		init(context);
    	switch (method) {
    	case CLEAR_COLOR:
    		displayClearColor(context);
    		break;
    	case QUAD_GL3:
    		displayQuadGL3(context);
    		break;
		case QUAD_IMMEDIATE:
			displayQuadImmediate(context);
			break;
		default:
			break;
    	}
    }

    public void displayClearColor(GLActorContext context) { // works
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    public void displayQuadImmediate(GLActorContext context) { // works
    	GL gl = context.getGLAutoDrawable().getGL();
    GLError.checkGlError(gl, "solid background QUAD_GL2 86");
    	GL2 gl2 = gl.getGL2();
    	GL2GL3 gl2gl3 = gl.getGL2GL3();
    	
    	gl2.glMatrixMode(GL2.GL_PROJECTION);
        GLError.checkGlError(gl, "solid background QUAD_GL2 91");
    	gl2.glPushMatrix();
    	gl2.glLoadIdentity();
    GLError.checkGlError(gl, "solid background QUAD_GL2 94");
    	gl2.glMatrixMode(GL2.GL_MODELVIEW);
    	gl2.glPushMatrix();
    	gl2.glLoadIdentity();

    	shader.load(gl2);
    	shader.setUniform4v(gl2gl3, "color", 1, color);
    	
    GLError.checkGlError(gl, "solid background QUAD_GL2 100");
    
    	gl2.glDisable(GL2.GL_LIGHTING);
    	gl2.glColor4f(color[0], color[1], color[2], color[3]);
    	gl2.glBegin(GL.GL_TRIANGLE_STRIP);
    	for (int p = 0; p < positionCount; ++p)
    		gl2.glVertex3f(positions[3*p+0], positions[3*p+1], positions[3*p+2]);
    	gl2.glEnd();
    	
    	gl2.glMatrixMode(GL2.GL_PROJECTION);
    	gl2.glPopMatrix();
    	gl2.glMatrixMode(GL2.GL_MODELVIEW);
    	gl2.glPopMatrix();

    	shader.unload(gl2);
    	
    	GLError.checkGlError(gl, "solid background QUAD_GL2");
    }
    
    public void displayQuadVbo(GLActorContext context) {
    	GL gl = context.getGLAutoDrawable().getGL();
    	GL2GL3 gl2gl3 = gl.getGL2GL3();
    	GL2Adapter gl2a = context.getGL2Adapter();
    	    	
    	gl2a.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
    	gl2a.glPushMatrix();
    	gl2a.glLoadIdentity();
    	gl2a.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
    	gl2a.glPushMatrix();
    	gl2a.glLoadIdentity();
    	
    	shader.load(gl2gl3);
    	shader.setUniform4v(gl2gl3, "color", 1, color);
    	
    	gl2gl3.glBindVertexArray(vao);
    	
    	int vertexLoc = gl2gl3.glGetAttribLocation(
    			shader.getShaderProgram(), "gl_Vertex");
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, posVbo);
    	gl2gl3.glEnableVertexAttribArray(vertexLoc);
    	gl2gl3.glVertexAttribPointer(vertexLoc, 3, GL.GL_FLOAT, false, 0, 0);
    	
    	gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, positionCount * 3);
    	
    	gl2a.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
    	gl2a.glPopMatrix();
    	gl2a.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
    	gl2a.glPopMatrix();
    	
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    	
        gl2gl3.glBindVertexArray(0);
    	
        shader.unload(gl2gl3);        
    	GLError.checkGlError(gl, "solid background");
    }

    public void displayQuadGL3(GLActorContext context) {
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
    	gl2gl3.glEnableVertexAttribArray(vertexLocation);
    	gl2gl3.glVertexAttribPointer(vertexLocation, 3, GL.GL_FLOAT, false, 0, 0);

    	gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, positionCount * 3);
    	
    	gl2gl3.glBindVertexArray(0);
    	gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    	
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
    	case QUAD_GL3:
    		initQuad(context);
    		break;
		case QUAD_IMMEDIATE:
    		try {
				shader.init(context.getGLAutoDrawable().getGL().getGL2GL3());
			} catch (GLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ShaderCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case QUAD_VBO:
			initQuad(context);
			break;
    	}
    	initialized = true;
    }
    
    public void initQuad(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        GL2 gl2 = gl.getGL2();
        
        try {
			shader.init(gl2gl3);
		} catch (ShaderCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        int ix[] = {0};
        gl2gl3.glGenVertexArrays(1, ix, 0);
        vao = ix[0];
        gl2gl3.glBindVertexArray(vao);
        gl.glGenBuffers(1, ix, 0);
        posVbo = ix[0];

        final int positionCount = 4;
        final int bufferByteCount = Float.SIZE/8 * 3 * positionCount;

        gl2gl3.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        
        // VERTEX
        ByteBuffer positionByteBuffer = ByteBuffer.allocateDirect(bufferByteCount);
        positionByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionFloatBuffer = positionByteBuffer.asFloatBuffer();
        positionFloatBuffer.rewind();
        for (float f : positions) {
        	positionFloatBuffer.put(f);
        }
        positionFloatBuffer.flip();
        positionFloatBuffer.rewind();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferByteCount, positionFloatBuffer, GL.GL_STATIC_DRAW);
        positionFloatBuffer.rewind();
        gl2.glVertexPointer(3, GL.GL_FLOAT, 0, 0);

        gl2gl3.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void initClearColor(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClearColor(color[0], color[1], color[2], color[3]);
    }

    @Override
    public void dispose(GLActorContext context) 
    {
        GL gl = context.getGLAutoDrawable().getGL();
        int ix[] = {posVbo};
        if (posVbo > 0)
        	gl.glDeleteBuffers(1, ix, 0);
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        ix[0] = vao;
        if (vao > 0)
        	gl2gl3.glDeleteVertexArrays(1, ix, 0);
        posVbo = vao = 0;
        initialized = false;
    }

}
