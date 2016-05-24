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

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class TransparentEnvelope extends BasicMaterial
{
    private float[] diffuseColor = new float[] {1,0,1};
    private int colorIndex = -1;
    
    public TransparentEnvelope() {
        shaderProgram = new TransparentEnvelopeShader();
        setShadingStyle(Shading.SMOOTH);
        setCullFaces(false); // Show back faces
    }

    @Override
    public boolean usesNormals() {
        return true;
    }

    @Override
    public void init(GL3 gl) {
        if (isInitialized)
            return;
        super.init(gl);
        if (! isInitialized)
            return;
        int s = shaderProgram.getProgramHandle();
        colorIndex = gl.glGetUniformLocation(s, "diffuseColor");
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);
        gl.glDepthMask(false);
        gl.glUniform3fv(colorIndex, 1, diffuseColor, 0);
    }

    @Override
    public void unload(GL3 gl) {
        // gl.glDisable(GL3.GL_BLEND);
        gl.glDepthMask(true);
        super.unload(gl);
    }

    public void setDiffuseColor(Color color) {
        this.diffuseColor = color.getColorComponents(null);
    }

    private static class TransparentEnvelopeShader extends BasicShaderProgram {
        public TransparentEnvelopeShader() {
            try {
                getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "BasicMeshVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(
                        GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "TransparentEnvelopeFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
