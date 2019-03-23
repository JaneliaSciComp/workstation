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
 * Advanced wireframe representation, using geometry shader.
 * TODO - use GL_LINES instead of GL_TRIANGLES
 * TODO - adjust color and line width in API
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class WireframeMaterial extends BasicMaterial 
{
    private float lineWidth = 1.0f; // TODO 1.0f default
    private int lineWidthIndex = -1;
    private float[] outlineColor = new float[]{0.30f, 0.05f, 0.05f, 1.0f};
    private int colorIndex = -1;
    
    public WireframeMaterial() {
        shaderProgram = new WireframeShader();
        setShadingStyle(Shading.NONE);
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        lineWidthIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "lineWidth");
        colorIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(), 
                "outlineColor");
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        // Convert lineWidth from pixels (at focus) to gl units
        float lineWidthSceneUnits = lineWidth
                * camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        gl.glUniform1f(lineWidthIndex, lineWidthSceneUnits);
        gl.glUniform4fv(colorIndex, 1, outlineColor, 0);        
    }

    public void setOutlineColor(Color color) {
        outlineColor[0] = color.getRed()/255.0f;
        outlineColor[1] = color.getGreen()/255.0f;
        outlineColor[2] = color.getBlue()/255.0f;
        outlineColor[3] = color.getAlpha()/255.0f;
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    private static class WireframeShader extends BasicShaderProgram {

        public WireframeShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "WireframeVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "WireframeGeom.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "WireframeFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
}
