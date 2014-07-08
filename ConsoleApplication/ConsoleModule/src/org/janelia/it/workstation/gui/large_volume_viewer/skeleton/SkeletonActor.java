package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
// import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.ImageIcon;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.AnchorShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.PassThroughTextureShader;
// import TracedPathShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.PathShader;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.janelia.it.workstation.signal.Signal;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SkeletonActor is responsible for painting neuron traces in the large volume viewer.
 * @author brunsc
 *
 */
public class SkeletonActor 
implements GLActor
{
	private static final Logger log = LoggerFactory.getLogger(SkeletonActor.class);
	
	// semantic constants for allocating byte arrays
	private final int floatByteCount = 4;
	private final int vertexFloatCount = 3;
	private final int intByteCount = 4;
	private final int colorFloatCount = 3;

	private int hoverAnchorIndex = -1;
	private boolean bIsGlInitialized = false;
	
	private int vertexCount = 3;
	private FloatBuffer vertices;
	private FloatBuffer colors;
	private int vbo = -1;
	private int lineIbo = -1;
	private int pointIbo = -1;
	private int colorBo = -1;
	private IntBuffer lineIndices;
	private IntBuffer pointIndices;
	private boolean linesNeedCopy = false;
	private boolean verticesNeedCopy = false;
	private PathShader lineShader = new PathShader();
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
	private float zThicknessInPixels = 100;
	//
	private Anchor nextParent = null;
	//
    private boolean bIsVisible = true;
    
    private Map<SegmentIndex, TracedPathActor> tracedSegments =
    		new ConcurrentHashMap<SegmentIndex, TracedPathActor>();

    // note: this initial color is now overridden by other components
    private float neuronColor[] = {0.8f,1.0f,0.3f};
    private final float blackColor[] = {0,0,0};
    // private TracedPathShader tracedShader = new TracedPathShader();
    private boolean anchorsVisible = true;
	
	public Signal skeletonActorChangedSignal = new Signal();

    public Signal1<Anchor> nextParentChangedSignal = new Signal1<Anchor>();
	
	private Slot updateAnchorsSlot = new Slot() {
		@Override
		public void execute() {updateAnchors();}
	};

    public Slot1<Long> setNextParentSlot = new Slot1<Long>() {
        @Override
        public void execute(Long annotationID) {
            setNextParentByID(annotationID);
        }
    };

    public Slot1<Color> changeGlobalColorSlot = new Slot1<Color>() {
        @Override
        public void execute(Color color) {
            neuronColor[0] = color.getRed() / 255.0f;
            neuronColor[1] = color.getGreen() / 255.0f;
            neuronColor[2] = color.getBlue() / 255.0f;
            // skeletonActorChangedSignal.emit();
            updateAnchorsSlot.execute();
        }
    };

	private TileFormat tileFormat;

	public SkeletonActor() {
		// log.info("New SkeletonActor");
	}
	
	private synchronized void displayLines(GLAutoDrawable glDrawable) {
		// Line segments using vertex buffer objects
		if (lineIndices == null)
			return;
		if (lineIndices.capacity() < 2)
			return;

        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        GL2 gl2 = gl.getGL2();
		// if (verticesNeedCopy) { // TODO
        if (true) {
            gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
        		vertices.rewind();
	        gl.glBufferData(GL.GL_ARRAY_BUFFER, 
	        		vertexCount * floatByteCount * vertexFloatCount, 
	        		vertices, GL.GL_DYNAMIC_DRAW);
        		verticesNeedCopy = false;

    		// colors
	        colors.rewind();
	        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorBo);
	        gl.glBufferData(GL.GL_ARRAY_BUFFER, 
	        		vertexCount * floatByteCount * colorFloatCount,
	        		colors, 
	        		GL.GL_DYNAMIC_DRAW);
        }
		// if (linesNeedCopy) // TODO
		if (true) 
		{
	        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, lineIbo);
			lineIndices.rewind();
	        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, 
	        		lineIndices.capacity() * intByteCount,
	        		lineIndices, GL.GL_DYNAMIC_DRAW);
	        linesNeedCopy = false;
		}
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vbo );
        gl2.glVertexPointer(vertexFloatCount, GL2.GL_FLOAT, 0, 0L);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
        gl2.glColorPointer(colorFloatCount, GL2.GL_FLOAT, 0, 0L);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, lineIbo);
		lineShader.load(gl2);
 		lineShader.setUniform(gl, "zThickness", zThicknessInPixels);
 		// log.info("zThickness = "+zThickness);
 		float focus[] = {
 			(float)camera.getFocus().getX(),
 			(float)camera.getFocus().getY(),
 			(float)camera.getFocus().getZ()};
 		lineShader.setUniform3v(gl, "focus", 1, focus);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        // wider black line
		gl.glLineWidth(3.5f);
		lineShader.setUniform3v(gl, "baseColor", 1, blackColor);
        // gl.glColor4f(0, 0, 0, 0.7f); // black
        gl.glDrawElements(GL2.GL_LINES, 
        		lineIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        // narrower white line
		gl.glLineWidth(1.5f);
		lineShader.setUniform3v(gl, "baseColor", 1, neuronColor);
        // gl.glColor4f(1, 1, 1, 0.7f); // white
        gl.glDrawElements(GL2.GL_LINES, 
        		lineIndices.capacity(), 
        		GL2.GL_UNSIGNED_INT, 
        		0L);
        //
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);	
	    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, 0 );
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        lineShader.unload(gl2);
	}
	
	private synchronized void displayAnchors(GLAutoDrawable glDrawable) {
		// Paint anchors as point sprites
		if (pointIndices == null)
			return;
		if (pointIndices.capacity() < 1)
			return;

        GL2 gl = glDrawable.getGL().getGL2();
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
 		int parentIndex = getIndexForAnchor(nextParent);
 		anchorShader.setUniform(gl, "parentAnchorIndex", parentIndex);
 		// float zThickness = viewport.getDepth(); //  / (float)camera.getPixelsPerSceneUnit();
 		// At high zoom, keep thickness to at least 5 pixels deep.
 		anchorShader.setUniform(gl, "zThickness", zThicknessInPixels);
 		float focus[] = {
 	 			(float)camera.getFocus().getX(),
 	 			(float)camera.getFocus().getY(),
 	 			(float)camera.getFocus().getZ()};
 	 	anchorShader.setUniform3v(gl, "focus", 1, focus);
 		// anchorShader.setUniform(gl, "focusZ", (float)camera.getFocus().getZ());
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
	public void display(GLAutoDrawable glDrawable) {
	    if (! bIsVisible)
	        return;
		if (vertexCount <= 0)
			return;
		if ( ! bIsGlInitialized )
			init(glDrawable);

		// System.out.println("painting skeleton");
		displayLines(glDrawable);

		displayTracedSegments(glDrawable);
		
		if (isAnchorsVisible())
			displayAnchors(glDrawable);
	}

	private void displayTracedSegments(GLAutoDrawable glDrawable) {
		GL gl = glDrawable.getGL();
		GL2 gl2 = gl.getGL2();
		GL2GL3 gl2gl3 = gl.getGL2GL3();
		// log.info("Displaying "+tracedSegments.size()+" traced segments");
		lineShader.load(gl2);
 		float zt = zThicknessInPixels;
 		float zoomLimit = 5.0f;
 		if (camera.getPixelsPerSceneUnit() > zoomLimit) {
 			zt = zThicknessInPixels * (float)camera.getPixelsPerSceneUnit() / zoomLimit;
 		}
 		lineShader.setUniform(gl2gl3, "zThickness", zt);
 		// log.info("zThickness = "+zThickness);
 		float focus[] = {
 			(float)camera.getFocus().getX(),
 			(float)camera.getFocus().getY(),
 			(float)camera.getFocus().getZ()};
 		lineShader.setUniform3v(gl2gl3, "focus", 1, focus);
		// black background
        gl.glLineWidth(5.0f);
		lineShader.setUniform3v(gl2gl3, "baseColor", 1, blackColor);
		for (TracedPathActor segment : tracedSegments.values())
		    segment.display(glDrawable);
		// neuron colored foreground
        gl.glLineWidth(3.0f);
		lineShader.setUniform3v(gl2gl3, "baseColor", 1, neuronColor);
        for (TracedPathActor segment : tracedSegments.values())
            segment.display(glDrawable);
		lineShader.unload(gl2);
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
	
	public float getZThicknessInPixels() {
		return zThicknessInPixels;
	}

	public boolean isAnchorsVisible() {
		return anchorsVisible;
	}

	public void setAnchorsVisible(boolean anchorsVisible) {
		if (anchorsVisible == this.anchorsVisible)
			return; // no change
		this.anchorsVisible = anchorsVisible;
		skeletonActorChangedSignal.emit();
	}

	public void setZThicknessInPixels(float zThicknessInPixels) {
		this.zThicknessInPixels = zThicknessInPixels;
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
		int pointCount = 0;
		// Populate vertex array
		for (Anchor anchor : skeleton.getAnchors()) {
			Vec3 xyz = anchor.getLocation();
			vertices.put((float)xyz.getX());
			vertices.put((float)xyz.getY());
			vertices.put((float)xyz.getZ());
			//
			colors.put(neuronColor[0]); // red
			colors.put(neuronColor[1]); // green
			colors.put(neuronColor[2]); // blue
			//
			anchorIndices.put(anchor, vertexIndex);
			indexAnchors.put(vertexIndex, anchor);
			vertexIndex += 1;
			pointCount += 1;
		}
		//
		// Update Traced path actors
		Set<SegmentIndex> foundSegments = new HashSet<SegmentIndex>();
		Collection<AnchoredVoxelPath> skeletonSegments = skeleton.getTracedSegments();
		// log.info("Skeleton has " + skeletonSegments.size() + " traced segments");
		for (AnchoredVoxelPath segment : skeletonSegments) {
			SegmentIndex ix = segment.getSegmentIndex();
			foundSegments.add(ix);
			if (tracedSegments.containsKey(ix)) {
				// Is the old traced segment still valid?
				AnchoredVoxelPath oldSegment = tracedSegments.get(ix).getSegment();
				List<ZoomedVoxelIndex> p0 = oldSegment.getPath();
				List<ZoomedVoxelIndex> p1 = segment.getPath();
				boolean looksTheSame = true;
				if (p0.size() != p1.size()) // same size?
					looksTheSame = false;
				else if (p0.get(0) != p1.get(0)) // same first voxel?
					looksTheSame = false;
				else if (p0.get(p0.size()-1) != p1.get(p1.size()-1)) // same final voxel?
					looksTheSame = false;
				if (looksTheSame)
					continue; // already have this segment, no need to recompute!
				else
					tracedSegments.remove(ix); // obsolete. remove it.
			}
			TracedPathActor actor = new TracedPathActor(segment, getTileFormat());
			addTracedSegment(actor);
		}
    	// log.info("tracedSegments.size() [485] = "+tracedSegments.size());
		// Delete obsolete traced segments
    	// COPY the keyset, to avoid damaging the original tracedSegment keys.
		Set<SegmentIndex> orphanSegments = new HashSet<SegmentIndex>(tracedSegments.keySet());
		orphanSegments.removeAll(foundSegments);
		for (SegmentIndex ix : orphanSegments) {
			log.info("Removing orphan segment");
			tracedSegments.remove(ix);
		}
    	// log.info("tracedSegments.size() [492] = "+tracedSegments.size());
		//
		// Populate line index buffer - AFTER traced segments have been finalized
		List<Integer> tempLineIndices = new Vector<Integer>(); // because we don't know size yet
		for (Anchor anchor : skeleton.getAnchors()) {
			int i1 = getIndexForAnchor(anchor);
			if (i1 < 0)
				continue;
			for (Anchor neighbor : anchor.getNeighbors()) {
				int i2 = getIndexForAnchor(neighbor);
				if (i2 < 0)
					continue;
				if (i1 >= i2)
					continue; // only use ascending pairs, for uniqueness
				SegmentIndex segmentIndex = new SegmentIndex(anchor.getGuid(), neighbor.getGuid());
				// Don't draw lines where there is already a traced segment.
				if (tracedSegments.containsKey(segmentIndex))
					continue;
				tempLineIndices.add(i1);
				tempLineIndices.add(i2);
			}
		}
		ByteBuffer lineBytes = ByteBuffer.allocateDirect(tempLineIndices.size()*intByteCount);
		lineBytes.order(ByteOrder.nativeOrder());
		lineIndices = lineBytes.asIntBuffer();
		lineIndices.rewind();
		for (int i : tempLineIndices) // fill actual int buffer
			lineIndices.put(i);
		lineIndices.rewind();
		linesNeedCopy = true;
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
	
	public void setTileFormat(TileFormat tileFormat) {
		this.tileFormat = tileFormat;

        // propagate to all traced path actors, too:
        for (TracedPathActor path : tracedSegments.values()) {
            path.setTileFormat(tileFormat);
        }

	}
	
	private TileFormat getTileFormat() {
		return tileFormat;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// Required for gl_VertexID to be found in shader
		// System.out.println("init");
		// compile shader
        GL2 gl = glDrawable.getGL().getGL2();
        PassThroughTextureShader.checkGlError(gl, "load anchor texture 000");
		try {
			lineShader.init(gl);
			anchorShader.init(gl);
			// tracedShader.init(gl);
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

        // Create a buffer object for line indices
        //
        int ix[] = {0, 0, 0, 0};
        gl.glGenBuffers( 4, ix, 0 );
        vbo = ix[0];
        lineIbo = ix[1];
        pointIbo = ix[2];
        colorBo = ix[3];
        //
		PassThroughTextureShader.checkGlError(gl, "load anchor texture");
		linesNeedCopy = true;
		verticesNeedCopy = true;
		
		// Apply transparency, even when anchors are not shown
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
        bIsGlInitialized = true;
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		// System.out.println("dispose skeleton actor");
		bIsGlInitialized = false;
		int ix1[] = {anchorTextureId, parentAnchorTextureId};
        GL2 gl = glDrawable.getGL().getGL2();
		gl.glDeleteTextures(2, ix1, 0);
		int ix2[] = {vbo, lineIbo, pointIbo, colorBo};
		gl.glDeleteBuffers(4, ix2, 0);
		for (TracedPathActor path : tracedSegments.values())
			path.dispose(glDrawable);
		tracedSegments.clear();
    	// log.info("tracedSegments.size() [629] = "+tracedSegments.size());
	}

	public synchronized void setHoverAnchorIndex(int ix) {
		if (ix == hoverAnchorIndex) 
			return;
		hoverAnchorIndex = ix;
		skeletonActorChangedSignal.emit(); // TODO leads to instability?
	}

	public Anchor getNextParent() {
		return nextParent;
	}

    public boolean setNextParentByID(Long annotationID) {
        // find the anchor corresponding to this annotation ID and pass along
        Anchor foundAnchor = null;
        for (Anchor testAnchor: getSkeleton().getAnchors()) {
            if (testAnchor.getGuid().equals(annotationID)) {
                foundAnchor = testAnchor;
                break;
            }
        }

        // it's OK if we set a null (it's a deselect)
        return setNextParent(foundAnchor);
    }

	public boolean setNextParent(Anchor parent) {
		if (parent == nextParent)
			return false;
		nextParent = parent;
        // first signal is for drawing the marker, second is for notifying
        //  components that want to, eg, select the enclosing neuron
		skeletonActorChangedSignal.emit();
        nextParentChangedSignal.emit(nextParent);
		return true;
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
	
	public void lightweightPlaceAnchor(Anchor dragAnchor, Vec3 location) {
		if (dragAnchor == null)
			return;
		int index = getIndexForAnchor(dragAnchor);
		if (index < 0)
			return;
		int offset = index * vertexFloatCount;
		for (int i = 0; i < 3; ++i) {
			vertices.put( offset+i, (float)(double)location.get(i) );
		}
		skeletonActorChangedSignal.emit();
	}

	public boolean isVisible() {
	    return bIsVisible;
	}
    public void setVisible(boolean b) {
        if (bIsVisible == b)
            return;
        bIsVisible = b;
        skeletonActorChangedSignal.emit();
    }

    public void addTracedSegment(TracedPathActor actor) {
    	// log.info("tracedSegments.size() [691] = "+tracedSegments.size());
    	// log.info("Adding traced segment to SkeletonActor");
        tracedSegments.put(actor.getSegmentIndex(), actor);
    	// log.info("tracedSegments.size() [694] = "+tracedSegments.size());
        skeletonActorChangedSignal.emit();
    }
}
