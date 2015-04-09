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

import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 * Offscreen buffer for rendering OpenGL images
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Framebuffer implements GL3Resource, GLEventListener {
    private int frameBufferHandle = 0;
    private int width, height;
    private final List<RenderTarget> renderTargets = new ArrayList<RenderTarget>();
    private boolean needsResize = false;
    private GLAutoDrawable target;
    
    public Framebuffer(GLAutoDrawable target) { 
        this.target = target;
        target.addGLEventListener(this);
        reshape(target.getSurfaceWidth(), target.getSurfaceHeight());
    }

    public RenderTarget addRenderTarget(int internalFormat, int attachment) {
        RenderTarget result = new RenderTarget(width, height, internalFormat, attachment);
        renderTargets.add(result);
        return result;
    }
    
    public boolean bind(GL3 gl) {
        return bind(gl, GL3.GL_FRAMEBUFFER); // default to both read and write
    }

    public boolean bind(GL3 gl, int readWrite) {
        if (frameBufferHandle == 0)
            init(gl);
        if (frameBufferHandle == 0)
            return false;

        if ( (width != target.getSurfaceWidth()) || (height != target.getSurfaceHeight()) )
            reshape(target.getSurfaceWidth(), target.getSurfaceHeight());
        
        gl.glBindFramebuffer(readWrite, frameBufferHandle);
        int framebufferStatus = gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);
        if(framebufferStatus != GL3.GL_FRAMEBUFFER_COMPLETE)
            return false; // TODO better error handling
        if (needsResize) {
            for (RenderTarget rt : renderTargets) {
                rt.reshape(gl, width, height);
                rt.bind(gl);
                gl.glFramebufferTexture(readWrite, rt.getAttachment(), 
                        rt.getHandle(), 0);
                rt.unbind(gl);
            }
            needsResize = false;
        }
        return true;
    }
    
    @Override
    public void dispose(GL3 gl) {
        if (frameBufferHandle != 0) {
            int[] vals = {frameBufferHandle};
            gl.glDeleteFramebuffers(1, vals, 0);
            frameBufferHandle = 0;
        }
        for (RenderTarget rt : renderTargets) {
            rt.dispose(gl);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RenderTarget getRenderTarget(int attachment) {
        for (RenderTarget rt : renderTargets) {
            if (rt.getAttachment() == attachment)
                return rt;
        }
        return null;
    }

    @Override
    public void init(GL3 gl) {
        // TODO - avoid premature initialization
        if (width*height == 0)
            return;
        // DebugGL3 gl = new DebugGL3(gl0);
        if (frameBufferHandle == 0) {
            int[] vals = new int[1];
            gl.glGenFramebuffers(1, vals, 0);
            frameBufferHandle = vals[0];
        }
        if (frameBufferHandle == 0) 
            return;
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBufferHandle);
        for (RenderTarget rt : renderTargets) {
            rt.reshape(gl, width, height);
            rt.init(gl);
            rt.bind(gl);
            rt.clear(gl);
            gl.glFramebufferTexture(GL3.GL_FRAMEBUFFER, rt.getAttachment(), 
                    rt.getHandle(), 0);
        }
        // TODO - parameterize draw attachments
        int[] drawBuffers = new int[] {GL3.GL_COLOR_ATTACHMENT0};
        gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
        unbind(gl);
    }

    public final boolean reshape(int w, int h) {
        // System.out.println("Framebuffer.reshape(): "+w+", "+h);
        if ( (w == width ) && (h == height) )
            return false;
        for (RenderTarget rt : renderTargets)
            rt.setDirty(true); // Forces repaint during resizing
        width = w;
        height = h;
        if (renderTargets.size() > 0)
            needsResize = true;
        // System.out.println("Framebuffer.reshape() flagged");
        return true;
    }
    
    public void unbind(GL3 gl) {
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
    }

    // GLEventListener interface...
    
    @Override
    public void init(GLAutoDrawable glad) {
        reshape(glad.getSurfaceWidth(), glad.getSurfaceHeight());
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        init(gl);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        init(gl);
        dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable glad) {
        // display is delegated to containing actor
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        if ( ! reshape(w, h) ) return;
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        // GL3 gl = glad.getGL().getGL3();
        bind(gl);
        for (RenderTarget rt : renderTargets) {
            rt.reshape(gl, w, h);
            rt.bind(gl);
            rt.clear(gl);
            gl.glFramebufferTexture(GL3.GL_FRAMEBUFFER, rt.getAttachment(), 
                    rt.getHandle(), 0);
            rt.unbind(gl);
        }
        unbind(gl);
    }
    
}
