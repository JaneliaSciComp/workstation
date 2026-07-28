package org.janelia.horta.volume;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.apache.commons.io.IOUtils;
import org.janelia.geometry3d.*;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;
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
 * Multi-channel volume MIP material for OME-Zarr.
 *
 * Forked from {@link VolumeMipMaterial} so the KTX {@code BrickActor} path (which also extends
 * VolumeMipMaterial) is left untouched. Differs in that each channel is its own single-component
 * {@link Texture3d}, bound to a separate texture unit, and the channels are combined jointly in the
 * fragment shader (OmeZarrVolumeMipFrag.glsl) rather than packed into one texture.
 */
public class OmeZarrVolumeMipMaterial extends BasicMaterial
        implements DepthSlabClipper {

    // Must match MAX_CHANNELS in OmeZarrVolumeMipFrag.glsl
    public static final int MAX_CHANNELS = 8;

    // The texture unit reserved for the opaque depth texture (after the channel units).
    private static final int DEPTH_TEXTURE_UNIT = MAX_CHANNELS;

    private final Texture3d[] channelTextures;
    private final int channelCount;

    private Texture2d opaqueDepthTexture = null;

    private int channelTextureIndex = -1;
    private int channelCountIndex = -1;
    private int opaqueDepthTextureIndex = -1;
    private int cameraPositionInTextureCoordinatesIndex = -1;
    private int levelOfDetailIndex = -1;
    private int nearSlabPlaneIndex = -1;
    private int farSlabPlaneIndex = -1;
    private int volumeMicrometersIndex = -1;
    private int tcToCameraIndex = -1;
    private int opaqueZNearFarIndex = -1;
    private int filteringOrderIndex = -1;

    private int channelColorIndex = -1;
    private int channelMinIndex = -1;
    private int channelMaxIndex = -1;
    private int channelGammaIndex = -1;
    private int channelVisibleIndex = -1;
    private int unmixMinIndex = -1;
    private int unmixScaleIndex = -1;

    private float[] opaqueZNearFar = {1e-2f, 1e4f}; // absolute clip in camera space

    private final ImageColorModel colorMap;

    protected final ShaderProgram mipShader = new OmeZarrVolumeMipShader(0);
    protected final ShaderProgram occShader = new OmeZarrVolumeMipShader(1);
    protected final ShaderProgram isoShader = new OmeZarrVolumeMipShader(2);
    protected final ShaderProgram[] shaderPrograms = new ShaderProgram[]{
            mipShader,
            occShader,
            isoShader
    };

    private boolean uniformIndicesAreDirty = true;
    private VolumeMipMaterial.VolumeState volumeState = new VolumeMipMaterial.VolumeState();

    // Relative clip in camera space
    private float relativeZNear = 0.92f;
    private float relativeZFar = 1.08f;

    public OmeZarrVolumeMipMaterial(Texture3d[] channelTextures, ImageColorModel colorMap) {
        this.colorMap = colorMap;
        this.channelTextures = channelTextures;
        this.channelCount = Math.min(channelTextures.length, MAX_CHANNELS);

        for (Texture3d t : channelTextures) {
            if (t == null)
                continue;
            t.setGenerateMipmaps(true);
            t.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
            t.setMagFilter(GL3.GL_LINEAR);
        }

        shaderProgram = mipShader;

        setShadingStyle(Shading.FLAT);
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        this.relativeZNear = zNear;
        this.relativeZFar = zFar;
    }

    /** @return the first channel texture (used by caches that key off a representative texture). */
    public Texture3d getTexture() {
        return channelTextures.length > 0 ? channelTextures[0] : null;
    }

    public Texture3d[] getChannelTextures() {
        return channelTextures;
    }

    public VolumeMipMaterial.VolumeState getVolumeState() {
        return volumeState;
    }

    public void setVolumeState(VolumeMipMaterial.VolumeState volumeState) {
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
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);

        if (mesh.getGeometry() instanceof VolumeTextureMesh) {
            VolumeTextureMesh mg = (VolumeTextureMesh) mesh.getGeometry();
            Matrix4 world_X_tc = mg.getTransformWorldToTexCoord();
            Matrix4 camera_X_world = modelViewMatrix.inverse();
            Matrix4 camera_X_tc = new Matrix4(camera_X_world).multiply(world_X_tc);
            Vector4 tc_camera = camera_X_tc.multiply(new Vector4(0, 0, 0, 1));
            gl.glUniform3fv(cameraPositionInTextureCoordinatesIndex, 1, tc_camera.toArray(), 0);

            // level-of-detail
            float meshResolution = mg.getMinResolution();
            float screenResolution =
                    camera.getVantage().getSceneUnitsPerViewportHeight()
                            / camera.getViewport().getHeightPixels();
            float levelOfDetail = -(float) (Math.log(meshResolution / screenResolution) / Math.log(2.0));
            levelOfDetail += 0.5f;
            levelOfDetail = Math.max(levelOfDetail, 0);
            levelOfDetail = (float) Math.floor(levelOfDetail);
            gl.glUniform1i(levelOfDetailIndex, (int) levelOfDetail);

            float cameraFocusDistance = 0.0f;
            if (camera instanceof PerspectiveCamera) {
                cameraFocusDistance = ((PerspectiveCamera) camera).getCameraFocusDistance();
            }
            float absZNear = cameraFocusDistance * relativeZNear;
            float absZFar = cameraFocusDistance * relativeZFar;
            Vector4 nearSlabPlane_camera = new Vector4(0, 0, 1, absZNear);
            Vector4 farSlabPlane_camera = new Vector4(0, 0, 1, absZFar);
            Matrix4 planeXform = camera_X_tc.inverse().transpose();
            gl.glUniform4fv(nearSlabPlaneIndex, 1, planeXform.multiply(nearSlabPlane_camera).toArray(), 0);
            gl.glUniform4fv(farSlabPlaneIndex, 1, planeXform.multiply(farSlabPlane_camera).toArray(), 0);

            // Per-channel transfer functions, driven by the ImageColorModel.
            setChannelUniforms(gl);

            Vector4 micrometerVolumes = world_X_tc.multiply(new Vector4(1, 1, 1, 0));
            float[] volMic = new float[]{
                    1.0f / Math.abs(micrometerVolumes.get(0)),
                    1.0f / Math.abs(micrometerVolumes.get(1)),
                    1.0f / Math.abs(micrometerVolumes.get(2))};
            gl.glUniform3fv(volumeMicrometersIndex, 1, volMic, 0);

            gl.glUniformMatrix4fv(tcToCameraIndex, 1, false, camera_X_tc.inverse().asArray(), 0);

            opaqueZNearFar[0] = absZNear;
            opaqueZNearFar[1] = absZFar;
            gl.glUniform2fv(opaqueZNearFarIndex, 1, opaqueZNearFar, 0);
        }

        super.displayMesh(gl, mesh, camera, modelViewMatrix);
    }

    private void setChannelUniforms(GL3 gl) {
        int n = channelCount;
        gl.glUniform1i(channelCountIndex, n);

        if (colorMap == null)
            return;

        // Only read as many ImageColorModel channels as actually exist, to avoid the
        // index-out-of-bounds that produced PR #40's GLException when the model was not
        // yet sized to the data.
        int modelChannels = colorMap.getChannelCount();

        float[] colors = new float[MAX_CHANNELS * 3];
        float[] mins = new float[MAX_CHANNELS];
        float[] maxes = new float[MAX_CHANNELS];
        float[] gammas = new float[MAX_CHANNELS];
        float[] visible = new float[MAX_CHANNELS];

        for (int c = 0; c < n; ++c) {
            if (c >= modelChannels) {
                // No color model for this channel yet: render hidden until the model catches up.
                maxes[c] = 1f;
                gammas[c] = 1f;
                continue;
            }
            ChannelColorModel chan = colorMap.getChannel(c);
            colors[c * 3] = chan.getColor().getRed() / 255.0f;
            colors[c * 3 + 1] = chan.getColor().getGreen() / 255.0f;
            colors[c * 3 + 2] = chan.getColor().getBlue() / 255.0f;
            mins[c] = (float) chan.getNormalizedMinimum();
            maxes[c] = (float) chan.getNormalizedMaximum();
            gammas[c] = (float) chan.getGamma();
            visible[c] = chan.isVisible() ? 1f : 0f;
        }

        // Synthetic tracing/unmix channel display parameters, in the slot right after the real
        // channels (mirrors the legacy TetVolumeActor convention of a dedicated channel-index-2
        // slider for the unmixed channel). The shader combines it alongside the real channels
        // (see combineChannels()/integrate_intensity() in OmeZarrVolumeMipFrag.glsl).
        if (n < MAX_CHANNELS) {
            if (n < modelChannels) {
                ChannelColorModel tracingChan = colorMap.getChannel(n);
                colors[n * 3] = tracingChan.getColor().getRed() / 255.0f;
                colors[n * 3 + 1] = tracingChan.getColor().getGreen() / 255.0f;
                colors[n * 3 + 2] = tracingChan.getColor().getBlue() / 255.0f;
                mins[n] = (float) tracingChan.getNormalizedMinimum();
                maxes[n] = (float) tracingChan.getNormalizedMaximum();
                gammas[n] = (float) tracingChan.getGamma();
                visible[n] = tracingChan.isVisible() ? 1f : 0f;
            } else {
                maxes[n] = 1f;
                gammas[n] = 1f;
            }
        }

        gl.glUniform3fv(channelColorIndex, MAX_CHANNELS, colors, 0);
        gl.glUniform1fv(channelMinIndex, MAX_CHANNELS, mins, 0);
        gl.glUniform1fv(channelMaxIndex, MAX_CHANNELS, maxes, 0);
        gl.glUniform1fv(channelGammaIndex, MAX_CHANNELS, gammas, 0);
        gl.glUniform1fv(channelVisibleIndex, MAX_CHANNELS, visible, 0);

        // Per-channel unmixing weights (sized to MAX_CHANNELS; channels beyond n stay zero so they
        // do not contribute to the synthetic tracing channel).
        float[] unmixMin = new float[MAX_CHANNELS];
        float[] unmixScale = new float[MAX_CHANNELS];
        float[] unmixMins = colorMap.getUnmixChannelMinimums(n);
        float[] unmixScales = colorMap.getUnmixChannelScales(n);
        for (int c = 0; c < n; ++c) {
            unmixMin[c] = unmixMins[c];
            unmixScale[c] = unmixScales[c];
        }
        if (unmixMinIndex >= 0)
            gl.glUniform1fv(unmixMinIndex, MAX_CHANNELS, unmixMin, 0);
        if (unmixScaleIndex >= 0)
            gl.glUniform1fv(unmixScaleIndex, MAX_CHANNELS, unmixScale, 0);
    }

    @Override
    protected void displayWithMatrices(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
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
        } finally {
            camera.popInternalViewSlab();
        }
    }

    @Override
    public void dispose(GL3 gl) {
        for (ShaderProgram p : new ShaderProgram[]{isoShader, mipShader, occShader}) {
            p.dispose(gl);
        }
        super.dispose(gl);
        for (Texture3d t : channelTextures) {
            if (t != null)
                t.dispose(gl);
        }
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        updateShaderProgram();
        super.load(gl, camera);

        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);

        // Bind each channel texture to its own texture unit and point the sampler array at them.
        int[] samplerUnits = new int[MAX_CHANNELS];
        for (int c = 0; c < MAX_CHANNELS; ++c) {
            Texture3d t = (c < channelCount) ? channelTextures[c] : channelTextures[0];
            int unit = (c < channelCount) ? c : 0; // unused samplers alias unit 0 (guarded by channelCount)
            if (t != null) {
                if (volumeState.filteringOrder <= 0) {
                    t.setMagFilter(GL3.GL_NEAREST);
                    t.setMinFilter(GL3.GL_NEAREST_MIPMAP_NEAREST);
                } else {
                    t.setMagFilter(GL3.GL_LINEAR);
                    t.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
                }
                if (c < channelCount)
                    t.bind(gl, c);
            }
            samplerUnits[c] = unit;
        }
        gl.glUniform1iv(channelTextureIndex, MAX_CHANNELS, samplerUnits, 0);

        gl.glUniform1i(filteringOrderIndex, volumeState.filteringOrder);

        // 2D depth texture from the opaque render pass, on a unit past the channel units.
        if (opaqueDepthTexture != null) {
            opaqueDepthTexture.bind(gl, DEPTH_TEXTURE_UNIT);
            gl.glUniform1i(opaqueDepthTextureIndex, DEPTH_TEXTURE_UNIT);
        }
    }

    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        for (int c = 0; c < channelCount; ++c) {
            if (channelTextures[c] != null)
                channelTextures[c].unbind(gl);
        }
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
        for (Texture3d t : channelTextures) {
            if (t != null)
                t.init(gl);
        }
    }

    private void updateUniformIndices(GL3 gl) {
        int s = shaderProgram.getProgramHandle();

        cameraPositionInTextureCoordinatesIndex = gl.glGetUniformLocation(s, "camPosInTc");
        channelTextureIndex = gl.glGetUniformLocation(s, "channelTexture");
        channelCountIndex = gl.glGetUniformLocation(s, "channelCount");
        opaqueDepthTextureIndex = gl.glGetUniformLocation(s, "opaqueDepthTexture");
        levelOfDetailIndex = gl.glGetUniformLocation(s, "levelOfDetail");
        nearSlabPlaneIndex = gl.glGetUniformLocation(s, "nearSlabPlane");
        farSlabPlaneIndex = gl.glGetUniformLocation(s, "farSlabPlane");
        volumeMicrometersIndex = gl.glGetUniformLocation(s, "volumeMicrometers");
        filteringOrderIndex = gl.glGetUniformLocation(s, "filteringOrder");
        modelViewIndex = gl.glGetUniformLocation(s, "modelViewMatrix");
        projectionIndex = gl.glGetUniformLocation(s, "projectionMatrix");
        tcToCameraIndex = gl.glGetUniformLocation(s, "tcToCamera");
        opaqueZNearFarIndex = gl.glGetUniformLocation(s, "opaqueZNearFar");

        channelColorIndex = gl.glGetUniformLocation(s, "channelColor");
        channelMinIndex = gl.glGetUniformLocation(s, "channelMin");
        channelMaxIndex = gl.glGetUniformLocation(s, "channelMax");
        channelGammaIndex = gl.glGetUniformLocation(s, "channelGamma");
        channelVisibleIndex = gl.glGetUniformLocation(s, "channelVisible");
        unmixMinIndex = gl.glGetUniformLocation(s, "unmixMin");
        unmixScaleIndex = gl.glGetUniformLocation(s, "unmixScale");

        uniformIndicesAreDirty = false;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        this.opaqueDepthTexture = opaqueDepthTexture;
    }

    private static class OmeZarrVolumeMipShader extends BasicShaderProgram {
        public OmeZarrVolumeMipShader(int projectionMode) {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/VolumeMipVrtx.glsl"))
                );
                String projectionDefine = "#define PROJECTION_MODE " + projectionMode + "\n";
                String basicFragShaderString = IOUtils.toString(getClass().getResourceAsStream(
                        "/org/janelia/horta/shader/OmeZarrVolumeMipFrag.glsl"), "UTF-8");
                String fragShaderString = basicFragShaderString.replace(
                        "#define PROJECTION_MODE PROJECTION_MAXIMUM", projectionDefine);
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER, fragShaderString));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
