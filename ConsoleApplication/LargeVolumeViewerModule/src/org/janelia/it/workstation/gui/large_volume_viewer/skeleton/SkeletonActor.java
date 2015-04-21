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
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.ImageIcon;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.UpdateAnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.AnchorShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.PassThroughTextureShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.PathShader;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.workstation.tracing.VoxelPosition;
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
	private static final int FLOAT_BYTE_COUNT = 4;
	private static final int VERTEX_FLOAT_COUNT = 3;
	private static final int INT_BYTE_COUNT = 4;
	private static final int COLOR_FLOAT_COUNT = 3;

	private boolean bIsGlInitialized = false;
	
    // arrays for draw
    private Multiset<Long> neuronVertexCount = HashMultiset.create();
    private Map<Long, FloatBuffer> neuronVertices = new HashMap<>();
    private Map<Long, FloatBuffer> neuronColors = new HashMap<>();

	private int vbo = -1;
	private int lineIbo = -1;
	private int pointIbo = -1;
	private int colorBo = -1;

    // not currently used:
	private boolean linesNeedCopy = false;
	private boolean verticesNeedCopy = false;

	// Vertex buffer objects need indices
    private Map<Anchor, Integer> neuronAnchorIndices = new HashMap<>();
    private Map<Long, Map<Integer, Anchor>> neuronIndexAnchors = new HashMap<>();
    private Map<Long, IntBuffer> neuronPointIndices = new HashMap<>();
    private Map<Long, IntBuffer> neuronLineIndices = new HashMap<>();

	private PathShader lineShader = new PathShader();
	private AnchorShader anchorShader = new AnchorShader();
	private BoundingBox3d bb = new BoundingBox3d();
	//
	private BufferedImage anchorImage;
	private int anchorTextureId = -1;
	private BufferedImage parentAnchorImage;
    private int discardNonParent = 0; //Emphasize default value.
    private int parentAnchorTextureId = -1;
	//
	private Skeleton skeleton;
    private SkeletonActorStateUpdater updater;
	private Camera3d camera;
	private float zThicknessInPixels = 100;
	//
    private boolean bIsVisible = true;
    
    private Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments = new HashMap<>();

    private NeuronStyleModel neuronStyles;

    // note: this initial color is now overridden by other components
    private float neuronColor[] = {0.8f,1.0f,0.3f};
    private final float blackColor[] = {0,0,0};
    private boolean anchorsVisible = true;
	
	private TileFormat tileFormat;
    private RenderInterpositionMethod rim = RenderInterpositionMethod.MIP;
    
    public enum RenderInterpositionMethod {
        MIP, Occlusion
    }

	public SkeletonActor() {
        updater = new SkeletonActorStateUpdater();
		// log.info("New SkeletonActor");
	}
    
    public void setRenderInterpositionMethod( RenderInterpositionMethod rim ) {
        this.rim = rim;
    }

    public void clearStyles() {
        neuronStyles.clear();
    }
    
    public void setNeuronStyleModel( NeuronStyleModel nsModel ) {
        this.neuronStyles = nsModel;
    }

    public SkeletonActorStateUpdater getUpdater() {
        return updater;
    }
    
	private synchronized void displayLines(GLAutoDrawable glDrawable) {
        if (neuronLineIndices.size() == 0)
            return;

        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        GL2 gl2 = gl.getGL2();

        NeuronStyle style;
        for (Long neuronID: neuronVertices.keySet()) {
            if (neuronStyles.get(neuronID) != null  &&  !neuronStyles.get(neuronID).isVisible()) {
                continue;
            }

            if (!neuronLineIndices.containsKey(neuronID))
                continue;

            // if (verticesNeedCopy) { // TODO
            if (true) {
                gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
                neuronVertices.get(neuronID).rewind();
                gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT,
                        neuronVertices.get(neuronID), GL.GL_DYNAMIC_DRAW);
                verticesNeedCopy = false;

                // uses same color array as vertices (anchors) does
                neuronColors.get(neuronID).rewind();
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorBo);
                gl.glBufferData(GL.GL_ARRAY_BUFFER,
                        neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT,
                        neuronColors.get(neuronID),
                        GL.GL_DYNAMIC_DRAW);
            }
            // if (linesNeedCopy) // TODO
            if (true) {
                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, lineIbo);
                neuronLineIndices.get(neuronID).rewind();
                gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,
                        neuronLineIndices.get(neuronID).capacity() * INT_BYTE_COUNT,
                        neuronLineIndices.get(neuronID), GL.GL_DYNAMIC_DRAW);
                linesNeedCopy = false;
            }
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, vbo );
            gl2.glVertexPointer(VERTEX_FLOAT_COUNT, GL2.GL_FLOAT, 0, 0L);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
            gl2.glColorPointer(COLOR_FLOAT_COUNT, GL2.GL_FLOAT, 0, 0L);
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
            gl.glDrawElements(GL2.GL_LINES,
                    neuronLineIndices.get(neuronID).capacity(),
                    GL2.GL_UNSIGNED_INT,
                    0L);
            // narrower colored line
            gl.glLineWidth(1.5f);
            if (neuronStyles.containsKey(neuronID)) {
                style = neuronStyles.get(neuronID);
            } else {
                style = NeuronStyle.getStyleForNeuron(neuronID);
            }
            lineShader.setUniform3v(gl, "baseColor", 1, style.getColorAsFloatArray());
            gl.glDrawElements(GL2.GL_LINES,
                    neuronLineIndices.get(neuronID).capacity(),
                    GL2.GL_UNSIGNED_INT,
                    0L);
        }

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);	
	    gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, 0 );
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        lineShader.unload(gl2);
	}

	private synchronized void displayAnchors(GLAutoDrawable glDrawable) {
		// Paint anchors as point sprites
		if (neuronPointIndices == null)
			return;
		if (neuronPointIndices.size() < 1)
			return;

        GL2 gl = glDrawable.getGL().getGL2();
        setupAnchorShaders(gl);

        for (Long neuronID: neuronVertices.keySet()) {
            if (neuronStyles.get(neuronID) != null  &&  !neuronStyles.get(neuronID).isVisible()) {
                continue;
            }

            // setup per-neuron anchor shader settings (used to be in setupAnchorShader)
            int tempIndex;
            Anchor hoverAnchor = skeleton.getHoverAnchor();
            if (hoverAnchor != null && hoverAnchor.getNeuronID().equals(neuronID)) {
                tempIndex = getIndexForAnchor(hoverAnchor);
            } else {
                tempIndex = -1;
            }
            anchorShader.setUniform(gl, "highlightAnchorIndex", tempIndex);
            Anchor nextParent = skeleton.getNextParent();
            if (nextParent != null && nextParent.getNeuronID().equals(neuronID)) {
                tempIndex = getIndexForAnchor(nextParent);
            } else {
                tempIndex = -1;
            }
            anchorShader.setUniform(gl, "parentAnchorIndex", tempIndex);
            anchorShader.setUniform(gl, "isDiscardNonParent", discardNonParent);

            // TODO - crashes unless glBufferData called every time.
            // if (verticesNeedCopy) {
            if (true) {
                // vertices
                neuronVertices.get(neuronID).rewind();
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
                gl.glBufferData(GL2.GL_ARRAY_BUFFER,
                        neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT,
                        neuronVertices.get(neuronID), GL2.GL_DYNAMIC_DRAW);

                // colors
                neuronColors.get(neuronID).rewind();
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
                gl.glBufferData(GL2.GL_ARRAY_BUFFER,
                        neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT,
                        neuronColors.get(neuronID),
                        GL2.GL_DYNAMIC_DRAW);

                verticesNeedCopy = false;
                // point indices
                neuronPointIndices.get(neuronID).rewind();
                gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
                gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER,
                        neuronPointIndices.get(neuronID).capacity() * INT_BYTE_COUNT,
                        neuronPointIndices.get(neuronID), GL2.GL_DYNAMIC_DRAW);
            }
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
            gl.glVertexPointer(VERTEX_FLOAT_COUNT, GL2.GL_FLOAT, 0, 0L);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
            gl.glColorPointer(COLOR_FLOAT_COUNT, GL2.GL_FLOAT, 0, 0L);
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
            PassThroughTextureShader.checkGlError(gl, "paint anchors 1");
            gl.glDrawElements(GL2.GL_POINTS,
                    neuronPointIndices.get(neuronID).capacity(),
                    GL2.GL_UNSIGNED_INT,
                    0L);
            // tear down
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        }

        tearDownAnchorShaders(gl);
	}

    private void setupAnchorShaders(GL2 gl) {
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
        if (rim == RenderInterpositionMethod.MIP) {
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else {
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LESS);
        }
        anchorShader.load(gl);

        // used to set uniforms for hover index and parent index here, but
        //  that's now done in the appropriate update loops

        // At high zoom, keep thickness to at least 5 pixels deep.
        anchorShader.setUniform(gl, "zThickness", zThicknessInPixels);
        float focus[] = {
                 (float)camera.getFocus().getX(),
                 (float)camera.getFocus().getY(),
                 (float)camera.getFocus().getZ()};
        anchorShader.setUniform3v(gl, "focus", 1, focus);
        anchorShader.setUniform(gl, "anchorTexture", 0);
        anchorShader.setUniform(gl, "parentAnchorTexture", 1);
    }

    private void tearDownAnchorShaders(GL2 gl) {
        anchorShader.unload(gl);
        gl.glDisable(GL2.GL_POINT_SPRITE);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        if (rim == RenderInterpositionMethod.MIP) {
            gl.glDisable(GL2.GL_BLEND);
        }
        else {
            gl.glDisable(GL2.GL_DEPTH_TEST);
        }
    }

    @Override
	public void display(GLAutoDrawable glDrawable) {
	    if (! bIsVisible)
	        return;
		if (neuronVertexCount.size() <= 0)
			return;
		if ( ! bIsGlInitialized )
			init(glDrawable);


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

        for (Long neuronID: neuronTracedSegments.keySet()) {
            if (neuronStyles.get(neuronID) != null  &&  !neuronStyles.get(neuronID).isVisible()) {
                continue;
            }

            lineShader.setUniform3v(gl2gl3, "baseColor", 1, blackColor);
            for (TracedPathActor segment : neuronTracedSegments.get(neuronID).values())
                segment.display(glDrawable);
            gl.glLineWidth(3.0f);
            NeuronStyle style;
            if (neuronStyles.containsKey(neuronID)) {
                style = neuronStyles.get(neuronID);
            } else {
                style = NeuronStyle.getStyleForNeuron(neuronID);
            }
            for (TracedPathActor segment : neuronTracedSegments.get(neuronID).values()) {
                // neuron colored foreground
                lineShader.setUniform3v(gl2gl3, "baseColor", 1, style.getColorAsFloatArray());
                segment.display(glDrawable);
            }
        }
        lineShader.unload(gl2);
	}

	public int getIndexForAnchor(Anchor anchor) {
		if (anchor == null)
			return -1;
		if (neuronAnchorIndices.containsKey(anchor))
			return neuronAnchorIndices.get(anchor);
		return -1;
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
		this.skeleton = skeleton;
		updateAnchors();
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
        updater.update();
	}
    
    public void setShowOnlyParentAnchors(boolean showOnlyParent) {
        this.discardNonParent = showOnlyParent ? 1 : 0;
    }

	public void setZThicknessInPixels(float zThicknessInPixels) {
		this.zThicknessInPixels = zThicknessInPixels;
	}

    public void changeNeuronColor(Color color) {
        neuronColor[0] = color.getRed() / 255.0f;
        neuronColor[1] = color.getGreen() / 255.0f;
        neuronColor[2] = color.getBlue() / 255.0f;
        // skeletonActorChangedSignal.emit();
        updateAnchors();
    }

    public void changeNeuronStyle(TmNeuron neuron, NeuronStyle style) {
        if (neuron != null) {
            neuronStyles.put(neuron.getId(), style);
            updateAnchors();
        }
    }

    /**
     * update the arrays we'll send to OpenGL; this includes the
     * anchors/points (thus the name of the method), the lines
     * between them, and the automatically traced paths if present
     *
     * the update in general consists of looping over all points,
     * and copying their positions into appropriate byte array,
     * getting the data ready for the next call to display()
     */
	public synchronized void updateAnchors() {
		if (skeleton == null)
			return;

        // we do the point update in this method, then call out
        //  to other methods for the lines and paths; no reason we
        //  couldn't also refactor this into its own method, too

        // clear out the maps first
        neuronVertexCount.clear();
        neuronVertices.clear();
        neuronColors.clear();
        // first, how many vertices per neuron; then, fill the buffers (one per neuron)
        for (Anchor anchor: skeleton.getAnchors()) {
            neuronVertexCount.add(anchor.getNeuronID());
        }
        for (Long neuronID: neuronVertexCount.elementSet()) {
            ByteBuffer tempBytes = ByteBuffer.allocateDirect(neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * VERTEX_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronVertices.put(neuronID, tempBytes.asFloatBuffer());
            neuronVertices.get(neuronID).rewind();

            tempBytes = ByteBuffer.allocateDirect(neuronVertexCount.count(neuronID) * FLOAT_BYTE_COUNT * COLOR_FLOAT_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronColors.put(neuronID, tempBytes.asFloatBuffer());
            neuronColors.get(neuronID).rewind();
        }

        neuronAnchorIndices.clear();
        neuronIndexAnchors.clear();
        Map<Long, Integer> neuronVertexIndex = new HashMap<>();
        int currentVertexIndex;
        NeuronStyle style;
        for (Anchor anchor: skeleton.getAnchors()) {
            Long neuronID = anchor.getNeuronID();
            Vec3 xyz = anchor.getLocation();
            neuronVertices.get(neuronID).put((float) xyz.getX());
            neuronVertices.get(neuronID).put((float) xyz.getY());
            neuronVertices.get(neuronID).put((float) xyz.getZ());
            if (neuronStyles.containsKey(neuronID)) {
                style = neuronStyles.get(neuronID);
            } else {
                style = NeuronStyle.getStyleForNeuron(neuronID);
            }
            neuronColors.get(neuronID).put(style.getRedAsFloat());
            neuronColors.get(neuronID).put(style.getGreenAsFloat());
            neuronColors.get(neuronID).put(style.getBlueAsFloat());

            if (neuronVertexIndex.containsKey(neuronID)) {
                currentVertexIndex = neuronVertexIndex.get(neuronID);
            } else {
                currentVertexIndex = 0;
                neuronVertexIndex.put(neuronID, currentVertexIndex);
            }
            neuronAnchorIndices.put(anchor, currentVertexIndex);

            if (!neuronIndexAnchors.containsKey(neuronID)) {
                neuronIndexAnchors.put(neuronID, new HashMap<Integer, Anchor>());
            }
            neuronIndexAnchors.get(neuronID).put(currentVertexIndex, anchor);

            neuronVertexIndex.put(neuronID, currentVertexIndex + 1);
        }

        neuronPointIndices.clear();
        for (Long neuronID: neuronVertexIndex.keySet()) {
            // recall that the last value neuronVertexIndex takes is the
            //  number of points:
            ByteBuffer tempBytes = ByteBuffer.allocateDirect(neuronVertexIndex.get(neuronID) * INT_BYTE_COUNT);
            tempBytes.order(ByteOrder.nativeOrder());
            neuronPointIndices.put(neuronID, tempBytes.asIntBuffer());
            neuronPointIndices.get(neuronID).rewind();
        }
        for (Anchor anchor: skeleton.getAnchors()) {
            int i1 = neuronAnchorIndices.get(anchor);
            neuronPointIndices.get(anchor.getNeuronID()).put(i1);
        }


		// automatically traced paths
        updateTracedPaths();

        // lines between points, if no path (must be done after path updates so
        //  we know where the paths are!)
        updateLines();

        if (!skeleton.getAnchors().contains(getNextParent())) {
            setNextParent(null);
        }

        updater.update();
	}

    private void updateLines() {
        // iterate through anchors and record lines where there are no traced
        //  paths; then copy the line indices you get into an array
        // note: I believe this works because we process the points and
        //  lines in exactly the same order (the order skeleton.getAnchors()
        //  returns them in)

        Map<Long, List<Integer>> tempLineIndices = new HashMap<>();
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
                // if neuron has any paths, check and don't draw line
                //  where there's already a traced segment
                if (neuronTracedSegments.containsKey(anchor.getNeuronID()) &&
                    neuronTracedSegments.get(anchor.getNeuronID()).containsKey(segmentIndex)) {
                    continue;
                }
                if (!tempLineIndices.containsKey(anchor.getNeuronID())) {
                    tempLineIndices.put(anchor.getNeuronID(), new Vector<Integer>());
                }
                tempLineIndices.get(anchor.getNeuronID()).add(i1);
                tempLineIndices.get(anchor.getNeuronID()).add(i2);
            }
        }

        // loop over neurons and fill the arrays
        neuronLineIndices.clear();
        for (Long neuronID: tempLineIndices.keySet()) {
            ByteBuffer lineBytes = ByteBuffer.allocateDirect(tempLineIndices.get(neuronID).size() * INT_BYTE_COUNT);
            lineBytes.order(ByteOrder.nativeOrder());
            neuronLineIndices.put(neuronID, lineBytes.asIntBuffer());
            neuronLineIndices.get(neuronID).rewind();
            for (int i : tempLineIndices.get(neuronID)) // fill actual int buffer
                neuronLineIndices.get(neuronID).put(i);
            neuronLineIndices.get(neuronID).rewind();
        }

        linesNeedCopy = true;
        verticesNeedCopy = true;
    }

    private void updateTracedPaths() {
        // Update Traced path actors

        // first, a short-circuit; if there are no anchors, the whole
        //  skeleton was cleared, and we can clear our traced segments as well;
        //  this is necessary because unlike in the old not-per-neuron way of
        //  doing things, we would normally need some info from anchors that
        //  just isn't there when the whole skeleton is cleared
        if (skeleton.getAnchors().size() == 0) {
            neuronTracedSegments.clear();
            return;
        }

        Set<SegmentIndex> foundSegments = new HashSet<>();
        Collection<AnchoredVoxelPath> skeletonSegments = skeleton.getTracedSegments();
        // log.info("Skeleton has " + skeletonSegments.size() + " traced segments");
        for (AnchoredVoxelPath segment : skeletonSegments) {
            SegmentIndex ix = segment.getSegmentIndex();

            // need neuron ID; get it from the anchor at either end of the
            //  traced path; if there isn't an anchor, just move on--that
            //  path is also gone (happens when neurons deleted, merged)
            Anchor pathAnchor = skeleton.getAnchorByID(ix.getAnchor1Guid());
            if (pathAnchor == null) {
                continue;
            }
            Long neuronID = pathAnchor.getNeuronID();

            foundSegments.add(ix);
            if (neuronTracedSegments.containsKey(neuronID)) {
                if (neuronTracedSegments.get(neuronID).containsKey(ix)) {
                    // Is the old traced segment still valid?
                    AnchoredVoxelPath oldSegment = neuronTracedSegments.get(neuronID).get(ix).getSegment();
                    List<VoxelPosition> p0 = oldSegment.getPath();
                    List<VoxelPosition> p1 = segment.getPath();
                    boolean looksTheSame = true;
                    if (p0.size() != p1.size()) // same size?
                        looksTheSame = false;
                    else if (p0.get(0) != p1.get(0)) // same first voxel?
                        looksTheSame = false;
                    else if (p0.get(p0.size()-1) != p1.get(p1.size()-1)) // same final voxel?
                        looksTheSame = false;
                    if (looksTheSame) {
                        continue; // already have this segment, no need to recompute!
                    } else {
                        neuronTracedSegments.get(neuronID).remove(ix);
                    }
                }
            } else {
                // haven't seen this neuron yet
                neuronTracedSegments.put(neuronID, new ConcurrentHashMap<SegmentIndex, TracedPathActor>());
            }
            TracedPathActor actor = new TracedPathActor(segment, getTileFormat());

            neuronTracedSegments.get(neuronID).put(actor.getSegmentIndex(), actor);

            // not sure why this is in the loop instead of out of it!
            //  all it does is trigger a repaint; I suppose it's better to
            //  paint after every path added, so they pop in as they are
            //  ready; paint can't be that expensive, can it?
            updater.update();
        }

        // carefully iterate over segments and prune the obsolete ones
        for (Long neuronID: neuronTracedSegments.keySet()) {
            Set<SegmentIndex> neuronSegmentIndices = new HashSet<>(neuronTracedSegments.get(neuronID).keySet());
            for (SegmentIndex ix: neuronSegmentIndices) {
                if (!foundSegments.contains(ix) ) {
                    log.info("Removing orphan segment");
                    neuronTracedSegments.get(neuronID).remove(ix);
                }
            }
        }
    }

    public void setTileFormat(TileFormat tileFormat) {
		this.tileFormat = tileFormat;

        // propagate to all traced path actors, too:
        for (Long neuronID: neuronTracedSegments.keySet()) {
            for (TracedPathActor path: neuronTracedSegments.get(neuronID).values()) {
                path.setTileFormat(tileFormat);
            }
        }
	}
	
	private TileFormat getTileFormat() {
		return tileFormat;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
		// Required for gl_VertexID to be found in shader
		// compile shader
        GL2 gl = glDrawable.getGL().getGL2();
        PassThroughTextureShader.checkGlError(gl, "load anchor texture 000");
		try {
			lineShader.init(gl);
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
		
        if (rim == RenderInterpositionMethod.MIP) {
    		// Apply transparency, even when anchors are not shown
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else {
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LEQUAL);
        }
		
        bIsGlInitialized = true;
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
		bIsGlInitialized = false;
		int ix1[] = {anchorTextureId, parentAnchorTextureId};
        GL2 gl = glDrawable.getGL().getGL2();
		gl.glDeleteTextures(2, ix1, 0);
		int ix2[] = {vbo, lineIbo, pointIbo, colorBo};
		gl.glDeleteBuffers(4, ix2, 0);

        for (Long neuronID: neuronTracedSegments.keySet()) {
            for (TracedPathActor path: neuronTracedSegments.get(neuronID).values()) {
                path.dispose(glDrawable);
            }
        }
        updater.update();
	}

    public synchronized  void setHoverAnchor(Anchor anchor) {
        if (anchor == skeleton.getHoverAnchor()) {
            return;
        }
        skeleton.setHoverAnchor(anchor);
        updater.update();
    }

	public Anchor getNextParent() {
		return skeleton.getNextParent();
	}

    public boolean setNextParentByID(Long annotationID) {
        // find the anchor corresponding to this annotation ID and pass along
        Anchor foundAnchor = null;
        if (getSkeleton() == null) {
            return false;
        }
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
		if (parent == skeleton.getNextParent())
			return false;
		skeleton.setNextParent(parent);
        // first signal is for drawing the marker, second is for notifying
        //  components that want to, eg, select the enclosing neuron
        updater.update();
        updater.update(skeleton.getNextParent());
		return true;
	}

    public void addAnchorUpdateListener(UpdateAnchorListener l) {
        getUpdater().addListener(l);
    }
    
	/*
	 * Change visual anchor position without actually changing the Skeleton model
	 */
	public void lightweightPlaceAnchor(Anchor dragAnchor, Vec3 location) {
		if (dragAnchor == null)
			return;
		int index = getIndexForAnchor(dragAnchor);
		if (index < 0)
			return;
		int offset = index * VERTEX_FLOAT_COUNT;
		for (int i = 0; i < 3; ++i) {
            neuronVertices.get(dragAnchor.getNeuronID()).put(offset+i, (float)(double)location.get(i) );
		}
        updater.update();
	}

    /**
     * is anything visible (ie, to be drawn)?
     */
	public boolean isVisible() {
	    return bIsVisible;
	}
    public void setVisible(boolean b) {
        if (bIsVisible == b)
            return;
        bIsVisible = b;
        updater.update();
    }

    /**
     * is the input anchor's neuron visible?
     */
    public boolean anchorIsVisible(Anchor anchor) {
        if (anchor == null || anchor.getNeuronID() == null || !neuronStyles.containsKey(anchor.getNeuronID())) {
            return false;
        } else {
            return neuronStyles.get(anchor.getNeuronID()).isVisible();
        }
    }
}
