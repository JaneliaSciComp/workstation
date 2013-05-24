package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.OutlineShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.PassThroughTextureShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.SpriteShader;

/**
 * SkeletonActor is responsible for painting neuron traces in the slice viewer.
 * @author brunsc
 *
 */
public class SkeletonActor 
implements GLActor
{
	// semantic constants for allocating byte arrays
	final int floatByteCount = 4;
	final int vertexFloatCount = 3;
	final int intByteCount = 4;
	final int edgeIntCount = 2;

	private int hoverAnchorIndex = -1;
	private boolean bIsGlInitialized = false;
	
	private int vertexCount = 3;
	private FloatBuffer vertices;
	private int vao = -1;
	private int vbo = -1;
	private int edgeIbo = -1;
	private int pointIbo = -1;
	private IntBuffer edgeIndices;
	private IntBuffer pointIndices;
	private boolean edgesNeedCopy = false;
	private boolean verticesNeedCopy = false;
	private OutlineShader edgeShader = new OutlineShader();
	private SpriteShader anchorShader = new SpriteShader();
	private BoundingBox3d bb = new BoundingBox3d();
	private BufferedImage anchorImage;
	private int anchorTextureId = -1;
	private Skeleton skeleton;
	// Vertex buffer objects need indices
	private Map<Anchor, Integer> anchorIndices = new HashMap<Anchor, Integer>();
	
	public Signal skeletonActorChangedSignal = new Signal();
	
	private Slot updateAnchorsSlot = new Slot() {
		@Override
		public void execute() {updateAnchors();}
	};

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
	}
	
	private void displayEdges(GL2 gl) {
		// Line segments using vertex buffer objects
		if (edgeIndices == null)
			return;
		if (edgeIndices.capacity() < 2)
			return;

		// if (verticesNeedCopy) {
        if (true) {
            gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vbo );
        		vertices.rewind();
	        gl.glBufferData(GL2.GL_ARRAY_BUFFER, 
	        		vertexCount * floatByteCount * vertexFloatCount, 
	        		vertices, GL2.GL_DYNAMIC_DRAW);
        		verticesNeedCopy = false;
        }
		// if (edgesNeedCopy) 
		if (true) 
		{
	        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, edgeIbo);
			edgeIndices.rewind();
	        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, 
	        		edgeIndices.capacity() * intByteCount,
	        		edgeIndices, GL2.GL_DYNAMIC_DRAW);
	        edgesNeedCopy = false;
		}
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vbo );
        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, 0L);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, edgeIbo);
		edgeShader.load(gl);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        // wider black line
		gl.glLineWidth(5.0f);
        gl.glColor4f(0, 0, 0, 0.7f); // black
        gl.glDrawElements(GL2.GL_LINES, 
        		edgeIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        // narrower white line
		gl.glLineWidth(3.0f);
        gl.glColor4f(1, 1, 1, 0.7f); // white
        gl.glDrawElements(GL2.GL_LINES, 
        		edgeIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        //
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);	
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, 0 );
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        edgeShader.unload(gl);
	}
	
	private synchronized void displayAnchors(GL2 gl) {
		// Paint anchors as point sprites
		if (pointIndices == null)
			return;
		if (pointIndices.capacity() < 1)
			return;

        gl.glEnable(GL2.GL_POINT_SPRITE);
        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
        gl.glBindTexture(GL2.GL_TEXTURE_2D, anchorTextureId);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        anchorShader.load(gl);
 		anchorShader.setUniform(gl, "highlightAnchorIndex", hoverAnchorIndex);

        // To ease transition to vbos...
		boolean bUseVertexArray = false;
		if (bUseVertexArray) {
			// This works
	        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	        vertices.rewind();
	        	gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, vertices);			
	 		boolean bUseIndices = true;
	 		if (bUseIndices) { // works
	 			pointIndices.rewind();
	 			gl.glDrawElements(GL2.GL_POINTS, pointIndices.capacity(), GL2.GL_UNSIGNED_INT, pointIndices);
	 		}
	 		else { // works
	 			gl.glDrawArrays(GL2.GL_POINTS, 0, vertexCount);
	 		}
		    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		}
		else { // vertex buffer object
			// TODO - crashes unless glBufferData called every time.
	        // if (verticesNeedCopy) {
		    if (true) {
	        		// vertices
	        		vertices.rewind();
	    			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
		        gl.glBufferData(GL2.GL_ARRAY_BUFFER, 
		        		vertexCount * floatByteCount * vertexFloatCount, 
		        		vertices, GL2.GL_DYNAMIC_DRAW);
		        verticesNeedCopy = false;
	        		// point indices
				pointIndices.rewind();
		        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
	    	        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, 
	    	        		pointIndices.capacity() * intByteCount,
	    	        		pointIndices, GL2.GL_DYNAMIC_DRAW);
	        }
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
	        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, 0L);
	        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
			PassThroughTextureShader.checkGlError(gl, "paint anchors 1");
	        gl.glDrawElements(GL2.GL_POINTS, 
	        		pointIndices.capacity(), 
	        		GL2.GL_UNSIGNED_INT, 
	        		0L);
	        // tear down
	        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		}
		// 
	    anchorShader.unload(gl);
        gl.glDisable(GL2.GL_POINT_SPRITE);
        gl.glDisable(GL2.GL_TEXTURE_2D);
	}
	
	@Override
	public void display(GL2 gl) {
		if (vertexCount <= 0)
			return;
		if ( ! bIsGlInitialized )
			init(gl);

		// System.out.println("painting skeleton");
		displayEdges(gl);
        displayAnchors(gl);
	}

	public int getAnchorIndex(Anchor anchor) {
		if (anchorIndices.containsKey(anchor))
			return anchorIndices.get(anchor);
		return -1;
	}
	
	@Override
	public BoundingBox3d getBoundingBox3d() {
		return bb; // TODO actually populate bounding box
	}

	public Skeleton getSkeleton() {
		return skeleton;
	}
	

	public void setSkeleton(Skeleton skeleton) {
		if (skeleton == this.skeleton)
			return;
		if (this.skeleton != null) { 
			// disconnect previous skeleton, if any
			this.skeleton.skeletonChangedSignal.deleteObserver(updateAnchorsSlot);
		}
		this.skeleton = skeleton;
		updateAnchors();
		skeleton.skeletonChangedSignal.connect(updateAnchorsSlot);
	}
	
	protected synchronized void updateAnchors() {
		if (skeleton == null)
			return;
		vertexCount = skeleton.getAnchors().size();
		ByteBuffer vertexBytes = ByteBuffer.allocateDirect(vertexCount * floatByteCount * vertexFloatCount);
		vertexBytes.order(ByteOrder.nativeOrder()); // important!
		vertices = vertexBytes.asFloatBuffer();
		vertices.rewind();
		int vertexIndex = 0;
		// Track vertex index, to support vertex buffer object
		anchorIndices.clear();
		int edgeCount = 0;
		int pointCount = 0;
		// Populate vertex array
		for (Anchor anchor : skeleton.getAnchors()) {
			Vec3 xyz = anchor.getLocation();
			vertices.put((float)xyz.getX());
			vertices.put((float)xyz.getY());
			vertices.put((float)xyz.getZ());
			anchorIndices.put(anchor, vertexIndex);
			vertexIndex += 1;
			pointCount += 1;
			edgeCount += anchor.getNeighbors().size();
		}
		// Populate edge index buffer
		edgeCount = edgeCount / 2; // both forward and back directions were counted.
		ByteBuffer edgeBytes = ByteBuffer.allocateDirect(edgeCount*intByteCount*edgeIntCount);
		edgeBytes.order(ByteOrder.nativeOrder());
		edgeIndices = edgeBytes.asIntBuffer();
		edgeIndices.rewind();
		for (Anchor anchor : skeleton.getAnchors()) {
			int i1 = anchorIndices.get(anchor);
			for (Anchor neighbor : anchor.getNeighbors()) {
				int i2 = anchorIndices.get(neighbor);
				if (i1 < i2) {// only use ascending pairs, for uniqueness
					edgeIndices.put(i1);
					edgeIndices.put(i2);
				}
			}
		}
		edgeIndices.rewind();
		edgesNeedCopy = true;
		// Populate point index buffer
		ByteBuffer pointBytes = ByteBuffer.allocateDirect(pointCount*intByteCount);
		pointBytes.order(ByteOrder.nativeOrder());
		pointIndices = pointBytes.asIntBuffer();
		pointIndices.rewind();
		for (Anchor anchor : skeleton.getAnchors()) {
			int i1 = anchorIndices.get(anchor);
			pointIndices.put(i1);
		}
		verticesNeedCopy = true;
		//
		skeletonActorChangedSignal.emit();
	}
	

	@Override
	public void init(GL2 gl) {
		// Required for gl_VertexID to be found in shader
		// System.out.println("init");
		// compile shader
		try {
			edgeShader.init(gl);
			anchorShader.init(gl);
		} catch (ShaderCreationException e) {
			e.printStackTrace();
			return;
		}
		
		if (anchorImage == null) {
			// load anchor texture
			String imageFileName = "SkeletonAnchor16.png";
			ImageIcon anchorIcon = Icons.getIcon(imageFileName);
			Image source = anchorIcon.getImage();
			int w = source.getWidth(null);
			int h = source.getHeight(null);
			anchorImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D)anchorImage.getGraphics();
			g2d.drawImage(source, 0, 0, null);
			g2d.dispose();
		}
		int w = anchorImage.getWidth();
		int h = anchorImage.getHeight();
		int ids[] = {0};
		gl.glGenTextures(1, ids, 0); // count, array, offset
		anchorTextureId = ids[0];
		byte byteArray[] = new byte[w*h*4];
		ByteBuffer pixels = ByteBuffer.wrap(byteArray);
		pixels.order(ByteOrder.nativeOrder());
		IntBuffer intPixels = pixels.asIntBuffer();
		// Produce image pixels
		intPixels.rewind();
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				intPixels.put(anchorImage.getRGB(x, y));
			}
		}
		pixels.rewind();
		// Upload anchor texture to video card
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, anchorTextureId);
		pixels.rewind();
		intPixels.rewind();
        gl.glTexImage2D( GL2.GL_TEXTURE_2D,
                0, // mipmap level
                GL2.GL_RGBA,
                w,
                h,
                0, // border
                GL2.GL_RGBA,
                GL2.GL_UNSIGNED_BYTE,
                pixels);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        // Create a buffer object for edge indices
        int ix2[] = {0};
        gl.glGenVertexArrays(1, ix2, 0);
        vao = ix2[0];
        gl.glBindVertexArray(vao);
        //
        int ix[] = {0, 0, 0};
        gl.glGenBuffers( 3, ix, 0 );
        vbo = ix[0];
        edgeIbo = ix[1];
        pointIbo = ix[2];
        //
        gl.glBindVertexArray(0);
        //
		PassThroughTextureShader.checkGlError(gl, "load anchor texture");
	}

	@Override
	public void dispose(GL2 gl) {
		// System.out.println("dispose skeleton actor");
		bIsGlInitialized = false;
		int ix1[] = {anchorTextureId};
		gl.glDeleteTextures(1, ix1, 0);
		int ix2[] = {vbo, edgeIbo, pointIbo};
		gl.glDeleteBuffers(3, ix2, 0);
		int ix3[] = {vao};
		gl.glDeleteVertexArrays(1, ix3, 0);
	}

	public synchronized void setHoverAnchorIndex(int ix) {
		if (ix == hoverAnchorIndex) 
			return;
		hoverAnchorIndex = ix;
		skeletonActorChangedSignal.emit(); // TODO leads to instability
	}
}
