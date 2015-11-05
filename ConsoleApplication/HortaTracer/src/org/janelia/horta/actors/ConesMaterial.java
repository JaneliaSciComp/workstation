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

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class ConesMaterial extends BasicMaterial
{
    private static final int UNINITIALIZED_UNIFORM_INDEX = -1;
    // shader uniform parameter handles
    private int colorIndex = UNINITIALIZED_UNIFORM_INDEX;
    private int lightProbeIndex = UNINITIALIZED_UNIFORM_INDEX;
    private int radiusOffsetIndex = UNINITIALIZED_UNIFORM_INDEX;
    
    private Texture2d lightProbeTexture;
    private final float[] color = new float[] {1, 0, 0, 1};
    private float minPixelRadius = 0.0f;

    public ConesMaterial() {
            shaderProgram = new ConesShader();
        try {
            lightProbeTexture = new Texture2d();
            lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                    "/org/janelia/gltools/material/lightprobe/"
                            + "Office1W165Both.ppm"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }    
    }

    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        mesh.displayEdges(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        colorIndex = UNINITIALIZED_UNIFORM_INDEX;
        lightProbeIndex = UNINITIALIZED_UNIFORM_INDEX;
        lightProbeTexture.dispose(gl);
        radiusOffsetIndex = UNINITIALIZED_UNIFORM_INDEX;
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        colorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "color");
        lightProbeIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "lightProbe");
        lightProbeTexture.init(gl);
        radiusOffsetIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "radiusOffset");
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (colorIndex == UNINITIALIZED_UNIFORM_INDEX) 
            init(gl);
        super.load(gl, camera);
        lightProbeTexture.bind(gl, 0);
        gl.glUniform4fv(colorIndex, 1, color, 0);
        gl.glUniform1i(lightProbeIndex, 0); // use default texture unit, 0
        // radius offset depends on current zoom
        float micrometersPerPixel = 
            camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        float radiusOffset = minPixelRadius * micrometersPerPixel;
        gl.glUniform1f(radiusOffsetIndex, radiusOffset);
    }
    
    @Override
    public void unload(GL3 gl) {
        super.unload(gl);
        lightProbeTexture.unbind(gl);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }
    
    public void setColor(Color color) {
        this.color[0] = color.getRed()/255f;
        this.color[1] = color.getGreen()/255f;
        this.color[2] = color.getBlue()/255f;
        this.color[3] = color.getAlpha()/255f;
        // Convert sRGB to linear-ish (RGB, but not alpha)
        for (int i = 0; i < 3; ++i)
            this.color[i] = this.color[i] * this.color[i];
    }
    
    public Color getColor()
    {
        return new Color(
                // convert linear to sRGB (but not alpha)
                (float)Math.sqrt(color[0]), 
                (float)Math.sqrt(color[1]), 
                (float)Math.sqrt(color[2]), 
                color[3]);
    }    

    void setMinPixelRadius(float minPixelRadius)
    {
        this.minPixelRadius = minPixelRadius;
    }
    
    public static class ConesShader extends BasicShaderProgram
    {
        public ConesShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
