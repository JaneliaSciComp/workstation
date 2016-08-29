/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.actors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Material for tetrahedral volume rendering.
 * @author Christopher Bruns
 */
public class TetVolumeMaterial extends BasicMaterial
implements DepthSlabClipper
{
    private int volumeTextureHandle = 0;
    private final KtxData ktxData;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private IntBuffer pbos;
    private float zNearRelative = 0.10f;
    private float zFarRelative = 100.0f; // relative z clip planes
    private final float[] zNearFar = new float[] {0.1f, 100.0f}; // absolute clip for shader

    public TetVolumeMaterial(KtxData ktxData) {
        this.ktxData = ktxData;
        shaderProgram = new TetVolumeShader();
    }
    
    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        mesh.displayTriangleAdjacencies(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        gl.glDeleteTextures(1, new int[] {volumeTextureHandle}, 0);
        volumeTextureHandle = 0;
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
        
        // Volume texture
        int[] h = {0};
        gl.glGenTextures(1, h, 0);
        volumeTextureHandle = h[0];
        
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);

        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1); // TODO: Verify that this fits data
        
        // TODO: Test and verify endian parity behavior
        /*
        if (ktxData.header.byteOrder == ByteOrder.LITTLE_ENDIAN) {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_TRUE);
        }
        else {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_FALSE);
        }
         */
        
        /* 
        gl.glTexStorage3D(GL3.GL_TEXTURE_3D, 
                ktxData.header.numberOfMipmapLevels, 
                GL3.GL_R8UI, // ktxData.header.glInternalFormat, 
                ktxData.header.pixelWidth,
                ktxData.header.pixelHeight,
                ktxData.header.pixelDepth);
                */
        
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_R, GL3.GL_CLAMP_TO_EDGE);

        // Use pixel buffer objects for asynchronous transfer
        
        // Phase 1: Allocate pixel buffer objects (in GL thread)
        long t0 = System.nanoTime();
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
        long t1 = System.nanoTime();
        logger.info("Creating pixel buffer objects took "+(t1-t0)/1.0e9+" seconds");
        
        // Phase 2: Initiate loading of texture to GPU (in GL thread)
        for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
        {
            // logger.info("GL Error: " + gl.glGetError());
            int mw = mipmapSize(mipmapLevel, ktxData.header.pixelWidth);
            int mh = mipmapSize(mipmapLevel, ktxData.header.pixelHeight);
            int md = mipmapSize(mipmapLevel, ktxData.header.pixelDepth);
            gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, pbos.get(mipmapLevel));
            gl.glTexImage3D(
                    GL3.GL_TEXTURE_3D,
                    mipmapLevel,
                    ktxData.header.glBaseInternalFormat,
                    mw,
                    mh,
                    md,
                    0, // border
                    ktxData.header.glFormat,
                    ktxData.header.glType,
                    0); // zero means read from PBO
        }
        gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, 0);
        long t2 = System.nanoTime();
        logger.info("Uploading tetrahedral volume texture to GPU took "+(t2-t1)/1.0e9+" seconds");
        
        // Phase 3: Use the texture in draw calls, after some delay... TODO:
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        // 3D volume texture
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);
        // Z-clip planes
        float focusDistance = ((PerspectiveCamera)camera).getCameraFocusDistance();
        zNearFar[0] = zNearRelative * focusDistance;
        zNearFar[1] = zFarRelative * focusDistance;
        final int zNearFarUniformIndex = 2; // explicitly set in shader
        gl.glUniform2fv(zNearFarUniformIndex, 1, zNearFar, 0);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        // TODO: not yet used
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        zNearRelative = zNear;
        zFarRelative = zFar;
    }
    
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
