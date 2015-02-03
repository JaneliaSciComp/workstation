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
import java.util.Collection;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Object3d;
import org.janelia.geometry3d.ScreenQuadMesh;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.Material;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class BasicScreenBlitActor implements GL3Actor
{
    protected final Texture2d screenTexture;
    protected final GL3Actor actor;
    protected final Material material;

    public BasicScreenBlitActor(Texture2d screenTexture)
    {
        this.screenTexture = screenTexture;
        MeshGeometry blitMesh = new ScreenQuadMesh();
        material = createMaterial();
        actor = new MeshActor(blitMesh, material, null);
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix)
    {
        screenTexture.bind(gl);
        gl.glDisable(GL3.GL_DEPTH_TEST);
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA); // standard transparency blending
        actor.display(gl, camera, parentModelViewMatrix);
        screenTexture.unbind(gl);
    }

    protected Material createMaterial() {
        BasicMaterial blitMaterial = new BlitMaterial();
        blitMaterial.setShaderProgram(createShaderProgram());
        blitMaterial.setShadingStyle(Material.Shading.FLAT);
        return blitMaterial;
    }
    
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
                                            + "RenderPassThroughFrag.glsl"))
            );
        } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
        }  
        return blitProgram;
    }
    
    static protected class BlitMaterial extends BasicMaterial 
    {
        private int upstreamImageIndex = 0;
        
        @Override
        public boolean usesNormals() {
            return false;
        }

        @Override
        public void init(GL3 gl) {
            super.init(gl);
            upstreamImageIndex = gl.glGetUniformLocation(
                shaderProgram.getProgramHandle(),
                "upstreamImage");
        }

        @Override
        public void load(GL3 gl, AbstractCamera camera) {
            super.load(gl, camera);
            int textureUnit = 0;
            gl.glUniform1i(upstreamImageIndex, textureUnit);
        }
    }

    @Override
    public Object3d getParent()
    {
        return actor.getParent();
    }

    @Override
    public Object3d setParent(Object3d parent)
    {
        actor.setParent(parent);
        return this;
    }

    @Override
    public Collection<? extends Object3d> getChildren()
    {
        return actor.getChildren();
    }

    @Override
    public CompositeObject3d addChild(Object3d child)
    {
        actor.addChild(child);
        return this;
    }

    @Override
    public Matrix4 getTransformInWorld()
    {
        return actor.getTransformInWorld();
    }

    @Override
    public Matrix4 getTransformInParent()
    {
        return actor.getTransformInParent();
    }

    @Override
    public boolean isVisible()
    {
        return actor.isVisible();
    }

    @Override
    public Object3d setVisible(boolean isVisible)
    {
        actor.setVisible(isVisible);
        return this;
    }

    @Override
    public String getName()
    {
        return actor.getName();
    }

    @Override
    public Object3d setName(String name)
    {
        actor.setName(name);
        return this;
    }

    @Override
    public void dispose(GL3 gl)
    {
        actor.dispose(gl);
    }

    @Override
    public void init(GL3 gl)
    {
        actor.init(gl);
    }
    
}
