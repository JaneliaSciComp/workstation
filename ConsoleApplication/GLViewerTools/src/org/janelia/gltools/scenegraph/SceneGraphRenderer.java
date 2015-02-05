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

package org.janelia.gltools.scenegraph;

import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 *
 * @author Christopher Bruns
 * TODO - use this scene-graph based rendering approach for more scalable scene rendering
 * , as opposed to SceneRenderer
 */
public class SceneGraphRenderer implements GLEventListener
{
    // Handle for the scene graph itself
    private SceneNode rootNode;
    // Viewport objects: multiple, for use in stereo and minimap modes
    private List<RenderViewport> viewports = new ArrayList<RenderViewport>();

    // getGL3 delegate method, so we can turn debug on/off here
    private static GL3 getGL3(GLAutoDrawable drawable) {
        if (drawable == null)
            return null;
        GL gl = drawable.getGL();
        if (gl == null) 
            return null;
        GL3 gl3 = gl.getGL3();
        if (gl3 == null)
            return null;
        return new DebugGL3(gl3);
    };
    
    @Override
    public void init(GLAutoDrawable drawable)
    {
        // Use lazy initialization elsewhere (e.g. display()), rather than monolithic initialization here.
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        if (drawable == null)
            return;
        GL3 gl = getGL3(drawable);
        // TODO - what about nodes that have been deleted previously?
        new DisposeGlVisitor(gl).visit(rootNode);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL3 gl = getGL3(drawable);
        for (RenderViewport v : viewports) { // ordinariy just one viewport
            if (v.getHeightPixels() * v.getWidthPixels() < 1)
                continue; // Too small to draw anything
            // TODO - compute viewport contribution to transform
            for (CameraNode c : v.getCameras()) { // ordinarily just one camera
                // TODO - walk camera to root node, to finish View and Projection matrices
                // TODO - cull nodes based on view frustum
                // TODO - assort all Drawables into RenderPasses
                // TODO - execute RenderPasses
            }
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        for (RenderViewport v : viewports) {
            v.setHeightPixels( (int)(height*v.getHeightRelative()) );
            v.setWidthPixels( (int)(width * v.getWidthRelative()) );
            v.setOriginBottomPixels( y + (int)(height * v.getOriginBottomRelative()) );
            v.setOriginLeftPixels( x + (int)(width * v.getOriginLeftRelative()) );
        }
        // TODO - does anyone need to be notified? Or is display called automatically?
    }
    
}
