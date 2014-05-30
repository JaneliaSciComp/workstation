package org.janelia.it.workstation.gui.opengl;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import org.janelia.it.workstation.gui.opengl.shader.BasicShader;
import org.janelia.it.workstation.gui.opengl.shader.SolidColor120Shader;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;

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
        QUAD_VBO,
    };
    private Method method = Method.QUAD_VBO;
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
        case QUAD_VBO:
            displayQuadVbo(context);
            break;
        default:
            break;
        }
    }

    public void displayClearColor(GLActorContext context) { // works
        GL gl = context.getGLAutoDrawable().getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    public void displayQuadVbo(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        GL2GL3 gl2gl3 = gl.getGL2GL3();

        GLError.checkGlError(gl, "SolidBackgroundActor displayQuadVbo 77");
        shader.load(gl2gl3);
        GLError.checkGlError(gl, "SolidBackgroundActor displayQuadVbo 79");
        shader.setUniform4v(gl2gl3, "color", 1, color);

        GLError.checkGlError(gl, "SolidBackgroundActor displayQuadVbo 82");
        gl2gl3.glBindVertexArray(vao);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, positionCount * 3);
        gl2gl3.glBindVertexArray(0);

        shader.unload(gl2gl3);        
        GLError.checkGlError(gl, "SolidBackgroundActor displayQuadVbo 88");
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
        case QUAD_VBO:
            initQuad(context);
            break;
        }
        initialized = true;
    }

    public void initQuad(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 151");
        GL2GL3 gl2gl3 = gl.getGL2GL3();

        try {
            shader.init(gl2gl3);
        } catch (BasicShader.ShaderCreationException e) {
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

        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 172");
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
        
        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 187");
        int vertexLoc = gl2gl3.glGetAttribLocation(
                shader.getShaderProgram(), "position");
        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 190");
        gl2gl3.glEnableVertexAttribArray(vertexLoc);
        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 192");
        gl2gl3.glVertexAttribPointer(vertexLoc, 3, GL.GL_FLOAT, false, 0, 0);

        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 195");
        gl2gl3.glBindVertexArray(0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        GLError.checkGlError(gl, "SolidBackgroundActor initQuad 198");
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
