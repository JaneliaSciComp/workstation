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
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class ImageParticleMaterial extends BasicMaterial {
    private Texture2d particleTexture;
    private int sphereTextureIndex = 0;
    private int particleScaleIndex = 0;
    
    private int diffuseColorIndex = 0;
    private int specularColorIndex = 0;
    private float[] diffuseColor = new float[] {1, 0, 1, 1};
    private float[] specularColor = new float[] {0, 0, 0, 0};    

    public ImageParticleMaterial(Texture2d particleTexture) {
        initialize(particleTexture);
    }
    
    public ImageParticleMaterial(BufferedImage particleImage) {
        particleTexture = new Texture2d();
        particleTexture.loadFromBufferedImage(particleImage);
        initialize(particleTexture);
    }
    
    private void initialize(Texture2d particleTexture) {
        shaderProgram = new ImageParticleShader();
        this.particleTexture = particleTexture;
    }

    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE); // important with my latest Windows nvidia driver 10/20/2014
        mesh.displayParticles(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        sphereTextureIndex = 0;
        particleScaleIndex = 0;
        diffuseColorIndex = 0;
        specularColorIndex = 0;
        particleTexture.dispose(gl);
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        particleTexture.init(gl);
        sphereTextureIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "sphereTexture");
        particleScaleIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "particleScale");
        diffuseColorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "diffuseColor");
        specularColorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "specularColor");       
        // System.out.println("sphereTextureIndex = "+sphereTextureIndex);
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (particleScaleIndex == 0) 
            init(gl);
        super.load(gl, camera);
        int textureUnit = 0;
        particleTexture.bind(gl, textureUnit);
        gl.glUniform1i(sphereTextureIndex, textureUnit);
        float particleScale = 1.0f; // in pixels
        if (camera instanceof PerspectiveCamera) {
            PerspectiveCamera pc = (PerspectiveCamera)camera;
            particleScale = 0.5f * pc.getViewport().getHeightPixels()
                    / (float)Math.tan(0.5 * pc.getFovRadians());
        }
        // System.out.println("Particle scale = "+particleScale);
        gl.glUniform1f(particleScaleIndex, particleScale);
        gl.glUniform4fv(diffuseColorIndex, 1, diffuseColor, 0);
        gl.glUniform4fv(specularColorIndex, 1, specularColor, 0);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    public void setDiffuseColor(Color color) {
        diffuseColor[0] = color.getRed()/255f;
        diffuseColor[1] = color.getGreen()/255f;
        diffuseColor[2] = color.getBlue()/255f;
        diffuseColor[3] = color.getAlpha()/255f;
    }

    public void setSpecularColor(Color color) {
        specularColor[0] = color.getRed()/255f;
        specularColor[1] = color.getGreen()/255f;
        specularColor[2] = color.getBlue()/255f;
        specularColor[3] = color.getAlpha()/255f;
    }

    private static class ImageParticleShader extends BasicShaderProgram {

        public ImageParticleShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "ImageParticleVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "ImageParticleFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
}
