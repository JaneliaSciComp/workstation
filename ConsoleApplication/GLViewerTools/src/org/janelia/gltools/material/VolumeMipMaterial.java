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
import org.apache.commons.io.IOUtils;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.VolumeTextureMesh;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture3d;
import org.openide.util.Exceptions;

/**
 * Renders 3D texture on polygons at texture coordinate
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class VolumeMipMaterial extends BasicMaterial 
{
    private final Texture3d volumeTexture;
    private int volumeTextureIndex = -1;
    private int cameraPositionInTextureCoordinatesIndex = -1;
    private int levelOfDetailIndex = -1;
    private int nearSlabPlaneIndex = -1;
    private int farSlabPlaneIndex = -1;
    private int opacityFunctionMinIndex = -1;
    private int opacityFunctionMaxIndex = -1;
    private int volumeMicrometersIndex = -1;
    private int tcToCameraIndex = -1;
    
    private final BrightnessModel colorMap;
    
    private int filteringOrder = 1;  // 0: NEAREST; 1: TRILINEAR; 2: <not used> 3: TRICUBIC
    private int filteringOrderIndex = -1;
    
    private int projectionMode = 0; // 0: Maximum intensity projection; 1: Occluding
    // private int projectionModeIndex = -1;
    
    protected final ShaderProgram mipShader;
    protected final ShaderProgram occShader;
    protected final ShaderProgram isoShader;
    private boolean uniformIndicesAreDirty = true;
    
    public VolumeMipMaterial(Texture3d volumeTexture, BrightnessModel colorMap) 
    {
        this.colorMap = colorMap;
        this.volumeTexture = volumeTexture;
        this.volumeTexture.setGenerateMipmaps(true);
        // this.volumeTexture.setMinFilter(GL3.GL_NEAREST_MIPMAP_NEAREST);
        this.volumeTexture.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
        this.volumeTexture.setMagFilter(GL3.GL_LINEAR);
        
        mipShader = new VolumeMipShader(0);
        occShader = new VolumeMipShader(1);
        isoShader = new VolumeMipShader(2);
        
        shaderProgram = mipShader;
        
        setShadingStyle(Shading.FLAT);
    }

    @Override
    protected void activateCull(GL3 gl) {
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_FRONT);
    }
    
    public float getViewSlabThickness(AbstractCamera camera) {
        // Clip on slab, to limit depth of rendering
        float screenResolution = 
                camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        float slabThickness = 300.0f * screenResolution; // micrometers
        slabThickness = Math.max(25.0f, slabThickness);
        return slabThickness;
    }

    public int getFilteringOrder() {
        return filteringOrder;
    }

    public void setFilteringOrder(int filteringOrder) {
        this.filteringOrder = filteringOrder;
    }

    public int getProjectionMode() {
        return projectionMode;
    }

    public void setProjectionMode(int projectionMode) {
        if (this.projectionMode == projectionMode)
            return;
        this.projectionMode = projectionMode;
        if (projectionMode == 0)
            shaderProgram = mipShader;
        else if (projectionMode == 1)
            shaderProgram = occShader;
        else
            shaderProgram = isoShader;
        uniformIndicesAreDirty = true;
    }
    
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) 
    {
        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);
        // Pass camera position, in texture coordinates, to shader
        if (mesh.getGeometry() instanceof VolumeTextureMesh) {
            // Pass in camera position in texture coordinates
            // Use inverse-Kane notation, since opengl matrices, such as Matrix4, are row major
            VolumeTextureMesh mg = (VolumeTextureMesh)mesh.getGeometry();
            Matrix4 world_X_tc = mg.getTransformWorldToTexCoord(); // OK
            // System.out.println("world_X_tc = "+world_X_tc);
            // System.out.println("modelViewMatrix = "+modelViewMatrix);
            Matrix4 camera_X_world = modelViewMatrix.inverse();
            // System.out.println("camera_X_world = "+camera_X_world);
            Matrix4 camera_X_tc = new Matrix4(camera_X_world).multiply(world_X_tc); // OK
            // System.out.println("camera_X_tc = "+camera_X_tc);
            Vector4 tc_camera = camera_X_tc.multiply( new Vector4(0, 0, 0, 1) );
            // System.out.println("camera position in texture coordinates = "+tc_camera); // OK
            float[] arr = tc_camera.toArray();
            gl.glUniform3fv(cameraPositionInTextureCoordinatesIndex, 1, arr, 0);
            
            // Also pass in level-of-detail
            float meshResolution = mg.getMinResolution();
            float screenResolution = 
                    camera.getVantage().getSceneUnitsPerViewportHeight()
                    / camera.getViewport().getHeightPixels();
            float levelOfDetail = -(float)( 
                    Math.log(meshResolution / screenResolution) 
                    / Math.log(2.0) );
            // Performance/Quality tradeoff: adjust to taste; 0.5f matches automatic lod
            levelOfDetail += 0.5f; 
            levelOfDetail = Math.max(levelOfDetail, 0); // hard minimum
            levelOfDetail = (float)Math.floor(levelOfDetail); // convert to int
            int intLod = (int) levelOfDetail;
            // System.out.println("Computed level of detail = "+levelOfDetail);
            gl.glUniform1i(levelOfDetailIndex, intLod);
            
            // Clip on slab, to limit depth of rendering
            float slabThickness = getViewSlabThickness(camera);
            float cameraFocusDistance = 0.0f;
            if (camera instanceof PerspectiveCamera) {
                PerspectiveCamera pc = (PerspectiveCamera) camera;
                cameraFocusDistance = pc.getCameraFocusDistance();
            }
            // Plane equation is easy to express in camera frame
            Vector4 nearSlabPlane_camera = new Vector4(0, 0, 1, 
                    cameraFocusDistance - 0.5f*slabThickness);
            Vector4 farSlabPlane_camera = new Vector4(0, 0, 1, 
                    cameraFocusDistance + 0.5f*slabThickness);
            // But we need plane equation in texture coordinate frame
            Matrix4 planeXform = camera_X_tc.inverse().transpose(); // look it up...
            Vector4 nearSlabPlane_tc = planeXform.multiply( nearSlabPlane_camera );
            Vector4 farSlabPlane_tc = planeXform.multiply( farSlabPlane_camera );
            // System.out.println("plane equation texCoord = "+nearSlabPlane_tc);
            gl.glUniform4fv(nearSlabPlaneIndex, 1, nearSlabPlane_tc.toArray(), 0);
            gl.glUniform4fv(farSlabPlaneIndex, 1, farSlabPlane_tc.toArray(), 0);
            
            // Brightness/Contrast
            // float [] oc = new float[] {0, 0, 1};
            // TODO - make use of alpha channel, which gets set to "1.0" for images with less than 4 channels
            float [] opMin = new float[] {0, 0, 0, 1};
            float [] opMax = new float[] {1, 1, 1, 2};
            if (colorMap != null) {
                for (int i = 0; i < 3; ++i) {
                    opMin[i] = colorMap.getMinimum();
                    opMax[i] = colorMap.getMaximum();
                }
            }
            gl.glUniform4fv(opacityFunctionMinIndex, 1, opMin, 0);
            gl.glUniform4fv(opacityFunctionMaxIndex, 1, opMax, 0);

            Vector4 micrometerVolumes = world_X_tc.multiply(new Vector4(1, 1, 1, 0));
            float [] volMic = new float[] {
                1.0f / Math.abs(micrometerVolumes.get(0)), 
                1.0f / Math.abs(micrometerVolumes.get(1)), 
                1.0f / Math.abs(micrometerVolumes.get(2))};
            gl.glUniform3fv(volumeMicrometersIndex, 1, volMic, 0);
            
            // for isosurface, we need to convert normals from texCoords to camera
            gl.glUniformMatrix4fv(tcToCameraIndex, 1, false, camera_X_tc.inverse().asArray(), 0);
        }
        super.displayMesh(gl, mesh, camera, modelViewMatrix);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        volumeTexture.dispose(gl);
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        
        if (uniformIndicesAreDirty)
            updateUniformIndices(gl);

        if (filteringOrder <= 0) {
            volumeTexture.setMagFilter(GL3.GL_NEAREST);
            volumeTexture.setMinFilter(GL3.GL_NEAREST_MIPMAP_NEAREST);
        }
        else {
            // Both TRILINEAR and TRICUBIC filtering use hardware trilinear filtering as a basis
            // The distinction is made in the shader
            volumeTexture.setMagFilter(GL3.GL_LINEAR);
            volumeTexture.setMinFilter(GL3.GL_LINEAR_MIPMAP_NEAREST);
        }
        gl.glUniform1i(filteringOrderIndex, filteringOrder);
        // gl.glUniform1i(projectionModeIndex, projectionMode);

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
        updateUniformIndices(gl);
        volumeTexture.init(gl);
    }
    
    private void updateUniformIndices(GL3 gl) {
        int s = shaderProgram.getProgramHandle();

        cameraPositionInTextureCoordinatesIndex = gl.glGetUniformLocation(s,
            "camPosInTc");
        volumeTextureIndex = gl.glGetUniformLocation(s, "volumeTexture");
        levelOfDetailIndex = gl.glGetUniformLocation(s, "levelOfDetail");
        nearSlabPlaneIndex = gl.glGetUniformLocation(s, "nearSlabPlane");
        farSlabPlaneIndex = gl.glGetUniformLocation(s, "farSlabPlane");
        opacityFunctionMinIndex = gl.glGetUniformLocation(s, "opacityFunctionMin");
        opacityFunctionMaxIndex = gl.glGetUniformLocation(s, "opacityFunctionMax");
        volumeMicrometersIndex = gl.glGetUniformLocation(s, "volumeMicrometers");
        filteringOrderIndex = gl.glGetUniformLocation(s, "filteringOrder");
        modelViewIndex = gl.glGetUniformLocation(s, "modelViewMatrix"); // -1 means no such item
        projectionIndex = gl.glGetUniformLocation(s, "projectionMatrix");
        tcToCameraIndex = gl.glGetUniformLocation(s, "tcToCamera");
        
        uniformIndicesAreDirty = false;
    }
    
    private static class VolumeMipShader extends BasicShaderProgram {
        public VolumeMipShader(int projectionMode) {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeMipVrtx.glsl"))
                );
                String projectionDefine = "#define PROJECTION_MODE " + projectionMode + "\n";
                String basicFragShaderString = IOUtils.toString(getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "VolumeMipFrag.glsl"), "UTF-8");
                // System.out.println(basicFragShaderString);
                String fragShaderString = basicFragShaderString.replace("#define PROJECTION_MODE PROJECTION_MAXIMUM", projectionDefine);
                // System.out.println(fragShaderString);
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER, fragShaderString)
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
}
