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
package org.janelia.gltools.material;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture3d;
import org.openide.util.Exceptions;

/**
 * Renders 3D texture on polygons at texture coordinate
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class VolumeSurfaceMaterial extends BasicMaterial 
{
    private final Texture3d volumeTexture;
    private int volumeTextureIndex = 0;
    
    public VolumeSurfaceMaterial(Texture3d volumeTexture) {
        this.volumeTexture = volumeTexture;
        shaderProgram = new VolumeSurfaceShader();
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        volumeTexture.dispose(gl);
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        int textureUnit = 0;
        volumeTexture.bind(gl, textureUnit);
        gl.glUniform1i(volumeTextureIndex, textureUnit);
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        volumeTexture.unbind(gl); // restore depth buffer writes
    }

    @Override
    public boolean usesNormals() {
        return false;
    }
    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
        volumeTexture.init(gl);
        volumeTextureIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "volumeTexture");
    }
    
    private static class VolumeSurfaceShader extends BasicShaderProgram {
        public VolumeSurfaceShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeSurfaceVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeSurfaceFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
}
