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
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Material for tetrahedral volume rendering.
 * @author Christopher Bruns
 */
public class TetVolumeMaterial extends BasicMaterial
{
    private int volumeTextureHandle = 0;
    private final KtxData ktxData;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

        long t0 = System.nanoTime();
        for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
        {
            ByteBuffer buf = ktxData.mipmaps.get(mipmapLevel);
            buf.rewind();
            // logger.info("GL Error: " + gl.glGetError());
            gl.glTexImage3D(
                    GL3.GL_TEXTURE_3D,
                    mipmapLevel,
                    ktxData.header.glBaseInternalFormat,
                    mipmapSize(mipmapLevel, ktxData.header.pixelWidth),
                    mipmapSize(mipmapLevel, ktxData.header.pixelHeight),
                    mipmapSize(mipmapLevel, ktxData.header.pixelDepth),
                    0, // border
                    ktxData.header.glFormat,
                    ktxData.header.glType,
                    buf);
        }
        long t1 = System.nanoTime();
        logger.info("Uploading tetrahedral volume texture to GPU took "+(t1-t0)/1.0e9+" seconds");
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        // 3D volume texture
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
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
