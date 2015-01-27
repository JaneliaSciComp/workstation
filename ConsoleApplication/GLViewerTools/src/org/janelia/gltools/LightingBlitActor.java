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

package org.janelia.gltools;

import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.Material;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class LightingBlitActor extends BasicScreenBlitActor
{

    public LightingBlitActor(Texture2d screenTexture)
    {
        super(screenTexture);
    }
    
    @Override
    protected ShaderProgram createShaderProgram() 
    {
         // Simple shader to blit color buffer to screen
        BasicShaderProgram blitProgram = new BasicShaderProgram() {};
        try {
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_VERTEX_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "RenderPassVrtx.glsl"))
            );
            blitProgram.getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_FRAGMENT_SHADER, 
                        getClass().getResourceAsStream(
                                    "/org/janelia/gltools/material/shader/"
                                            + "LightingFrag.glsl"))
            );
        } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
        }  
        return blitProgram;
    }
    
    @Override
    protected Material createMaterial() {
        BasicMaterial blitMaterial = new BlitMaterial();
        blitMaterial.setShaderProgram(createShaderProgram());
        blitMaterial.setShadingStyle(Material.Shading.FLAT);
        return blitMaterial;
    }

}
