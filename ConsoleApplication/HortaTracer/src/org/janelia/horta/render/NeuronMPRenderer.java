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

import java.awt.Color;
import java.awt.geom.Point2D;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.gltools.BasicScreenBlitActor;
import org.janelia.gltools.Framebuffer;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.LightingBlitActor;
import org.janelia.gltools.MultipassRenderer;
import org.janelia.gltools.RemapColorActor;
import org.janelia.gltools.RenderPass;
import org.janelia.gltools.RenderTarget;
import org.janelia.horta.actors.SwcActor;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronMPRenderer
extends MultipassRenderer
{
    private final Framebuffer gBuffer;
    private final RenderPass hdrPass;
    private final RenderTarget hdrTarget;
    private final RenderTarget pickBuffer;
    private final RenderTarget depthTarget;
    private final float[] clearColor4 = new float[] {0,0,0,0};
    private final int[] clearColor4i = new int[] {0,0,0,0};
    private final float[] depthOne = new float[] {1};
    private final GLAutoDrawable drawable;
    // private final ColorBackgroundActor backgroundActor;
    private final BackgroundRenderPass backgroundRenderPass;
    private final OpaqueRenderPass opaqueRenderPass;
    
    public NeuronMPRenderer(GLAutoDrawable drawable, final BrightnessModel brightnessModel) 
    {
        this.drawable = drawable;
        
        backgroundRenderPass = new BackgroundRenderPass();
        add(backgroundRenderPass);
        
        // CMB September 2015 begin work on opaque render pass
        opaqueRenderPass = new OpaqueRenderPass(drawable);
        add(opaqueRenderPass);
        
        // Create G-Buffer for deferred rendering
        final int hdrAttachment = GL3.GL_COLOR_ATTACHMENT0;
        final int depthAttachment = GL3.GL_DEPTH_ATTACHMENT;
        final int pickAttachment = GL3.GL_COLOR_ATTACHMENT1;
        
        gBuffer = new Framebuffer(drawable);
        hdrTarget = gBuffer.addRenderTarget(
                GL3.GL_RGBA16,
                hdrAttachment
        );
        pickBuffer = gBuffer.addRenderTarget(
                GL3.GL_RG16UI,
                pickAttachment
        );
        depthTarget = gBuffer.addRenderTarget(
                // warning: using naked "GL_DEPTH_COMPONENT" without a size result in GL_INVALID_ENUM error, after glTexStorage2D(...)
                // warning: using naked "GL_DEPTH_COMPONENT16" results in an error at glClear...
                GL3.GL_DEPTH_COMPONENT24, // 16? 24? 32? // 16 does not work; unspecified does not work
                depthAttachment);
        
        // TODO - Opaque pass
        
        // 2) Second pass: volume intensities to hdr buffer
        hdrPass = new RenderPass(gBuffer) 
        {
            private int targetAttachments[];
            
            {
                addRenderTarget(hdrTarget);
                addRenderTarget(pickBuffer);
                targetAttachments = new int[renderTargets.size()];
                for (int rt = 0; rt < renderTargets.size(); ++rt)
                    targetAttachments[rt] = renderTargets.get(rt).getAttachment();
            }
            
            @Override
            protected void renderScene(GL3 gl, AbstractCamera camera)
            {
                // if (true) return; // TODO
                if (! hdrTarget.isDirty())
                    return;
                
                gl.glDrawBuffers(targetAttachments.length, targetAttachments, 0);

                gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor4, 0);
                gl.glClearBufferfv(GL3.GL_DEPTH, 0, depthOne, 0);
                gl.glClearBufferuiv(GL3.GL_COLOR, 1, clearColor4i, 0); // pick buffer...

                // Blend intensity channel, but not pick channel
                gl.glDisablei(GL3.GL_BLEND, 0); // TODO
                gl.glDisablei(GL3.GL_BLEND, 1); // TODO - how to write pick for BRIGHTER image?
                
                super.renderScene(gl, camera);
                
                for (RenderTarget rt : new RenderTarget[] {hdrTarget, pickBuffer}) 
                {
                    rt.setHostBufferNeedsUpdate(true);
                    rt.setDirty(false);
                }
                gl.glDrawBuffers(1, targetAttachments, 0);
            }
        };

        add(hdrPass);
        
        // 2.5 blit opaque geometry to screen
        add(new RenderPass(null) {
            { // constructor
                addActor(new BasicScreenBlitActor(opaqueRenderPass.getColorTarget()));
            }
        });
        
        // 3) Colormap volume onto screen
        add(new RenderPass(null) { // render to screen
            private GL3Actor lightingActor = new LightingBlitActor(hdrTarget); // for isosurface
            private final GL3Actor colorMapActor = new RemapColorActor(hdrTarget, brightnessModel); // for MIP, occluding

            {
                // addActor(lightingActor); // TODO - use for isosurface
                addActor(colorMapActor); // Use for MIP and occluding
                // lightingActor.setVisible(false);
                // colorMapActor.setVisible(true);
                // addActor(new BasicScreenBlitActor(hdrTarget));
            }
            
            @Override
            protected void renderScene(GL3 gl, AbstractCamera camera) {
                gl.glEnable(GL3.GL_BLEND);
                gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
                super.renderScene(gl, camera);
            }
        });
        
    }
    
    public void addVolumeActor(GL3Actor boxMesh) {
        hdrPass.addActor(boxMesh);
        setIntensityBufferDirty();
    }
    
    public void clearVolumeActors() {
        hdrPass.clearActors();
        setIntensityBufferDirty();
    }
    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
        gBuffer.init(gl);
        hdrPass.init(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        hdrPass.dispose(gl);
        gBuffer.dispose(gl);
        super.dispose(gl);
    }
    
    public int pickIdForScreenXy(Point2D xy) {
        return valueForScreenXy(xy, pickBuffer.getAttachment(), 0);
    }

    public int intensityForScreenXy(Point2D xy) {
        int result = valueForScreenXy(xy, hdrTarget.getAttachment(), 0);
        if (result <= 0) {
            return -1;
        }
        return result;
    }
    
    public float relativeDepthOffsetForScreenXy(Point2D xy, AbstractCamera camera) {
        float result = 0;
        int intensity = intensityForScreenXy(xy);
        if (intensity == -1) {
            return result;
        }
        if (gBuffer == null) {
            return result;
        }
        RenderTarget intensityDepthTarget = pickBuffer;
        if (intensityDepthTarget == null) {
            return result;
        }
        int relDepth = intensityDepthTarget.getIntensity(
                drawable,
                (int) xy.getX(),
                // y convention is opposite between screen and texture buffer
                intensityDepthTarget.getHeight() - (int) xy.getY(),
                1); // channel index
        result = 2.0f * (relDepth / 65535.0f - 0.5f); // range [-1,1]
        return result;
    }

    private int valueForScreenXy(Point2D xy, int glAttachment, int channel) {
        int result = -1;
        if (gBuffer == null) {
            return result;
        }
        RenderTarget target = gBuffer.getRenderTarget(glAttachment);
        if (target == null) {
            return result;
        }
        int intensity = target.getIntensity(
                drawable,
                (int) Math.round(xy.getX()),
                // y convention is opposite between screen and texture buffer
                target.getHeight() - (int) Math.round(xy.getY()),
                channel); // channel index
        return intensity;
    }
    
    public void setIntensityBufferDirty() {
        for (RenderTarget rt : new RenderTarget[] {hdrTarget, pickBuffer}) 
            rt.setDirty(true);
    }

    public Iterable<GL3Actor> getVolumeActors()
    {
        return hdrPass.getActors();
    }
    
    public void setBackgroundColor(Color topColor, Color bottomColor) {
        backgroundRenderPass.setColor(topColor, bottomColor);
    }

    public void removeOpaqueActor(GL3Actor actor)
    {
        opaqueRenderPass.removeActor(actor);
    }

    public void addOpaqueActor(SwcActor na)
    {
        opaqueRenderPass.addActor(na);
    }

}
