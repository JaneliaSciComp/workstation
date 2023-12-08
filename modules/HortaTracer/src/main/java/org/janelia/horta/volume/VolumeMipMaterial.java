
package org.janelia.horta.volume;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.apache.commons.io.IOUtils;
import org.janelia.geometry3d.*;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;
// import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.geometry3d.camera.BasicViewSlab;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.texture.Texture3d;
import org.openide.util.Exceptions;

/**
 * Renders 3D texture on polygons at texture coordinate
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class VolumeMipMaterial extends BasicMaterial
implements DepthSlabClipper
{
    private final Texture3d volumeTexture;
    private Texture2d opaqueDepthTexture = null;
    private int volumeTextureIndex = -1;
    private int opaqueDepthTextureIndex = -1;
    private int cameraPositionInTextureCoordinatesIndex = -1;
    private int levelOfDetailIndex = -1;
    private int nearSlabPlaneIndex = -1;
    private int farSlabPlaneIndex = -1;
    private int opacityFunctionMinIndex = -1;
    private int opacityFunctionMaxIndex = -1;
    private int volumeMicrometersIndex = -1;
    private int tcToCameraIndex = -1;
    private int opaqueZNearFarIndex = -1;
    private int colorChannelIndex1 = -1;
    private int channelVisibilityMaskIndex = -1;
    
    private float[] opaqueZNearFar = {1e-2f, 1e4f}; // absolute clip in camera space
    
    private final ImageColorModel colorMap;
    
    private int filteringOrderIndex = -1;
    
    // private int projectionModeIndex = -1;
    
    protected final ShaderProgram mipShader = new VolumeMipShader(0);
    protected final ShaderProgram occShader = new VolumeMipShader(1);
    protected final ShaderProgram isoShader = new VolumeMipShader(2);
    protected final ShaderProgram[] shaderPrograms = new ShaderProgram[] {
        mipShader,
        occShader,
        isoShader
    };
    
    private boolean uniformIndicesAreDirty = true;
    private VolumeState volumeState = new VolumeState();

    // Relative clip in camera space
    private float relativeZNear = 0.92f;
    private float relativeZFar = 1.08f;
    
    public VolumeMipMaterial(Texture3d volumeTexture, ImageColorModel colorMap) 
    {
        this.colorMap = colorMap;
        this.volumeTexture = volumeTexture;
        this.volumeTexture.setGenerateMipmaps(true);
        // this.volumeTexture.setMinFilter(GL3.GL_NEAREST_MIPMAP_NEAREST);
        this.volumeTexture.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
        this.volumeTexture.setMagFilter(GL3.GL_LINEAR);

        shaderProgram = mipShader;
        
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        this.relativeZNear = zNear;
        this.relativeZFar = zFar;
    }
    
    public Texture3d getTexture() {return volumeTexture;}

    public VolumeState getVolumeState()
    {
        return volumeState;
    }

    public void setVolumeState(VolumeState volumeState)
    {
        if (this.volumeState == volumeState)
            return;
        this.volumeState = volumeState;
        this.uniformIndicesAreDirty = true;
    }

    @Override
    protected void activateCull(GL3 gl) {
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_FRONT);
    }

    public int getFilteringOrder() {
        return volumeState.filteringOrder;
    }

    public void setFilteringOrder(int filteringOrder) {
        volumeState.filteringOrder = filteringOrder;
    }

    public int getProjectionMode() {
        return volumeState.projectionMode;
    }

    public void setProjectionMode(int projectionMode) {
        if (volumeState.projectionMode == projectionMode)
            return;
        volumeState.projectionMode = projectionMode;
        updateShaderProgram();
        uniformIndicesAreDirty = true;
    }
    
    private void updateShaderProgram() {
        if (shaderProgram != shaderPrograms[volumeState.projectionMode]) {
            shaderProgram = shaderPrograms[volumeState.projectionMode];
            uniformIndicesAreDirty = true;
        }
    }
    
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) 
    {
        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);
        // Pass camera position, in texture coordinates, to shader
        if (mesh.getGeometry() instanceof VolumeTextureMesh) {
            // Pass in camera position in texture coordinates
            // Use inverse-Kane notation, since opengl matrices, such as Matrix4, are row major
            VolumeTextureMesh mg = (VolumeTextureMesh)mesh.getGeometry();
            Matrix4 world_X_tc = mg.getTransformWorldToTexCoord(); // OK
            Matrix4 camera_X_world = modelViewMatrix.inverse();
            Matrix4 camera_X_tc = new Matrix4(camera_X_world).multiply(world_X_tc); // OK
            Vector4 tc_camera = camera_X_tc.multiply( new Vector4(0, 0, 0, 1) );
            float[] arr = tc_camera.toArray();
            gl.glUniform3fv(cameraPositionInTextureCoordinatesIndex, 1, arr, 0);
            
            // Also pass in level-of-detail
            float meshResolution = mg.getMinResolution();
            float screenResolution = 
                    camera.getVantage().getSceneUnitsPerViewportHeight()
                    / camera.getViewport().getHeightPixels();
            float levelOfDetail = -(float)( 
                    Math.log(meshResolution / screenResolution) 
                    / Math.log(2.0) );
            // Performance/Quality tradeoff: adjust to taste; 0.5f matches automatic lod
            levelOfDetail += 0.5f; 
            levelOfDetail = Math.max(levelOfDetail, 0); // hard minimum
            levelOfDetail = (float)Math.floor(levelOfDetail); // convert to int
            int intLod = (int) levelOfDetail;
            // System.out.println("Computed level of detail = "+levelOfDetail);
            gl.glUniform1i(levelOfDetailIndex, intLod);
            
            // Clip on slab, to limit depth of rendering
            // float slabThickness = getViewSlabThickness(camera);
            // float slabThickness = relativeSlabThickness * camera.getVantage().getSceneUnitsPerViewportHeight();
            float cameraFocusDistance = 0.0f;
            if (camera instanceof PerspectiveCamera) {
                PerspectiveCamera pc = (PerspectiveCamera) camera;
                cameraFocusDistance = pc.getCameraFocusDistance();
            }
            // Plane equation is easy to express in camera frame
            float absZNear = cameraFocusDistance * relativeZNear;
            float absZFar = cameraFocusDistance * relativeZFar;
            Vector4 nearSlabPlane_camera = new Vector4(0, 0, 1, absZNear);
            Vector4 farSlabPlane_camera = new Vector4(0, 0, 1, absZFar);
            // But we need plane equation in texture coordinate frame
            Matrix4 planeXform = camera_X_tc.inverse().transpose(); // look it up...
            Vector4 nearSlabPlane_tc = planeXform.multiply( nearSlabPlane_camera );
            Vector4 farSlabPlane_tc = planeXform.multiply( farSlabPlane_camera );
            // System.out.println("plane equation texCoord = "+nearSlabPlane_tc);
            gl.glUniform4fv(nearSlabPlaneIndex, 1, nearSlabPlane_tc.toArray(), 0);
            gl.glUniform4fv(farSlabPlaneIndex, 1, farSlabPlane_tc.toArray(), 0);

            // Brightness/Contrast
            // float [] oc = new float[] {0, 0, 1};
            // TODO - make use of alpha channel, which gets set to "1.0" for images with less than 4 channels
            float [] opMin = new float[] {0, 0};
            float [] opMax = new float[] {1, 1};
            if (colorMap != null) {
                ChannelColorModel m = colorMap.getChannel(0);
                Vector3 color = new Vector3(m.getColor().getRed()/255.0f, m.getColor().getGreen()/255.0f, m.getColor().getBlue()/255.0f);
                gl.glUniform3fv(colorChannelIndex1, 1, color.toArray(), 0);

                Vector3 channelVisibilityMask = new Vector3(colorMap.getChannel(0).isVisible() ? 1 : 0,
                        colorMap.getChannel(1).isVisible() ? 1 : 0,
                        1);
                gl.glUniform3fv(channelVisibilityMaskIndex, 1, channelVisibilityMask.toArray(), 0);

                for (int c = 0; c < 2; ++c) {
                    ChannelColorModel chan = colorMap.getChannel(c);
                    opMin[c] = chan.getNormalizedMinimum();
                    opMax[c] = chan.getNormalizedMaximum();
                }
            }
            gl.glUniform2fv(opacityFunctionMinIndex, 1, opMin, 0);
            gl.glUniform2fv(opacityFunctionMaxIndex, 1, opMax, 0);

            Vector4 micrometerVolumes = world_X_tc.multiply(new Vector4(1, 1, 1, 0));
            float [] volMic = new float[] {
                1.0f / Math.abs(micrometerVolumes.get(0)), 
                1.0f / Math.abs(micrometerVolumes.get(1)), 
                1.0f / Math.abs(micrometerVolumes.get(2))};
            gl.glUniform3fv(volumeMicrometersIndex, 1, volMic, 0);
            
            // for isosurface, we need to convert normals from texCoords to camera
            gl.glUniformMatrix4fv(tcToCameraIndex, 1, false, camera_X_tc.inverse().asArray(), 0);

            opaqueZNearFar[0] = absZNear;
            opaqueZNearFar[1] = absZFar;
            gl.glUniform2fv(opaqueZNearFarIndex, 1, opaqueZNearFar, 0);
        }
       
        super.displayMesh(gl, mesh, camera, modelViewMatrix);
    }

    @Override
    protected void displayWithMatrices(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        
        Viewport vp = camera.getViewport();
        ConstViewSlab slab = new BasicViewSlab(vp.getzNearRelative() / 10.0f, vp.getzFarRelative() + 100.0f);
        try {
            camera.pushInternalViewSlab(slab);
            if (modelViewMatrix == null)
                modelViewMatrix = new Matrix4(camera.getViewMatrix());
            gl.glUniformMatrix4fv(modelViewIndex, 1, false, modelViewMatrix.asArray(), 0);
        
            Matrix4 projectionMatrix = camera.getProjectionMatrix();
            gl.glUniformMatrix4fv(projectionIndex, 1, false, projectionMatrix.asArray(), 0);

            displayNoMatrices(gl, mesh, camera, modelViewMatrix);
        }
        finally {
            camera.popInternalViewSlab();
        }
    }

    @Override
    public void dispose(GL3 gl) {
        // Destroy extra shader programs
        for (ShaderProgram p : new ShaderProgram[] {isoShader, mipShader, occShader}) {
            // if (p == shaderProgram) continue;
            p.dispose(gl);
        }
        super.dispose(gl);
        volumeTexture.dispose(gl);
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        updateShaderProgram();
        super.load(gl, camera);
        
        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);

        if (volumeState.filteringOrder <= 0) {
            volumeTexture.setMagFilter(GL3.GL_NEAREST);
            volumeTexture.setMinFilter(GL3.GL_NEAREST_MIPMAP_NEAREST);
        }
        else {
            // Both TRILINEAR and TRICUBIC filtering use hardware trilinear filtering as a basis
            // The distinction is made in the shader
            volumeTexture.setMagFilter(GL3.GL_LINEAR);
            volumeTexture.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
        }
        gl.glUniform1i(filteringOrderIndex, volumeState.filteringOrder);
        // gl.glUniform1i(projectionModeIndex, projectionMode);

        // 3D volume texture
        int volumeTextureUnit = 0;
        volumeTexture.bind(gl, volumeTextureUnit);
        gl.glUniform1i(volumeTextureIndex, volumeTextureUnit); // TODO - sometimes triggers GL error
        
        // 2D depth texture -- Z-buffer from opaque rendering pass
        if (opaqueDepthTexture != null) {
            int depthTextureUnit = 1;
            opaqueDepthTexture.bind(gl, depthTextureUnit);
            gl.glUniform1i(opaqueDepthTextureIndex, depthTextureUnit);
        }
     }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        volumeTexture.unbind(gl); // restore depth buffer writes
        if (opaqueDepthTexture != null)
            opaqueDepthTexture.unbind(gl);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }
    
    @Override
    public void init(GL3 gl) {
        updateShaderProgram();
        super.init(gl);
        updateUniformIndices(gl);
        volumeTexture.init(gl);
    }
    
    private void updateUniformIndices(GL3 gl) {
        int s = shaderProgram.getProgramHandle();

        cameraPositionInTextureCoordinatesIndex = gl.glGetUniformLocation(s,
            "camPosInTc");
        volumeTextureIndex = gl.glGetUniformLocation(s, "volumeTexture");
        opaqueDepthTextureIndex = gl.glGetUniformLocation(s, "opaqueDepthTexture");
        levelOfDetailIndex = gl.glGetUniformLocation(s, "levelOfDetail");
        nearSlabPlaneIndex = gl.glGetUniformLocation(s, "nearSlabPlane");
        farSlabPlaneIndex = gl.glGetUniformLocation(s, "farSlabPlane");
        opacityFunctionMinIndex = gl.glGetUniformLocation(s, "opacityFunctionMin");
        opacityFunctionMaxIndex = gl.glGetUniformLocation(s, "opacityFunctionMax");
        volumeMicrometersIndex = gl.glGetUniformLocation(s, "volumeMicrometers");
        filteringOrderIndex = gl.glGetUniformLocation(s, "filteringOrder");
        modelViewIndex = gl.glGetUniformLocation(s, "modelViewMatrix"); // -1 means no such item
        projectionIndex = gl.glGetUniformLocation(s, "projectionMatrix");
        tcToCameraIndex = gl.glGetUniformLocation(s, "tcToCamera");
        opaqueZNearFarIndex = gl.glGetUniformLocation(s, "opaqueZNearFar");
        colorChannelIndex1 = gl.glGetUniformLocation(s, "colorChannel1");
        channelVisibilityMaskIndex = gl.glGetUniformLocation(s, "channelVisibilityMask");

        uniformIndicesAreDirty = false;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture)
    {
        this.opaqueDepthTexture = opaqueDepthTexture;
    }
    
    private static class VolumeMipShader extends BasicShaderProgram {
        public VolumeMipShader(int projectionMode) {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeMipVrtx.glsl"))
                                        // + "PanoramaVrtx.glsl")) // TODO: for testing only
                );
                String projectionDefine = "#define PROJECTION_MODE " + projectionMode + "\n";
                String basicFragShaderString = IOUtils.toString(getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeMipFrag.glsl"), "UTF-8");
                // System.out.println(basicFragShaderString);
                String fragShaderString = basicFragShaderString.replace("#define PROJECTION_MODE PROJECTION_MAXIMUM", projectionDefine);
                // System.out.println(fragShaderString);
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER, fragShaderString)
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
    public static class VolumeState {
        public static int PROJECTION_MAXIMUM = 0;
        public static int PROJECTION_OCCLUDING = 1;
        public static int PROJECTION_ISOSURFACE = 2;
        
        public static int FILTER_NEAREST = 0;
        public static int FILTER_TRILINEAR = 1;
        public static int FILTER_TRICUBIC = 3; // There is no "2"

        public static int BLOCK_STRATEGY_FINEST_8_MAX = 0;
        public static int BLOCK_STRATEGY_OCTTREE = 1;
        
        public int filteringOrder = FILTER_TRILINEAR;  // 0: NEAREST; 1: TRILINEAR; 2: <not used> 3: TRICUBIC
        public int projectionMode = PROJECTION_MAXIMUM; // 0: Maximum intensity projection; 1: Occluding
        public int blockStrategy = BLOCK_STRATEGY_OCTTREE;
    }
    
}
