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
package org.janelia.scenewindow.stereo;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.LateralOffsetCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Viewport;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class SideBySideRenderer implements StereoRenderer {
    private PerspectiveCamera monoCamera = null;
    private AbstractCamera leftCamera = null;
    private AbstractCamera rightCamera = null;
    private final float ipdPixels = 120.0f; // TODO - rational choice of stereo parameters

    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        if (renderer.getCamera() != monoCamera) {
            monoCamera = (PerspectiveCamera) renderer.getCamera();
            leftCamera  = new LateralOffsetCamera(monoCamera, 
                +0.5f * ipdPixels,
                0, 0, 0.5f, 1.0f);
            rightCamera = new LateralOffsetCamera(monoCamera, 
                -0.5f * ipdPixels,
                0.5f, 0, 0.5f, 1.0f);
        }
        
        GL3 gl = glDrawable.getGL().getGL3();
        gl.glEnable(GL.GL_SCISSOR_TEST);
        
        // Left eye in left half
        Viewport v = leftCamera.getViewport();
        gl.glScissor(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        if (swapEyes)
            renderer.renderScene(gl, rightCamera);
        else
            renderer.renderScene(gl, leftCamera);            

        // Right eye in right half
        v = rightCamera.getViewport();
        gl.glScissor(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        if (swapEyes)
            renderer.renderScene(gl, leftCamera);
        else
            renderer.renderScene(gl, rightCamera);            

        // Restore default state
        v = monoCamera.getViewport();
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }
    
}
