package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
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
 * SkeletonActor is responsible for painting neuron traces in the large volume
 * viewer.
 *
 * @author brunsc
 *
 */
public class SkeletonActor
        implements GLActor {

    private static final String LARGE_PARENT_IMG = "white-ball-icone-6188-32.png";
    private static final String SMALL_PARENT_IMG = "ParentAnchor16.png";
    
    public enum ParentAnchorImage {
        SMALL {
            public String toString() {
                return SMALL_PARENT_IMG;
            }
        },
        LARGE {
            public String toString() {
                return LARGE_PARENT_IMG;
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SkeletonActor.class);

    private boolean bIsGlInitialized = false;

    private int vbo = -1;
    private int lineIbo = -1;
    private int pointIbo = -1;
    private int colorBo = -1;

    private SkeletonActorModel model=new SkeletonActorModel();

    private int skeletonActorModelVersion=0;
    public int getSkeletonActorModelVersion() { return skeletonActorModelVersion; }
    public void setSkeletonActorModelVersion(int skeletonActorModelVersion) { this.skeletonActorModelVersion=skeletonActorModelVersion; }

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

    private Camera3d camera;
    private float zThicknessInPixels = 100;
    private String parentAnchorImageName = SMALL_PARENT_IMG;
    private boolean modulateParentImage = true;
    private boolean isFocusOnNextParent = false;
    
    //
    private boolean bIsVisible = true;


    // note: this initial color is now overridden by other components
    private float neuronColor[] = {0.8f, 1.0f, 0.3f};
    private final float blackColor[] = {0, 0, 0};

    private TileFormat tileFormat;
    private RenderInterpositionMethod rim = RenderInterpositionMethod.MIP;

    public enum RenderInterpositionMethod {
        MIP, Occlusion
    }

    public SkeletonActor() {

    }

    public void setRenderInterpositionMethod(RenderInterpositionMethod rim) {
        this.rim = rim;
    }


    /**
     * Overrides the default parent image name (found among the icons).
     * The only candidates which should be given, are enumerated.
     */
    public void setParentAnchorImageName(ParentAnchorImage image) {
        if (image == ParentAnchorImage.LARGE) {
            modulateParentImage = false;
        }
        parentAnchorImageName = image.toString();
    }

    private synchronized void displayLines(GLAutoDrawable glDrawable) {
        if (model.getNeuronLineIndices().isEmpty()) {
            return;
        }

        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        GL2 gl2 = gl.getGL2();
        transparencyDepthMode(gl, true);

        int n=0;

        boolean refreshBufferData=model.updateVertices();

        if (refreshBufferData) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, model.getCummulativeVertexOffset(), model.getVertexBuffer(), GL.GL_DYNAMIC_DRAW);

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorBo);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, model.getCummulativeColorOffset(), model.getColorBuffer(), GL.GL_DYNAMIC_DRAW);

            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, lineIbo);
            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, model.getCummulativeLineOffset(), model.getLineBuffer(), GL.GL_DYNAMIC_DRAW);
        }

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        lineShader.load(gl2);
        lineShader.setUniform(gl, "zThickness", zThicknessInPixels);
        float focus[] = {
                (float) camera.getFocus().getX(),
                (float) camera.getFocus().getY(),
                (float) camera.getFocus().getZ()};
        lineShader.setUniform3v(gl, "focus", 1, focus);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

        List<ElementDataOffset> lineOffsets=model.getLineOffsets();
        List<ElementDataOffset> vertexOffsets=model.getVertexOffsets();
        List<ElementDataOffset> colorOffsets=model.getColorOffsets();
        n=0;
        for (ElementDataOffset lineElementOffset : lineOffsets) {
            ElementDataOffset vertexElementOffset = vertexOffsets.get(n);
            ElementDataOffset colorElementOffset = colorOffsets.get(n);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
            gl2.glVertexPointer(SkeletonActorModel.VERTEX_FLOAT_COUNT, GL2.GL_FLOAT, 0, vertexElementOffset.offset);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
            gl2.glColorPointer(SkeletonActorModel.COLOR_FLOAT_COUNT, GL2.GL_FLOAT, 0, colorElementOffset.offset);
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, lineIbo);

            // 1st pass : wider black line
            gl.glLineWidth(3.5f);
            lineShader.setUniform3v(gl, "baseColor", 1, blackColor);
            gl.glDrawElements(GL2.GL_LINES,
                    lineElementOffset.size/SkeletonActorModel.INT_BYTE_COUNT,
                    GL2.GL_UNSIGNED_INT,
                    lineElementOffset.offset);
            lineOffset(gl, true);

            // 2nd pass : colored line
            gl.glLineWidth(1.5f);
            NeuronStyle style;
            NeuronStyleModel neuronStyles=model.getNeuronStyles();
            if (neuronStyles.containsKey(lineElementOffset.id)) {
                style = neuronStyles.get(lineElementOffset.id);
            } else {
                style = NeuronStyle.getStyleForNeuron(lineElementOffset.id);
            }
            lineShader.setUniform3v(gl, "baseColor", 1, style.getColorAsFloatArray());
            gl.glDrawElements(GL2.GL_LINES,
                    lineElementOffset.size/SkeletonActorModel.INT_BYTE_COUNT,
                    GL2.GL_UNSIGNED_INT,
                    lineElementOffset.offset);
            lineOffset(gl, false);
            n++;
        }

        //log.info("displayLines2 Check4");

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        transparencyDepthMode(gl, false);
        lineShader.unload(gl2);
    }

    protected void lineOffset(GL2GL3 gl, boolean enable) {
        if (rim == RenderInterpositionMethod.Occlusion) {
            if (enable) {
                // narrower colored line
                gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
                gl.glPolygonOffset(1.0f, 1.0f);
            }
            else {
                gl.glDisable(GL2.GL_POLYGON_OFFSET_LINE);
            }
        }
    }

    private synchronized void displayAnchors(GLAutoDrawable glDrawable) {
        // Paint anchors as point sprites
        Map<Long, IntBuffer> neuronPointIndices=model.getNeuronPointIndices();
        if (neuronPointIndices == null) {
            return;
        }
        if (neuronPointIndices.size() < 1) {
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        setupAnchorShaders(gl);

        int n=0;

        boolean refreshBufferData=model.updatePoints();

        if (refreshBufferData) {
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, model.getCummulativePointOffset(), model.getPointBuffer(), GL.GL_DYNAMIC_DRAW);
        }

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        //PassThroughTextureShader.checkGlError(gl, "paint anchors 1");

        List<ElementDataOffset> pointOffsets=model.getPointOffsets();
        Map<Long, ElementDataOffset> vertexOffsetMap=model.getVertexOffsetMap();
        Map<Long, ElementDataOffset> colorOffsetMap=model.getColorOffsetMap();

        for (ElementDataOffset pointOffset : pointOffsets) {

            Long neuronID = pointOffset.id;
            ElementDataOffset vertexOffset=vertexOffsetMap.get(neuronID);
            ElementDataOffset colorOffset=colorOffsetMap.get(neuronID);

            Skeleton skeleton=model.getSkeleton();

            if (vertexOffset!=null && colorOffset!=null) {

                // setup per-neuron anchor shader settings (used to be in setupAnchorShader)
                int tempIndex;
                Anchor hoverAnchor = skeleton.getHoverAnchor();
                if (hoverAnchor != null && hoverAnchor.getNeuronID().equals(neuronID)) {
                    tempIndex = model.getIndexForAnchor(hoverAnchor);
                } else {
                    tempIndex = -1;
                }
                anchorShader.setUniform(gl, "highlightAnchorIndex", tempIndex);
                Anchor nextParent = skeleton.getNextParent();
                if (nextParent != null && nextParent.getNeuronID().equals(neuronID)) {
                    tempIndex = model.getIndexForAnchor(nextParent);
                } else {
                    tempIndex = -1;
                }
                anchorShader.setUniform(gl, "parentAnchorIndex", tempIndex);
                anchorShader.setUniform(gl, "isDiscardNonParent", discardNonParent);

                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
                gl.glVertexPointer(SkeletonActorModel.VERTEX_FLOAT_COUNT, GL2.GL_FLOAT, 0, vertexOffset.offset);

                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBo);
                gl.glColorPointer(SkeletonActorModel.COLOR_FLOAT_COUNT, GL2.GL_FLOAT, 0, colorOffset.offset);

                gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, pointIbo);
                gl.glDrawElements(GL2.GL_POINTS,
                        pointOffset.size / SkeletonActorModel.INT_BYTE_COUNT,
                        GL2.GL_UNSIGNED_INT,
                        pointOffset.offset);
            }
        }

        // tear down
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);

        tearDownAnchorShaders(gl);
    }

    private void setupAnchorShaders(GL2 gl) {
        gl.glEnable(GL2.GL_POINT_SPRITE);
        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        // parent anchor texture
        gl.glActiveTexture(GL2.GL_TEXTURE1);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, parentAnchorTextureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        // plain anchor texture
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, anchorTextureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        //
        transparencyDepthMode(gl, true);
        anchorShader.load(gl);

        // used to set uniforms for hover index and parent index here, but
        //  that's now done in the appropriate update loops
        // At high zoom, keep thickness to at least 5 pixels deep.
        anchorShader.setUniform(gl, "zThickness", zThicknessInPixels);
        float focus[] = {
            (float) camera.getFocus().getX(),
            (float) camera.getFocus().getY(),
            (float) camera.getFocus().getZ()};
        anchorShader.setUniform3v(gl, "focus", 1, focus);
        anchorShader.setUniform(gl, "anchorTexture", 0);
        anchorShader.setUniform(gl, "parentAnchorTexture", 1);
        anchorShader.setUniform(gl, "modulateParentImage", this.modulateParentImage ? 1 : 0);
        if (! modulateParentImage) {
            anchorShader.setUniform(gl, "startingPointSize", 20.0f);
            anchorShader.setUniform(gl, "maxPointSize", 6.0f);
        }
    }

    private void tearDownAnchorShaders(GL2 gl) {
        anchorShader.unload(gl);
        gl.glDisable(GL2.GL_POINT_SPRITE);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        transparencyDepthMode(gl, false);
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        if (!bIsVisible) {
            return;
        }
        Multiset<Long> neuronVertexCount=model.getNeuronVertexCount();
        if (neuronVertexCount.size() <= 0) {
            return;
        }
        if (!bIsGlInitialized) {
            init(glDrawable);
        }

        GL gl = glDrawable.getGL();
        if (rim == RenderInterpositionMethod.Occlusion) {
            gl.glEnable(GL2GL3.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2GL3.GL_LESS);
        }

        displayLines(glDrawable);
        displayTracedSegments(glDrawable);

        if (model.isAnchorsVisible()) {
            displayAnchors(glDrawable);
        }

        if (rim == RenderInterpositionMethod.Occlusion) {
            gl.glDisable(GL2GL3.GL_DEPTH_TEST);
        }
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
            zt = zThicknessInPixels * (float) camera.getPixelsPerSceneUnit() / zoomLimit;
        }
        lineShader.setUniform(gl2gl3, "zThickness", zt);
        // log.info("zThickness = "+zThickness);
        float focus[] = {
            (float) camera.getFocus().getX(),
            (float) camera.getFocus().getY(),
            (float) camera.getFocus().getZ()};
        lineShader.setUniform3v(gl2gl3, "focus", 1, focus);
        // black background
        gl.glLineWidth(5.0f);
        NeuronStyleModel neuronStyles=model.getNeuronStyles();
        Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments=model.getNeuronTracedSegments();

        for (Long neuronID : neuronTracedSegments.keySet()) {
            if (neuronStyles.get(neuronID) != null && !neuronStyles.get(neuronID).isVisible()) {
                continue;
            }

            lineShader.setUniform3v(gl2gl3, "baseColor", 1, blackColor);
            for (TracedPathActor segment : neuronTracedSegments.get(neuronID).values()) {
                segment.display(glDrawable);
            }
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

    public float getZThicknessInPixels() {
        return zThicknessInPixels;
    }

    public void setFocusOnNextParent(boolean flag) {
        isFocusOnNextParent = flag;
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
        model.updateAnchors();
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
            Graphics2D g2d = (Graphics2D) anchorImage.getGraphics();
            g2d.drawImage(source, 0, 0, null);
            g2d.dispose();
        }
        if (parentAnchorImage == null) {
            // load anchor texture
            ImageIcon anchorIcon = Icons.getIcon(parentAnchorImageName);
            Image source = anchorIcon.getImage();
            int w = source.getWidth(null);
            int h = source.getHeight(null);
            parentAnchorImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) parentAnchorImage.getGraphics();
            g2d.drawImage(source, 0, 0, null);
            g2d.dispose();
        }
        
        int ids[] = {0, 0};
        gl.glGenTextures(2, ids, 0); // count, array, offset
        anchorTextureId = ids[0];
        parentAnchorTextureId = ids[1];

        int w = anchorImage.getWidth();
        int h = anchorImage.getHeight();
        byte byteArray[] = new byte[w * h * 4];
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
        intPixels.rewind();
        gl.glTexImage2D(GL2.GL_TEXTURE_2D,
                0, // mipmap level
                GL2.GL_RGBA,
                w,
                h,
                0, // border
                GL2.GL_RGBA,
                GL2.GL_UNSIGNED_BYTE,
                pixels);
        
        // Parent texture may be like anchor texture, but with a "P" in it.
        // If not, could be different shape entirely.
        if (!( w == parentAnchorImage.getWidth() && h == parentAnchorImage.getHeight() ) ) {
            w = parentAnchorImage.getWidth();
            h = parentAnchorImage.getHeight();
            byteArray = new byte[w * h * 4];
            pixels = ByteBuffer.wrap(byteArray);
            pixels.order(ByteOrder.nativeOrder());
            intPixels = pixels.asIntBuffer();
        }
        gl.glBindTexture(GL2.GL_TEXTURE_2D, parentAnchorTextureId);
        intPixels.rewind();
        
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                intPixels.put(parentAnchorImage.getRGB(x, y));
            }
        }
        pixels.rewind();
        gl.glTexImage2D(GL2.GL_TEXTURE_2D,
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
        gl.glGenBuffers(4, ix, 0);
        vbo = ix[0];
        lineIbo = ix[1];
        pointIbo = ix[2];
        colorBo = ix[3];
        //
        PassThroughTextureShader.checkGlError(gl, "load anchor texture");
        transparencyDepthMode(gl, true);

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

        Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments=model.getNeuronTracedSegments();
        for (Long neuronID : neuronTracedSegments.keySet()) {
            for (TracedPathActor path : neuronTracedSegments.get(neuronID).values()) {
                path.dispose(glDrawable);
            }
        }
        model.getUpdater().update();
    }



    /**
     * is anything visible (ie, to be drawn)?
     */
    public boolean isVisible() {
        return bIsVisible;
    }

    public void setVisible(boolean b) {
        if (bIsVisible == b) {
            return;
        }
        bIsVisible = b;
        model.getUpdater().update();
    }

    protected void transparencyDepthMode(GL2GL3 gl, boolean enable) {
        if (enable) {
            if (rim == RenderInterpositionMethod.MIP) {
                // Apply transparency, even when anchors are not shown
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                gl.glEnable(GL2.GL_DEPTH_TEST);
                gl.glDepthFunc(GL2.GL_LEQUAL);
            }
        } else {
            if (rim == RenderInterpositionMethod.MIP) {
                gl.glDisable(GL2.GL_BLEND);
            } else {
                gl.glDisable(GL2.GL_DEPTH_TEST);
            }
        }
    }

    private TileFormat getTileFormat() {
        return tileFormat;
    }

}
