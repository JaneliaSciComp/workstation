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
public class SpheresMaterial extends BasicMaterial
{
    // shader uniform parameter handles
    private int colorIndex = -1;
    private int lightProbeIndex = -1;
    private int radiusOffsetIndex = -1;
    
    private final Texture2d lightProbeTexture;
    private final boolean manageLightProbeTexture;
    private final float[] color = new float[] {1, 0, 0, 1};
    private float minPixelRadius = 0.0f;

    public SpheresMaterial(Texture2d lightProbeTexture, ShaderProgram spheresShader) {
        if (spheresShader == null)
            shaderProgram = new SpheresShader();
        else
            shaderProgram = spheresShader;
        
        if (lightProbeTexture == null) {
            manageLightProbeTexture = true;
            this.lightProbeTexture = new Texture2d();
            try {
                this.lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "Office1W165Both.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        else {
            manageLightProbeTexture = false;
            this.lightProbeTexture = lightProbeTexture;
        }
    }

    // Simplified display() method, with no load/unload, nor matrix manipulation.
    // So this can be in fast inner loop of multi-neuron render
    @Override 
    public void display(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        if (manageLightProbeTexture) {
            lightProbeTexture.bind(gl, 0);
            gl.glUniform4fv(colorIndex, 1, color, 0);
        }
        displayMesh(gl, mesh, camera, modelViewMatrix);
    }
    
    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        mesh.displayParticles(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        colorIndex = -1;
        lightProbeIndex = -1;
        if (manageLightProbeTexture) {
            lightProbeTexture.dispose(gl);
        }
        radiusOffsetIndex = -1;
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
        if (manageLightProbeTexture)
            lightProbeTexture.init(gl);
        radiusOffsetIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "radiusOffset");
    }

    // NOTE load and unload methods are not used, due to overridden display() method.
    // This class relies on some higher authority to set up the OpenGL state correctly.
    // (such as NeuronMPRenderer.AllSwcActor)
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (colorIndex == -1) 
            init(gl);
        super.load(gl, camera);
        if (manageLightProbeTexture)
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
        if (manageLightProbeTexture)
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

    public void setMinPixelRadius(float minPixelRadius)
    {
        this.minPixelRadius = minPixelRadius;
    }

    float[] getColorArray()
    {
        return color;
    }

    float getMinPixelRadius()
    {
        return minPixelRadius;
    }
    
    public static class SpheresShader extends BasicShaderProgram
    {
        public SpheresShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
