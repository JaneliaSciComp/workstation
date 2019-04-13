package org.janelia.horta.actors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.VolumeMipMaterial.VolumeState;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Material for tetrahedral volume rendering.
 * Unlike most materials, TetVolumeMaterial is NOT responsible for managing
 * the shader. TetVolumeActor has responsibility for the shader.
 * TetVolumeMaterial is responsible for managing the 3D texture for a volume
 * rendered block.
 * @author Christopher Bruns
 */
public class TetVolumeMaterial extends BasicMaterial
{
    private static final Set<Integer> volumeTextureInventory = new HashSet<>();
    private static final boolean doDebugInventory = false;
    
    private int volumeTextureHandle = 0;
    private final KtxData ktxData;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private IntBuffer pbos;
    private final TetVolumeActor parentActor;
    
    // Parameters for reconstructing original intensities
    private final float[] channelIntensityGammas;
    private final float[] channelIntensityScales;
    private final float[] channelIntensityOffsets;
    private final float voxelResolution;

    public TetVolumeMaterial(KtxData ktxData, TetVolumeActor parentActor) {
        this.ktxData = ktxData;
        shaderProgram = parentActor.getShader();
        this.parentActor = parentActor;
        
        this.voxelResolution = Float.parseFloat(ktxData.header.keyValueMetadata.get("nominal_resolution"));
        
        // Parse parameters for reconstructing the original 16-bit intensities
        int channel_count = 2; // TODO: compute channel count from gl_format
        if (ktxData.header.keyValueMetadata.containsKey("number_of_channels")) 
            channel_count = Integer.parseInt(ktxData.header.keyValueMetadata.get("number_of_channels").trim());
        channelIntensityGammas = new float[channel_count];
        channelIntensityScales = new float[channel_count];
        channelIntensityOffsets = new float[channel_count];
        for (int c = 0; c < channel_count; ++c) {
            float gamma = 0.0f;
            float scale = 1.0f;
            float offset = 0.0f;
            if (ktxData.header.keyValueMetadata.containsKey("channel_"+c+"_intensity_gamma")) {
                gamma = Float.parseFloat(ktxData.header.keyValueMetadata.get("channel_"+c+"_intensity_gamma").trim());
                scale = Float.parseFloat(ktxData.header.keyValueMetadata.get("channel_"+c+"_intensity_scale").trim());
                offset = Float.parseFloat(ktxData.header.keyValueMetadata.get("channel_"+c+"_intensity_offset").trim());
            }
            channelIntensityGammas[c] = gamma;
            channelIntensityScales[c] = scale;
            channelIntensityOffsets[c] = offset;
        }
        
    }

    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) 
    {
        gl.glUniform2fv(8, 1, channelIntensityGammas, 0);
        gl.glUniform2fv(9, 1, channelIntensityScales, 0);
        gl.glUniform2fv(10, 1, channelIntensityOffsets, 0);
        
        float screenResolution = 
                camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        float levelOfDetail = -(float)( 
                Math.log(voxelResolution / screenResolution) 
                / Math.log(2.0) );
        // Performance/Quality tradeoff: adjust to taste; 0.5f matches automatic lod
        levelOfDetail += 
                // 2f;  
                1f;
                // 0.5f; 
        levelOfDetail = Math.max(levelOfDetail, 0); // hard minimum
        // levelOfDetail = (float)Math.floor(levelOfDetail); // convert to int
        // int intLod = (int) levelOfDetail;
        // System.out.println("Computed level of detail = "+levelOfDetail);
        gl.glUniform1f(15, levelOfDetail);
        
        mesh.displayTriangleAdjacencies(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        if (volumeTextureHandle != 0) {
            if (doDebugInventory) {
                logger.info("deleting volume texture {}", volumeTextureHandle);
                volumeTextureInventory.remove(volumeTextureHandle);
                logger.info("volume texture inventory now contains {} textures", volumeTextureInventory.size());
            }
            gl.glDeleteTextures(1, new int[] {volumeTextureHandle}, 0);
            volumeTextureHandle = 0;
        }
        if ((pbos != null) && (pbos.capacity() > 0)) {
            gl.glDeleteBuffers(pbos.capacity(), pbos);
            pbos = IntBuffer.allocate(0);
        }
        super.dispose(gl);
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }
    
    private static int mipmapSize(long level, long baseSize) {
        int result = (int)Math.max(1, Math.floor(baseSize/(Math.pow(2,level))));
        return result;
    }
    
    @Override
    public void init(GL3 gl) 
    {
        super.init(gl);
        
        if (volumeTextureHandle == 0) {
            // Volume texture
            int[] h = {0};
            gl.glGenTextures(1, h, 0);
            volumeTextureHandle = h[0];
            if (doDebugInventory) {
                logger.info("adding volume texture {}", volumeTextureHandle);
                volumeTextureInventory.add(volumeTextureHandle);
                logger.info("volume texture inventory now contains {} textures", volumeTextureInventory.size());
                // todo: tetvolumeactor child count
            }
        }

        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);

        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1); // TODO: Verify that this fits data

        if (ktxData.header.byteOrder == ByteOrder.nativeOrder()) {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_FALSE);
        }
        else {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_TRUE);
        }
        /*  */
        
        int glInternalFormat = ktxData.header.glInternalFormat;
        // Work around problem with my initial 2-channel KTX files...
        // Future versions should be OK and won't need this hack.
        if (glInternalFormat == GL3.GL_RG16UI)
            glInternalFormat = GL3.GL_RG16;
        if (glInternalFormat == GL3.GL_RG8UI)
            glInternalFormat = GL3.GL_RG8;
        
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_R, GL3.GL_CLAMP_TO_EDGE);

        // Use pixel buffer objects for asynchronous transfer
        
        // Phase 1: Allocate pixel buffer objects (in GL thread)
        final boolean usePixelBufferObjects = false; // false is faster
        long t0 = System.nanoTime();
        long t1 = t0;
        if (usePixelBufferObjects) {
            int mapCount = ktxData.header.numberOfMipmapLevels;
            pbos = IntBuffer.allocate(mapCount);
            gl.glGenBuffers(mapCount, pbos);
            for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
            {
                ByteBuffer buf1 = ktxData.mipmaps.get(mipmapLevel);
                buf1.rewind();
                gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, pbos.get(mipmapLevel));
                gl.glBufferData(GL3.GL_PIXEL_UNPACK_BUFFER, buf1.capacity(), buf1, GL3.GL_STREAM_DRAW);
            }
            t1 = System.nanoTime();
            logger.info("Creating pixel buffer objects took "+(t1-t0)/1.0e9+" seconds");
        }
        
        final boolean useStorageSubimage = true; // true is much faster
        if (useStorageSubimage) {
            gl.glTexStorage3D(GL3.GL_TEXTURE_3D,
                    ktxData.header.numberOfMipmapLevels,
                    glInternalFormat,
                    ktxData.header.pixelWidth,
                    ktxData.header.pixelHeight,
                    ktxData.header.pixelDepth);
            t1 = System.nanoTime();
            // logger.info("Allocating texture storage took "+(t1-t0)/1.0e9+" seconds");
        }

        // Phase 2: Initiate loading of texture to GPU (in GL thread)
        for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
        {
            // logger.info("GL Error: " + gl.glGetError());
            int mw = mipmapSize(mipmapLevel, ktxData.header.pixelWidth);
            int mh = mipmapSize(mipmapLevel, ktxData.header.pixelHeight);
            int md = mipmapSize(mipmapLevel, ktxData.header.pixelDepth);
            if (usePixelBufferObjects) {
                gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, pbos.get(mipmapLevel));
                gl.glTexImage3D(
                        GL3.GL_TEXTURE_3D,
                        mipmapLevel,
                        glInternalFormat,
                        mw,
                        mh,
                        md,
                        0, // border
                        ktxData.header.glFormat,
                        ktxData.header.glType,
                        0); // zero means read from PBO
            }
            else {
                ByteBuffer buf1 = ktxData.mipmaps.get(mipmapLevel);
                buf1.rewind();
                if (useStorageSubimage) {
                    gl.glTexSubImage3D(
                            GL3.GL_TEXTURE_3D,
                            mipmapLevel,
                            0, 0, 0,// offsets
                            mw, mh, md,
                            ktxData.header.glFormat,
                            ktxData.header.glType,
                            buf1);
                }
                else {
                    gl.glTexImage3D(
                            GL3.GL_TEXTURE_3D,
                            mipmapLevel,
                            glInternalFormat,
                            mw,
                            mh,
                            md,
                            0, // border
                            ktxData.header.glFormat,
                            ktxData.header.glType,
                            buf1);
                }
            }
        }
        gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, 0);
        long t2 = System.nanoTime();
        // logger.info("Uploading tetrahedral volume texture to GPU took "+(t2-t1)/1.0e9+" seconds");
        
        // Phase 3: Use the texture in draw calls, after some delay... TODO:
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        // 3D volume texture
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);
        if (parentActor.getVolumeState().filteringOrder == VolumeState.FILTER_NEAREST) {
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST_MIPMAP_NEAREST);
        }
        else { // trilinear or tricubic
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_LINEAR);            
        }
    }

    @Override
    public boolean usesNormals() {
        return false;
    }

    // The singleton instance of the TetVolumeShader class is actually managed
    // by the TetVolumeActor class.
    public static class TetVolumeShader extends BasicShaderProgram
    {
        public TetVolumeShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
