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

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public abstract class BasicMaterial implements Material 
{
    protected ShaderProgram shaderProgram;
    private Shading shadingStyle = Shading.SMOOTH;
    protected boolean isInitialized = false;
    protected boolean cullFaces = true;
    // Uniform transforms
    protected int modelViewIndex = -1;
    protected int projectionIndex = -1;


    public void BasicMaterial() {}
    
    @Override 
    public void display(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        load(gl, camera);
        displayWithMatrices(gl, mesh, camera, modelViewMatrix);
        unload(gl);
    }
    
    protected void displayNoMatrices(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        activateCull(gl);
        displayMesh(gl, mesh, camera, modelViewMatrix);
    }
    
    protected void activateCull(GL3 gl) {
        if (cullFaces) {
            gl.glEnable(GL3.GL_CULL_FACE);
            gl.glCullFace(GL3.GL_BACK);    
        }
        else {
            gl.glDisable(GL3.GL_CULL_FACE);            
        }        
    }
    
    protected void displayWithMatrices(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix) 
    {
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        if (modelViewMatrix == null)
            modelViewMatrix = new Matrix4(camera.getViewMatrix());
        gl.glUniformMatrix4fv(modelViewIndex, 1, false, modelViewMatrix.asArray(), 0);
        gl.glUniformMatrix4fv(projectionIndex, 1, false, projectionMatrix.asArray(), 0);        
        displayNoMatrices(gl, mesh,camera, modelViewMatrix);
    }
    
    // Override displayMesh() to display something other than triangles
    protected void displayMesh(GL3 gl, 
            MeshActor mesh,
            AbstractCamera camera,
            Matrix4 modelViewMatrix) 
    {
        mesh.displayFaces(gl);
    }

    public boolean isCullFace() {
        return cullFaces;
    }

    @Override
    public void setCullFaces(boolean doCull) {
        this.cullFaces = doCull;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }
    
    @Override
    public void init(GL3 gl) {
        if (isInitialized)
            return;
        if (shaderProgram != null)
            shaderProgram.init(gl);
        int s = shaderProgram.getProgramHandle();
        modelViewIndex = gl.glGetUniformLocation(s, "modelViewMatrix"); // -1 means no such item
        projectionIndex = gl.glGetUniformLocation(s, "projectionMatrix");
        
        isInitialized = true;
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (! isInitialized) init(gl);
        shaderProgram.load(gl);
    }

    @Override
    public void unload(GL3 gl) {
        shaderProgram.unload(gl);
        gl.glPolygonMode(GL3.GL_FRONT_AND_BACK, GL3.GL_FILL);
    }

    public void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public Shading getShadingStyle() {
        return shadingStyle;
    }

    public void setShadingStyle(Shading shadingStyle) {
        this.shadingStyle = shadingStyle;
    }

    @Override
    public int getShaderProgramHandle() {
        if (shaderProgram != null)
            return shaderProgram.getProgramHandle();
        else
            return 0;
    }

    @Override
    public boolean hasPerFaceAttributes() {
        return shadingStyle == Shading.FLAT;
    }

    @Override
    public void dispose(GL3 gl) {
        if (! isInitialized)
            return;
        if (shaderProgram != null)
            shaderProgram.dispose(gl);
        isInitialized = false;
    }

}
