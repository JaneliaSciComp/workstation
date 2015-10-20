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

package org.janelia.horta.render;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Viewport;
import org.janelia.gltools.Framebuffer;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.RenderPass;
import org.janelia.gltools.RenderTarget;
import org.janelia.gltools.texture.Texture2d;

/**
 *
 * @author Christopher Bruns
 */
public class OpaqueRenderPass extends RenderPass
{
    // TODO - eventually use deferred shading; for now we render the whole color there...
    private final RenderTarget normalMaterialTarget; // normal in RGB, material in A
    private final RenderTarget depthTarget;
    private final RenderTarget pickTarget;
    private final int targetAttachments[];
    // Clear masks for render targets
    private final float[] clearColor4 = new float[] {0,0,0,0};
    private final int[] clearColor4i = new int[] {0,0,0,0};
    private final float[] depthOne = new float[] {1};
    private Framebuffer resolvedFrameBuffer = null;
    private RenderTarget resolvedColorTarget = null;
    private RenderTarget resolvedDepthTarget = null;
    private boolean useMsaa = true;
    private float cachedZNear = 1e-2f;
    private float cachedZFar = 1e4f;
    
    private PerspectiveCamera localCamera; // local version of camera with custom slab
    private Viewport localViewport = new Viewport(); // local version of camera with custom slab
    private float slabThickness = 0.50f; // Half of view height

    public OpaqueRenderPass(GLAutoDrawable drawable)
    {
        super(new Framebuffer(drawable));
        
        // Create render targets
        if (useMsaa) {
            int num_samples = 8;
            normalMaterialTarget = framebuffer.addMsaaRenderTarget(GL3.GL_RGBA8, GL3.GL_COLOR_ATTACHMENT0, num_samples);
            depthTarget = framebuffer.addMsaaRenderTarget(GL3.GL_DEPTH_COMPONENT24, GL3.GL_DEPTH_ATTACHMENT, num_samples);
            pickTarget = framebuffer.addMsaaRenderTarget(GL3.GL_R16UI, GL3.GL_COLOR_ATTACHMENT1, num_samples);
        } else {
            normalMaterialTarget = framebuffer.addRenderTarget(GL3.GL_RGBA8, GL3.GL_COLOR_ATTACHMENT0);
            depthTarget = framebuffer.addRenderTarget(GL3.GL_DEPTH_COMPONENT24, GL3.GL_DEPTH_ATTACHMENT);
            pickTarget = framebuffer.addRenderTarget(GL3.GL_R16UI, GL3.GL_COLOR_ATTACHMENT1);
        }
        

        // Attach render targets to renderer
        addRenderTarget(normalMaterialTarget);
        addRenderTarget(pickTarget);
        targetAttachments = new int[renderTargets.size()];
        for (int rt = 0; rt < renderTargets.size(); ++rt) {
            targetAttachments[rt] = renderTargets.get(rt).getAttachment();
        }
        
        if (useMsaa) {
            resolvedFrameBuffer = new Framebuffer(drawable);
            resolvedColorTarget = resolvedFrameBuffer.addRenderTarget(GL3.GL_RGBA8, GL3.GL_COLOR_ATTACHMENT0);
            resolvedDepthTarget = resolvedFrameBuffer.addRenderTarget(GL3.GL_DEPTH_COMPONENT24, GL3.GL_DEPTH_ATTACHMENT);
        }
          
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        framebuffer.dispose(gl);
        if (useMsaa)
            resolvedFrameBuffer.dispose(gl);
    }
    
    @Override
    public void init(GL3 gl) {
        if (useMsaa)
            gl.glEnable(GL3.GL_MULTISAMPLE);
        framebuffer.init(gl);
        if (useMsaa)
            resolvedFrameBuffer.init(gl);
        super.init(gl);
    }
    
    public void setRelativeSlabThickness(float relativeThickness) {
        this.slabThickness = relativeThickness;
    }
    
    @Override
    protected void renderScene(GL3 gl, AbstractCamera camera)
    {
        // Create local copy of camera, so we can fuss with the slab thickness
        if ( (localCamera == null) || (localCamera.getVantage() != camera.getVantage()) ) {
            localCamera = new PerspectiveCamera(camera.getVantage(), localViewport);
        }
        Viewport vp = camera.getViewport();
        localViewport.setWidthPixels(vp.getWidthPixels());
        localViewport.setHeightPixels(vp.getHeightPixels());
        localViewport.setOriginXPixels(vp.getOriginXPixels());
        localViewport.setOriginYPixels(vp.getOriginYPixels());
        localViewport.setzNearRelative(vp.getzNearRelative()); // TODO
        localViewport.setzFarRelative(vp.getzFarRelative()); // TODO
        // TODO - set slab thickness and update projection
        
        float focusDistance = localCamera.getCameraFocusDistance();
        float heightInUnits = localCamera.getVantage().getSceneUnitsPerViewportHeight();
        float slabInUnits = slabThickness * heightInUnits;
        float zNear = focusDistance - 0.5f * slabInUnits;
        float zFar  = focusDistance + 0.5f * slabInUnits;
        if (zNear < 1e-3f) zNear = 1e-3f;
        if (zFar <= zNear) zFar = zNear + 1e-3f;
        float relNear = zNear/focusDistance;
        float relFar = zFar/focusDistance;
        localViewport.setzNearRelative(relNear);
        localViewport.setzFarRelative(relFar);
        
        // Store camera parameters for use by volume rendering pass
        cachedZNear = zNear;
        cachedZFar = zFar;
        
        if (useMsaa) {
            gl.glEnable(GL3.GL_MULTISAMPLE);
            gl.glEnable(GL3.GL_BLEND);
        }
        gl.glEnable(GL3.GL_DEPTH_TEST);
        // 
        gl.glDrawBuffers(targetAttachments.length, targetAttachments, 0);

        gl.glClearBufferfv(GL3.GL_DEPTH, 0, depthOne, 0);
        gl.glClearBufferuiv(GL3.GL_COLOR, 1, clearColor4i, 0); // pick buffer...
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor4, 0);

        // Blend intensity channel, but not pick channel
        gl.glDisablei(GL3.GL_BLEND, 0); // TODO
        gl.glDisablei(GL3.GL_BLEND, 1); // TODO - how to write pick for BRIGHTER image?

        super.renderScene(gl, localCamera);

        for (RenderTarget rt : new RenderTarget[] {normalMaterialTarget, pickTarget}) 
        {
            rt.setHostBufferNeedsUpdate(true);
            rt.setDirty(false);
        }
        gl.glDrawBuffers(1, targetAttachments, 0);
        
        if (useMsaa) {
            // Resolve multisampled buffers into ordinary single-sample textures
            resolvedFrameBuffer.bind(gl, GL3.GL_DRAW_FRAMEBUFFER);
            framebuffer.bind(gl, GL3.GL_READ_FRAMEBUFFER);
            int width = framebuffer.getWidth();
            int height = framebuffer.getHeight();
            gl.glBlitFramebuffer(
                    0, 0, 
                    width, height, 
                    0, 0, 
                    width, height, 
                    GL3.GL_COLOR_BUFFER_BIT, GL3.GL_NEAREST);
            gl.glBlitFramebuffer(
                    0, 0, 
                    width, height, 
                    0, 0, 
                    width, height, 
                    GL3.GL_DEPTH_BUFFER_BIT, GL3.GL_NEAREST);
        }
    }

    public Texture2d getColorTarget()
    {
        if (useMsaa)
            return resolvedColorTarget;
        else
            return normalMaterialTarget;
    }
    
    public Texture2d getDepthTarget()
    {
        if (useMsaa)
            return resolvedDepthTarget;
        else
            return depthTarget;
    }
    
    public float getZNear() {
        return cachedZNear;
    }
    
    public float getZFar() {
        return cachedZFar;
    }

}
