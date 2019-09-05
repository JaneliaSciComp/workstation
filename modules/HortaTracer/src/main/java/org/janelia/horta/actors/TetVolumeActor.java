package org.janelia.horta.actors;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.model.UnmixingParameters;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Object3d;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;
import org.janelia.geometry3d.camera.BasicViewSlab;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.GL3Resource;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.material.VolumeMipMaterial.VolumeState;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.blocks.*;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;

/**
 * Volume rendering actor for blocks consisting of five tetrahedral components.
 * One Singleton TetVolumeActor may contain multiple TetVolumeMeshActors,
 * each of which represents one volume rendered block.
 * TetVolumeActor is responsible for managing the volume rendering GLSL shader.
 *
 * @author Christopher Bruns
 */
public class TetVolumeActor extends BasicGL3Actor implements DepthSlabClipper {
    private static TetVolumeActor singletonInstance;
    
    // Singleton access
    static public TetVolumeActor getInstance() {
        if (singletonInstance == null)
            singletonInstance = new TetVolumeActor();
        return singletonInstance;
    }
    
    // Use one global shader, rather than one shader per volume block.
    private final TetVolumeMaterial.TetVolumeShader shader;
    private ImageColorModel brightnessModel;
    private final Texture2d colorMapTexture = new Texture2d();
    private Texture2d opaqueDepthTexture = null;
    private float zNearRelative = 0.10f;
    private float zFarRelative = 100.0f; // relative z clip planes
    private final float[] zNearFar = new float[] {0.1f, 100.0f, 1.0f}; // absolute clip for shader
    // Initialize tracing channel to average of the first two channels
    private final UnmixingParameters unmixMinScale = new UnmixingParameters(new float[] {0.0f, 0.0f, 0.5f, 0.5f});
    private VolumeState volumeState = new VolumeState();

    private final BlockSorter blockSorter = new BlockSorter();    
    private final KtxTileCache dynamicTiles = new KtxTileCache(null);
    private BlockChooser<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> chooser;
    private BlockDisplayUpdater<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> blockDisplayUpdater;
    private final Collection<GL3Resource> obsoleteActors = new ArrayList<>();

    // Singleton actor has private constructor
    private TetVolumeActor() {
        super(null);
        shader = new TetVolumeMaterial.TetVolumeShader();
        BufferedImage colorMapImage = null;
        try {
            colorMapImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/org/janelia/horta/images/"
                            + "HotColorMap.png"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        colorMapTexture.loadFromBufferedImage(colorMapImage);
        colorMapTexture.setGenerateMipmaps(false);
        colorMapTexture.setMinFilter(GL3.GL_LINEAR);
        colorMapTexture.setMagFilter(GL3.GL_LINEAR);
        chooser = new OctreeDisplayBlockChooser();
        initBlockStrategy(chooser);

        Lookups.singleton(unmixMinScale);
    }

    public void changeStrategy(int strategy) {
        if (strategy == VolumeState.BLOCK_STRATEGY_OCTTREE) {
            chooser = new OctreeDisplayBlockChooser();
        } else if (strategy == VolumeState.BLOCK_STRATEGY_FINEST_8_MAX){
            chooser = new Finest8DisplayBlockChooser();
        }
        initBlockStrategy(chooser);
    }

    private void initBlockStrategy(BlockChooser<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> chooser) {
        dynamicTiles.setBlockStrategy(chooser);
        blockDisplayUpdater = new BlockDisplayUpdater<>(chooser);
        blockDisplayUpdater.getDisplayChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                dynamicTiles.updateDesiredTiles(blockDisplayUpdater.getDesiredBlocks());
            }
        });
    }

    public Object3d addPersistentBlock(Object3d child) {
        return addChild(child);
    }
    
    public void addTransientBlock(BlockTileKey key) {
        dynamicTiles.addDesiredTile((KtxOctreeBlockTileKey) key);
    }
    
    public void setVolumeState(VolumeState volumeState) {
        this.volumeState = volumeState;
    }

    public void setHortaVantage(Vantage vantage) {
        blockDisplayUpdater.setVantage(vantage);
    }
    
    public void setKtxTileSource(KtxOctreeBlockTileSource source) {
        dynamicTiles.setSource(source);
        blockDisplayUpdater.setBlockTileSource(source);
    }
    
    public ObservableInterface getDynamicTileUpdateObservable() {
        return dynamicTiles.getDisplayChangeObservable();
    }
    
    public ShaderProgram getShader() {
        return shader;
    }
    
    public VolumeState getVolumeState() {
        return volumeState;
    }
    
    @Override
    public void init(GL3 gl) 
    {
        super.init(gl);
        colorMapTexture.init(gl);
        shader.init(gl);
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) 
    {
        dynamicTiles.disposeObsoleteTiles(gl);
        for (GL3Resource res : obsoleteActors) {
            res.dispose(gl);
        }
        obsoleteActors.clear();
        
        // 1) Initial fail-fast checks
        if (! isVisible())
            return;
        if (! isInitialized) init(gl);

        if (! dynamicTiles.canDisplay()) {
            if (getChildren() == null)
                return;
            if (getChildren().size() < 1)
                return;
        }

        // 2) Set up shader exactly once, for all volume blocks
        // Adjust actual Z-clip planes to allow imposter geometry to lie
        // outside the "official" Z-clip planes. Correct final clipping will 
        // happen in the fragment shader. This is necessary because the
        // imposter geometry represents multiple voxels at various depths.
        Viewport vp = camera.getViewport();
        // Z-near remains unchanged, because we are using back faces for imposter geometry.
        // But Z-far needs to be pushed back significantly.
        ConstViewSlab slab = new BasicViewSlab(vp.getzNearRelative(), vp.getzFarRelative() + 100.0f);
        try {
            camera.pushInternalViewSlab(slab);
            
            gl.glEnable(GL3.GL_BLEND);
            if (volumeState.projectionMode == VolumeState.PROJECTION_OCCLUDING) {
                // Occluding
                ((GL4)gl).glBlendEquationi(0, GL4.GL_FUNC_ADD); // RGBA color target
                ((GL4)gl).glBlendFuncSeparatei(0, 
                        GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA,
                        // Unlike most systems, we actually care about the final blended alpha
                        // component of the framebuffer:
                        GL4.GL_ONE, GL4.GL_ONE_MINUS_SRC_ALPHA);
            }
            else {
                // Max intensity
                ((GL4)gl).glBlendEquationi(0, GL4.GL_MAX); // RGBA color target
            }
            // Always use Maximum for blending secondary render target
            ((GL4)gl).glBlendEquationi(1, GL4.GL_MAX); // core intensity/depth target

            final boolean doCull = true;
            if (doCull) {
                // Display Front faces only.
                gl.glEnable(GL3.GL_CULL_FACE);
                gl.glCullFace(GL3.GL_BACK);
                // (secretly the actual initial mesh geometry is the back faces of the tetrahedra though...)
                // (but semantically, we are showing front faces of the rendered volume)
            }            
            
            // Bind color map to texture unit 1, because 3D volume textures will use unit zero.
            colorMapTexture.bind(gl, 1);
            // 2D depth texture -- Z-buffer from opaque rendering pass
            if (opaqueDepthTexture != null) {
                final int depthTextureUnit = 2;
                opaqueDepthTexture.bind(gl, depthTextureUnit);
            }
            shader.load(gl);
            // Z-clip planes
            float focusDistance = ((PerspectiveCamera)camera).getCameraFocusDistance();
            zNearFar[0] = zNearRelative * focusDistance;
            zNearFar[1] = zFarRelative * focusDistance;
            zNearFar[2] = focusDistance;
            final int zNearFarUniformIndex = 2; // explicitly set in shader
            gl.glUniform3fv(zNearFarUniformIndex, 1, zNearFar, 0);
            // Brightness correction
            if (brightnessModel.getChannelCount() == 3) {
                // Use a multichannel model
                ChannelColorModel c0 = brightnessModel.getChannel(0);
                ChannelColorModel c1 = brightnessModel.getChannel(1);
                ChannelColorModel c2 = brightnessModel.getChannel(2);
                
                // float max0 = c0.getDataMax();
                // min
                gl.glUniform3fv(3, 1, new float[] {
                        c0.getNormalizedMinimum(), 
                        c1.getNormalizedMinimum(), 
                        c2.getNormalizedMinimum(), 
                    }, 0);
                // max
                gl.glUniform3fv(4, 1, new float[] {
                        c0.getNormalizedMaximum(), 
                        c1.getNormalizedMaximum(), 
                        c2.getNormalizedMaximum(), 
                    }, 0);
                // gamma
                gl.glUniform3fv(5, 1, new float[] {
                        (float)c0.getGamma(), 
                        (float)c1.getGamma(),
                        (float)c2.getGamma()
                    }, 0);
                // visibility
                gl.glUniform3fv(6, 1, new float[] {
                        c0.isVisible() ? 1.0f : 0.0f,
                        c1.isVisible() ? 1.0f : 0.0f,
                        c2.isVisible() ? 1.0f : 0.0f
                    }, 0);
                // color
                float hsv0[] = new float[3];
                float hsv1[] = new float[3];
                float hsv2[] = new float[3];
                Color.RGBtoHSB(c0.getColor().getRed(), c0.getColor().getGreen(), c0.getColor().getBlue(), hsv0);
                Color.RGBtoHSB(c1.getColor().getRed(), c1.getColor().getGreen(), c1.getColor().getBlue(), hsv1);
                Color.RGBtoHSB(c2.getColor().getRed(), c2.getColor().getGreen(), c2.getColor().getBlue(), hsv2);
                float hues[] = new float[] {360.0f*hsv0[0], 360.0f*hsv1[0], 360.0f*hsv2[0]};
                float saturations[] = new float[] {hsv0[1], hsv1[1], hsv2[1]};
                
                gl.glUniform3fv(11, 1, hues, 0);
                gl.glUniform3fv(12, 1, saturations, 0);
                
                // unmixing parameters
                gl.glUniform4fv(7, 1, unmixMinScale.getParams(), 0);
                
                gl.glUniform1i(13, volumeState.projectionMode);
                gl.glUniform1i(14, volumeState.filteringOrder);
            }
            else {
                throw new UnsupportedOperationException("Unexpected number of color channels");
            }
            
            Matrix4 modelViewMatrix = parentModelViewMatrix;
            if (modelViewMatrix == null)
                modelViewMatrix = camera.getViewMatrix();
            Matrix4 localMatrix = getTransformInParent();
            if (localMatrix != null)
                modelViewMatrix = new Matrix4(modelViewMatrix).multiply(localMatrix);

            // 3) Sort individual blocks by distance from camera, for 
            //    correct transparency blending
            List<SortableBlockActor> blockList = new ArrayList<>();
            List<GL3Actor> otherActorList = new ArrayList<>();
            List<Object3d> otherList = new ArrayList<>();
            for (SortableBlockActor actor : dynamicTiles.getDisplayedActors()) {
                blockList.add(actor);
            }
            for (Object3d child : getChildren()) {
                if (child instanceof SortableBlockActorSource) {
                    SortableBlockActorSource source = (SortableBlockActorSource)child;
                    for (SortableBlockActor block : source.getSortableBlockActors()) {
                        blockList.add(block);
                    }
                }
                else if (child instanceof GL3Actor) {
                    otherActorList.add((GL3Actor)child);
                }
                else {
                    otherList.add(child);
                }
            }
            assert otherActorList.isEmpty();
            assert otherList.isEmpty();
            
            // Order does not matter in MIP mode
            if (volumeState.projectionMode != VolumeState.PROJECTION_MAXIMUM) {
                blockSorter.setViewMatrix(modelViewMatrix);
                Collections.sort(blockList, blockSorter);        
            }

            // 4) Display blocks
            for (SortableBlockActor actor : blockList) {
                actor.display(gl, camera, modelViewMatrix);
            }
        }
        finally {
            // 5) Clean up shader
            camera.popInternalViewSlab();
            shader.unload(gl);
        }
    }
    
    @Override
    public void dispose(GL3 gl) {
        colorMapTexture.dispose(gl);
        shader.dispose(gl);
        dynamicTiles.disposeGL(gl);
        super.dispose(gl);
    }

    public ImageColorModel getBrightnessModel() {
        return brightnessModel;
    }

    /* Compute unmixing parameters for 
     *  channel 1 minus channel 2, 
     * using current brightness settings.
     */
    public void unmixChannelOne() {
        float scale = setChannelMins();
        unmixMinScale.getParams()[2] = 1.0f;
        unmixMinScale.getParams()[3] = scale;
    }
    
    public void unmixChannelTwo() {
        float scale = setChannelMins();
        unmixMinScale.getParams()[2] = 1.0f/scale;
        unmixMinScale.getParams()[3] = 1.0f;
        // update lookup
    }
    
    public void traceChannelOneTwoAverage() {
        setChannelMins();
        unmixMinScale.getParams()[0] = 0.0f;
        unmixMinScale.getParams()[1] = 0.0f;
        unmixMinScale.getParams()[2] = 0.5f;
        unmixMinScale.getParams()[3] = 0.5f;
        // update lookup
    }
    
    public void traceChannelOneRaw() {
        setChannelMins();
        unmixMinScale.getParams()[0] = 0.0f;
        unmixMinScale.getParams()[1] = 0.0f;
        unmixMinScale.getParams()[2] = 1.0f;
        unmixMinScale.getParams()[3] = 0.0f;
    }
    
    public void traceChannelTwoRaw() {
        setChannelMins();
        unmixMinScale.getParams()[0] = 0.0f;
        unmixMinScale.getParams()[1] = 0.0f;
        unmixMinScale.getParams()[2] = 0.0f;
        unmixMinScale.getParams()[3] = 1.0f;
    }
    
    public float[] getUnmixingParams() {
        return unmixMinScale.getParams();
    }
    
    public void setUnmixingParams(float[] minScale) {
        for (int i = 0; i < 4; ++i) {
            unmixMinScale.getParams()[i] = minScale[i];
        }
    }
    
    private float setChannelMins() {
        // Populate first two params, the min intensities, with absolute channel values
        ChannelColorModel c1 = brightnessModel.getChannel(0);
        ChannelColorModel c2 = brightnessModel.getChannel(1);
        float minA = c1.getNormalizedMinimum();
        float minB = c2.getNormalizedMinimum();
        unmixMinScale.getParams()[0] = minA;
        unmixMinScale.getParams()[1] = minB;
        float range1 = c1.getWhiteLevel() - c1.getBlackLevel();
        float range2 = c2.getWhiteLevel() - c2.getBlackLevel();
        float scaleB = -range1/range2;
        return scaleB;
    }
    
    public void setBrightnessModel(ImageColorModel brightnessModel) {
        this.brightnessModel = brightnessModel;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture)
    {
        this.opaqueDepthTexture = opaqueDepthTexture;
    }
    
    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        zNearRelative = zNear;
        zFarRelative = zFar;
    }

    public int getBlockCount() {
        int result = getChildren().size();
        result += dynamicTiles.getBlockCount();
        return result;
    }
    
    public void clearAllBlocks() {
        dynamicTiles.updateDesiredTiles(Collections.<KtxOctreeBlockTileKey>emptyList());
        for (Object3d actor : getChildren()) {
            if (! (actor instanceof GL3Resource))
                continue;
            GL3Resource res = (GL3Resource)actor;
            obsoleteActors.add(res);
        }
        getChildren().clear();
    }

    public void setAutoUpdate(boolean updateCache) {
        blockDisplayUpdater.setAutoUpdate(updateCache);
    }

    private static class BlockSorter implements Comparator<SortableBlockActor> 
    {
        private Matrix4 viewMatrix;

        public BlockSorter() {
        }

        @Override
        public int compare(SortableBlockActor o1, SortableBlockActor o2) {
            // 1) If blocks are not the same resolution, sort on resolution
            BlockTileResolution res1 = o1.getResolution();
            BlockTileResolution res2 = o2.getResolution();
            if (! res1.equals(res2)) {
                return res1.compareTo(res2);
            }
            // 2) sort based on distance from centroid to camera
            if (viewMatrix == null)
                throw new UnsupportedOperationException("View Matrix is Null");
            Vector4 v1 = viewMatrix.multiply(o1.getHomogeneousCentroid());
            Vector4 v2 = viewMatrix.multiply(o2.getHomogeneousCentroid());
            float d1 = 0.0f; // squared distance from camera to centroid
            float d2 = 0.0f; // squared distance from camera to centroid
            for (int i = 0; i < 3; ++i) {
                d1 += v1.get(i) * v1.get(i);
                d2 += v2.get(i) * v2.get(i);
            }
            return d1 > d2 ? -1 : d1 == d2 ? 0 : 1;
        }

        public void setViewMatrix(Matrix4 viewMatrix) {
            this.viewMatrix = viewMatrix;
        }
    }

}
