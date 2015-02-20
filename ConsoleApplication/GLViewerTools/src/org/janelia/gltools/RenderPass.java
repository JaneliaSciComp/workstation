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
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;

/**
 * One step of a multi-pass rendering pipeline.
 * Add GL3Actors to get the desired shaders.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class RenderPass // extends BasicGL3Actor
implements GL3Resource
{
    private boolean isDirty = true;
    private boolean cacheResults = false;
    private final List<RenderPass> dependencies = new ArrayList<>();
    protected final List<RenderTarget> renderTargets = new ArrayList<>();
    protected final Framebuffer framebuffer;
    private final List<GL3Actor> actors = new ArrayList<>();

    public RenderPass(Framebuffer framebuffer) {
        this.framebuffer = framebuffer;
    }
    
    public void addRenderTarget(RenderTarget target) {
        renderTargets.add(target);
    }

    public void addDependency(RenderPass pass) {
        dependencies.add(pass);
    }
    
    protected void renderScene(GL3 gl, AbstractCamera camera) {
        for (GL3Actor actor : actors)
            if (actor.isVisible())
                actor.display(gl, camera, null);
    }
    
    public void display(GL3 gl, AbstractCamera camera) {
        if (actors.isEmpty())
            return;
        if (! needsRerun())
            return;
        if ( (framebuffer != null) && (renderTargets.size() > 0) && (framebuffer.bind(gl)) ) 
        {
            // Don't apply anaglyph colormask during off-screen rendering...
            // TODO - avoid this hack
            byte[] savedColorMask = new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
            gl.glGetBooleanv(GL3.GL_COLOR_WRITEMASK, savedColorMask, 0);
            gl.glColorMask(true, true, true, true);                
                
            renderScene(gl, camera);
            framebuffer.unbind(gl);
            if (isCacheResults())
                setNeedsRerun(false);

            gl.glColorMask(
                    savedColorMask[0] != 0,
                    savedColorMask[1] != 0,
                    savedColorMask[2] != 0,
                    savedColorMask[3] != 0);
        }
        else {
            // Render to screen if no framebuffer configured
            renderScene(gl, camera);
        }
    }

    public boolean needsRerun() {
        if (! isCacheResults())
            return true;
        if (isDirty) 
            return true;
        for (RenderPass d : dependencies) {
            if (d.needsRerun()) return true;
        }
        return false;
    }

    public boolean isCacheResults() {
        return cacheResults;
    }

    public void setCacheResults(boolean cacheResults) {
        this.cacheResults = cacheResults;
    }
    
    public RenderPass setNeedsRerun(boolean doesNeed) {
        this.isDirty = doesNeed;
        return this;
    }

    public void addActor(GL3Actor actor)
    {
        actors.add(actor);
    }
    
    public void clearActors() {
        actors.clear();
    }

    @Override
    public void dispose(GL3 gl)
    {
        for (GL3Actor actor : actors)
            actor.dispose(gl);
    }

    @Override
    public void init(GL3 gl)
    {
        for (GL3Actor actor : actors)
            actor.init(gl);
    }
    
}
