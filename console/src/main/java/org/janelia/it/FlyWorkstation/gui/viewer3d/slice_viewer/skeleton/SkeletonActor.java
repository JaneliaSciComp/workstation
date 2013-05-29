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
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.PassThroughTextureShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.AnchorShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.PathShader;

/**
 * SkeletonActor is responsible for painting neuron traces in the slice viewer.
 * @author brunsc
 *
 */
public class SkeletonActor 
implements GLActor
{
	// semantic constants for allocating byte arrays
	private final int floatByteCount = 4;
	private final int vertexFloatCount = 3;
	private final int intByteCount = 4;
	private final int edgeIntCount = 2;
	private final int colorFloatCount = 3;

	private Viewport viewport;
	private int hoverAnchorIndex = -1;
	private boolean bIsGlInitialized = false;
	
	private int vertexCount = 3;
	private FloatBuffer vertices;
	private FloatBuffer colors;
	private int vao = -1;
	private int vbo = -1;
	private int edgeIbo = -1;
	private int pointIbo = -1;
	private int colorBo = -1;
	private IntBuffer edgeIndices;
	private IntBuffer pointIndices;
	private boolean edgesNeedCopy = false;
	private boolean verticesNeedCopy = false;
	private PathShader edgeShader = new PathShader();
	private AnchorShader anchorShader = new AnchorShader();
	private BoundingBox3d bb = new BoundingBox3d();
	//
	private BufferedImage anchorImage;
	private int anchorTextureId = -1;
	private BufferedImage parentAnchorImage;
	private int parentAnchorTextureId = -1;
	//
	private Skeleton skeleton;
	// Vertex buffer objects need indices
	private Map<Anchor, Integer> anchorIndices = new HashMap<Anchor, Integer>();
	private Map<Integer, Anchor> indexAnchors = new HashMap<Integer, Anchor>();
	private Camera3d camera;
	
	public Signal skeletonActorChangedSignal = new Signal();
	
	private Slot updateAnchorsSlot = new Slot() {
		@Override
		public void execute() {updateAnchors();}
	};

	public SkeletonActor() {}
	
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

    		// colors
	        colors.rewind();
	        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
	        gl.glBufferData(GL2.GL_ARRAY_BUFFER, 
	        		vertexCount * floatByteCount * colorFloatCount,
	        		colors, 
	        		GL2.GL_DYNAMIC_DRAW);
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
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vbo );
        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, 0L);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
        gl.glColorPointer(colorFloatCount, GL2.GL_FLOAT, 0, 0L);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, edgeIbo);
		edgeShader.load(gl);
 		float zThickness = viewport.getDepth() / (float)camera.getPixelsPerSceneUnit();
 		edgeShader.setUniform(gl, "zThickness", (float)(zThickness));
 		edgeShader.setUniform(gl, "focusZ", (float)camera.getFocus().getZ());
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        // wider black line
		gl.glLineWidth(3.5f);
		float blackColor[] = {0,0,0};
		edgeShader.setUniform3v(gl, "baseColor", 1, blackColor);
        // gl.glColor4f(0, 0, 0, 0.7f); // black
        gl.glDrawElements(GL2.GL_LINES, 
        		edgeIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        // narrower white line
		gl.glLineWidth(1.5f);
		float whiteColor[] = {1,1,1};
		edgeShader.setUniform3v(gl, "baseColor", 1, whiteColor);
        // gl.glColor4f(1, 1, 1, 0.7f); // white
        gl.glDrawElements(GL2.GL_LINES, 
        		edgeIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        //
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);	
	    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
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
        // parent anchor texture
        gl.glActiveTexture(GL2.GL_TEXTURE1);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, parentAnchorTextureId);
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
        // plain anchor texture
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, anchorTextureId);
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
        //
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        anchorShader.load(gl);
 		anchorShader.setUniform(gl, "highlightAnchorIndex", hoverAnchorIndex);
 		int parentIndex = -1;
 		Anchor parent = skeleton.getNextParent();
 		if (parent != null) {
 			parentIndex = getIndexForAnchor(parent);
 		}
 		anchorShader.setUniform(gl, "parentAnchorIndex", parentIndex);
 		float zThickness = viewport.getDepth() / (float)camera.getPixelsPerSceneUnit();
 		anchorShader.setUniform(gl, "zThickness", (float)(zThickness));
 		anchorShader.setUniform(gl, "focusZ", (float)camera.getFocus().getZ());
 		anchorShader.setUniform(gl, "anchorTexture", 0);
 		anchorShader.setUniform(gl, "parentAnchorTexture", 1);

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
		        
		        // colors
		        colors.rewind();
		        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
		        gl.glBufferData(GL2.GL_ARRAY_BUFFER, 
		        		vertexCount * floatByteCount * colorFloatCount,
		        		colors, 
		        		GL2.GL_DYNAMIC_DRAW);
		        
		        verticesNeedCopy = false;
	        		// point indices
				pointIndices.rewind();
		        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
	    	        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, 
	    	        		pointIndices.capacity() * intByteCount,
	    	        		pointIndices, GL2.GL_DYNAMIC_DRAW);
	        }
	        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
	        gl.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, 0L);
	        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
	        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
	        gl.glColorPointer(colorFloatCount, GL2.GL_FLOAT, 0, 0L);
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
		    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
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

	public int getIndexForAnchor(Anchor anchor) {
		if (anchor == null)
			return -1;
		if (anchorIndices.containsKey(anchor))
			return anchorIndices.get(anchor);
		return -1;
	}
	
	public Anchor getAnchorAtIndex(int index) {
		return indexAnchors.get(index);
	}
	
	@Override
	public BoundingBox3d getBoundingBox3d() {
		return bb; // TODO actually populate bounding box
	}

	public Camera3d getCamera() {
		return camera;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
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
	
	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
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
		//
		ByteBuffer colorBytes = ByteBuffer.allocateDirect(vertexCount * floatByteCount * colorFloatCount);
		colorBytes.order(ByteOrder.nativeOrder());
		colors = colorBytes.asFloatBuffer();
		colors.rewind();
		// Track vertex index, to support vertex buffer object
		anchorIndices.clear();
		indexAnchors.clear();
		int edgeCount = 0;
		int pointCount = 0;
		// Populate vertex array
		for (Anchor anchor : skeleton.getAnchors()) {
			Vec3 xyz = anchor.getLocation();
			vertices.put((float)xyz.getX());
			vertices.put((float)xyz.getY());
			vertices.put((float)xyz.getZ());
			//
			colors.put(0.8f); // red
			colors.put(1.0f); // green
			colors.put(0.3f); // blue
			//
			anchorIndices.put(anchor, vertexIndex);
			indexAnchors.put(vertexIndex, anchor);
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
			int i1 = getIndexForAnchor(anchor);
			if (i1 < 0)
				continue;
			for (Anchor neighbor : anchor.getNeighbors()) {
				int i2 = getIndexForAnchor(neighbor);
				if (i2 < 0)
					continue;
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
		if (parentAnchorImage == null) {
			// load anchor texture
			String imageFileName = "ParentAnchor16.png";
			ImageIcon anchorIcon = Icons.getIcon(imageFileName);
			Image source = anchorIcon.getImage();
			int w = source.getWidth(null);
			int h = source.getHeight(null);
			parentAnchorImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D)parentAnchorImage.getGraphics();
			g2d.drawImage(source, 0, 0, null);
			g2d.dispose();
		}
		int w = anchorImage.getWidth();
		int h = anchorImage.getHeight();
		int ids[] = {0, 0};
		gl.glGenTextures(2, ids, 0); // count, array, offset
		anchorTextureId = ids[0];
		parentAnchorTextureId = ids[1];
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
		// Upload anchor texture to video card
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, anchorTextureId);
		pixels.rewind();
        gl.glTexImage2D( GL2.GL_TEXTURE_2D,
                0, // mipmap level
                GL2.GL_RGBA,
                w,
                h,
                0, // border
                GL2.GL_RGBA,
                GL2.GL_UNSIGNED_BYTE,
                pixels);
        // Parent texture is like anchor texture, but with a "P" in it.
		gl.glBindTexture(GL2.GL_TEXTURE_2D, parentAnchorTextureId);
		intPixels.rewind();
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				intPixels.put(parentAnchorImage.getRGB(x, y));
			}
		}
		pixels.rewind();
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
        int ix[] = {0, 0, 0, 0};
        gl.glGenBuffers( 4, ix, 0 );
        vbo = ix[0];
        edgeIbo = ix[1];
        pointIbo = ix[2];
        colorBo = ix[3];
        //
        gl.glBindVertexArray(0);
        //
		PassThroughTextureShader.checkGlError(gl, "load anchor texture");
	}

	@Override
	public void dispose(GL2 gl) {
		// System.out.println("dispose skeleton actor");
		bIsGlInitialized = false;
		int ix1[] = {anchorTextureId, parentAnchorTextureId};
		gl.glDeleteTextures(2, ix1, 0);
		int ix2[] = {vbo, edgeIbo, pointIbo, colorBo};
		gl.glDeleteBuffers(4, ix2, 0);
		int ix3[] = {vao};
		gl.glDeleteVertexArrays(1, ix3, 0);
	}

	public synchronized void setHoverAnchorIndex(int ix) {
		if (ix == hoverAnchorIndex) 
			return;
		hoverAnchorIndex = ix;
		skeletonActorChangedSignal.emit(); // TODO leads to instability
	}

	/*
	 * Change visual anchor position without actually changing the Skeleton model
	 */
	public void lightweightNudgeAnchor(Anchor dragAnchor, Vec3 dv) {
		if (dragAnchor == null)
			return;
		int index = getIndexForAnchor(dragAnchor);
		if (index < 0)
			return;
		int offset = index * vertexFloatCount;
		for (int i = 0; i < 3; ++i) {
			vertices.put( offset+i, (float)(vertices.get(offset+i) + dv.get(i)) );
		}
		skeletonActorChangedSignal.emit();
	}
}
