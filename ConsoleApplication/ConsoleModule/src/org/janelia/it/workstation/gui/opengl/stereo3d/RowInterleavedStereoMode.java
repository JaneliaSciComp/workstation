package org.janelia.it.workstation.gui.opengl.stereo3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawable;

import org.janelia.it.workstation.gui.opengl.GL2Adapter;
import org.janelia.it.workstation.gui.opengl.GLActorContext;
import org.janelia.it.workstation.gui.opengl.GLError;
import org.janelia.it.workstation.gui.opengl.GLSceneComposer;

public class RowInterleavedStereoMode extends BasicStereoMode
{
	private boolean isInitialized = false;
	
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        GL gl = glDrawable.getGL();
        gl.glEnable(GL.GL_STENCIL_TEST);
        // TODO - do not init every frame
    	init(actorContext);
    	if (! isInitialized) {
    		init(actorContext);
    		isInitialized = true;
    	}
        updateViewport(glDrawable); // Just one viewport for interleaved
        gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP); // don't modify the stencil buffer

        // Even left
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glStencilFunc(GL.GL_EQUAL, 0, ~0);
        composer.displayScene(actorContext);

        // Odd right
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glStencilFunc(GL.GL_NOTEQUAL, 0, ~0);
        composer.displayScene(actorContext);

        // Restore
        gl.glDisable(GL.GL_STENCIL_TEST);
    }
    
    protected void init(GLActorContext context) {
    	GLDrawable g = context.getGLAutoDrawable();
    	int w = g.getWidth();
    	int h = g.getHeight();
    	int top = 0;
    	int left = 0;
    	NativeSurface ns = g.getNativeSurface();
    	if (ns instanceof NativeWindow) {
    		NativeWindow nw = (NativeWindow) ns;
    		Point p = nw.getLocationOnScreen(null);
    		top = p.getY();
    		left = p.getX();
    	}
    	fillStencil(left, top, w, h, context);
    }
    
    protected void fillStencil(int left, int top, int width, int height,
    		GLActorContext actorContext)
    {
    	// Avoid using explicit GL2, for forward compatibility
    	GL gl = actorContext.getGLAutoDrawable().getGL();
    	GL2GL3 gl2gl3 = gl.getGL2GL3();
    	GL2Adapter gl2Adapter = actorContext.getGL2Adapter();
    	
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl2Adapter.glPushMatrix();
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl2Adapter.glPushMatrix();

        // Modify only the stencil buffer
        gl.glDisable(GL.GL_DEPTH_TEST); // don't modify depth buffer
        gl2gl3.glDrawBuffer(GL.GL_BACK);
        gl.glColorMask(false, false, false, false); // don't modify color buffer
        gl.glDepthMask(false);
        gl.glEnable(GL.GL_STENCIL_TEST);

        // Draw a rectangle over the full screen, into the stencil buffer
        // using the simplest possible OpenGL geometry
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl2Adapter.glLoadIdentity();
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl2Adapter.glLoadIdentity();
        gl.glViewport(0, 0, width, height);
        gl2Adapter.glOrtho(0.0, width, 0.0, height, -1.0, 1.0); // 2D orthographic projection

        gl.glClearStencil(0x0);
        gl.glClear(GL.GL_STENCIL_BUFFER_BIT);
        gl.glStencilOp(GL.GL_INVERT, GL.GL_INVERT, GL.GL_INVERT); // Modify the stencil buffer everywhere
        gl.glStencilFunc(GL.GL_ALWAYS, ~0, ~0);

        gl.glLineWidth(1.0f);
        gl.glDisable(GL.GL_LINE_SMOOTH);

        // construct vbos
        int[] lineVbo = {0};
        gl.glGenBuffers(1, lineVbo, 0);
        int[] arrayObject = {0};
        gl2gl3.glGenVertexArrays(1, arrayObject, 0);
        gl2gl3.glBindVertexArray(arrayObject[0]);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, lineVbo[0]);
        gl2gl3.glEnableVertexAttribArray(0);
        gl2gl3.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
        // paint stencil for interleaved rendering
        // It might not be possible to guarantee the registration of the stippling.  So use GL.GL_LINES
        int offset = 0;
        // blank alternate rows
        offset = top % 2; // even=>0; odd=>1
        int lineCount = 0;
        for(float y = 0.5f - offset + height; y >= 0; y -= 2) // loop first time to count
            lineCount += 1;
        final int floatsPerVertex = 3;
        final int verticesPerLine = 2;
        int bufferByteCount = Float.SIZE/8 * floatsPerVertex * verticesPerLine * lineCount;
        ByteBuffer lineByteBuffer = ByteBuffer.allocateDirect(bufferByteCount);
        lineByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = lineByteBuffer.asFloatBuffer();
        vertices.rewind();
        for(float y = 0.5f - offset + height; y >= 0; y -= 2) { // loop second time
        	vertices.put(0); vertices.put(y); vertices.put(0);
        	vertices.put(width); vertices.put(y); vertices.put(0);
        }
        
        // System.out.println("height = "+height);
        
        // render lines
        vertices.rewind();
        gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferByteCount, vertices, GL.GL_STATIC_DRAW);
		gl.glDrawArrays(GL.GL_LINES, 0, lineCount*2);
		
		// clean up
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glDeleteBuffers(1, lineVbo, 0);
		gl2gl3.glDeleteVertexArrays(1, arrayObject, 0);

        // Restore OpenGL state
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_PROJECTION);
        gl2Adapter.glPopMatrix();
        gl2Adapter.glMatrixMode(GL2Adapter.MatrixMode.GL_MODELVIEW);
        gl2Adapter.glPopMatrix();
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glColorMask(true, true, true, true);
        GLError.checkGlError(gl, "fill stencil 10");
    }

}
