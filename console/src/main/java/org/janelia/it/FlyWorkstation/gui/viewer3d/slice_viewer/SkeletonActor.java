package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.OutlineShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.PassThroughTextureShader;

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
	private OutlineShader outlineShader = new OutlineShader();
	private PassThroughTextureShader textureShader = new PassThroughTextureShader();
	private BoundingBox3d bb = new BoundingBox3d();
	final int floatByteCount = 4;
	final int vertexFloatCount = 3;
	private final int colorFloatCount = 3;
	private PyramidTexture anchorTexture;

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
		outlineShader.load(gl);

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

		// Line segments using vertex array
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        vertices.rewind();
        colors.rewind();
        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, vertices);
        gl.glColorPointer(colorFloatCount, GL2.GL_FLOAT, 0, colors);
        // TODO use vertex buffer object, not just a vertex array
        gl.glColor4f(0, 1, 1, 1); // cyan
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertexCount);
        outlineShader.unload(gl);
        
        // point sprite experiment
        textureShader.load(gl);
		textureShader.setUniform(gl, "tileTexture", (int)0);
        gl.glEnable(GL2.GL_POINT_SPRITE);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glPointSize(8.0f);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        anchorTexture.enable(gl);
        gl.glTexEnvi(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL2.GL_TRUE);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        gl.glDisable(GL2.GL_LIGHTING);
        anchorTexture.bind(gl);
        vertices.rewind();
        colors.rewind();
        gl.glColor4f(0.3f, 0.3f, 0.3f, 1.0f);
        gl.glDrawArrays(GL2.GL_POINTS, 0, vertexCount);
        textureShader.unload(gl);

        // tear down
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glDisable(GL2.GL_POINT_SPRITE);
        anchorTexture.disable(gl);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return bb; // TODO actually populate bounding box
	}

	@Override
	public void init(GL2 gl) {
		// System.out.println("init");
		// compile shader
		try {
			outlineShader.init(gl);
		} catch (ShaderCreationException e) {
			e.printStackTrace();
			return;
		}
		// load anchor texture
		ImageIcon anchorIcon = Icons.getIcon("SkeletonAnchor8.png");
		Image source = anchorIcon.getImage();
		int w = source.getWidth(null);
		int h = source.getHeight(null);
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.drawImage(source, 0, 0, null);
		g2d.dispose();
		TextureData2dGL anchorData = new TextureData2dGL();
		anchorData.loadRenderedImage(image);
		anchorTexture = anchorData.createTexture(gl);
	}

	@Override
	public void dispose(GL2 gl) {
		// TODO Auto-generated method stub
		
	}
}
