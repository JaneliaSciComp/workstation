package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.OutlineShader;

/**
 * SkeletonActor is responsible for painting neuron traces in the slice viewer.
 * @author brunsc
 *
 */
public class SkeletonActor 
implements GLActor
{
	private int vertexCount = 3;
	private FloatBuffer vertices;
	private FloatBuffer colors;
	private OutlineShader shader = new OutlineShader();
	private BoundingBox3d bb = new BoundingBox3d();
	final int floatByteCount = 4;
	final int vertexFloatCount = 3;
	private final int colorFloatCount = 3;

	public SkeletonActor() {
		// Mock data for testing
		// Create a vertex array to hold the anchor locations
		// Need a "direct" buffer for native OpenGL use
		ByteBuffer vertexBytes = ByteBuffer.allocateDirect(vertexCount * floatByteCount * vertexFloatCount);
		vertexBytes.order(ByteOrder.nativeOrder()); // important!
		// vertices = GLBuffers.newDirectFloatBuffer(vertexCount*vertexFloatCount); // when my allocation does not work...
		vertices = vertexBytes.asFloatBuffer();
		vertices.rewind();
		// Hard code some anchor locations
		vertices.put(10f); vertices.put(0f); vertices.put(100f);
		vertices.put(30f); vertices.put(20f); vertices.put(100f);
		vertices.put(110f); vertices.put(100f); vertices.put(100f);
		// Colors
		ByteBuffer colorBytes = ByteBuffer.allocateDirect(vertexCount * floatByteCount * colorFloatCount);
		colorBytes.order(ByteOrder.nativeOrder());
		// colors = GLBuffers.newDirectFloatBuffer(vertexCount*colorFloatCount);
		colors = colorBytes.asFloatBuffer();
		colors.rewind();
		// Hard code some colors
		colors.put(1f); colors.put(0f); colors.put(0f); // red
		colors.put(0f); colors.put(1f); colors.put(0f); // green
		colors.put(0f); colors.put(0f); colors.put(1f); // blue
	}
	
	@Override
	public void display(GL2 gl) {
		// System.out.println("painting skeleton");
		shader.load(gl);

		gl.glLineWidth(4.0f);
		// old fashioned way as a control
		/*
        gl.glBegin(GL2.GL_LINE_STRIP);
        	gl.glColor4f(1, 0, 1, 1); // magenta
        	gl.glVertex3d(0,0,100);
        	gl.glVertex3d(20,20,100);
        	gl.glVertex3d(100,100,100);
        gl.glEnd();
        */

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        vertices.rewind();
        colors.rewind();
        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, vertices);
        gl.glColorPointer(colorFloatCount, GL2.GL_FLOAT, 0, colors);
        // TODO use vertex buffer object, not just a vertex array
        gl.glColor4f(0, 1, 1, 1); // cyan
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertexCount);
        // tear down
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);

        shader.unload(gl);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return bb; // TODO actually populate bounding box
	}

	@Override
	public void init(GL2 gl) {
		try {
			shader.init(gl);
		} catch (ShaderCreationException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public void dispose(GL2 gl) {
		// TODO Auto-generated method stub
		
	}
}
