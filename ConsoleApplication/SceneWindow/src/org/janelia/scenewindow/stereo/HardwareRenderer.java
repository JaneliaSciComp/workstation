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

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.LateralOffsetCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class HardwareRenderer implements StereoRenderer {
    // TODO - factor out stereo rig
    private PerspectiveCamera monoCamera = null;
    private AbstractCamera leftCamera = null;
    private AbstractCamera rightCamera = null;
    private final float ipdPixels = 120.0f; // TODO - rational choice of stereo parameters

    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());
        
        if (! canDisplay(glDrawable)) {
            renderer.renderScene(gl, renderer.getCamera());
            return;
        }
        
        if (renderer.getCamera() != monoCamera) {
            monoCamera = (PerspectiveCamera) renderer.getCamera();
            leftCamera  = new LateralOffsetCamera(monoCamera, +0.5f * ipdPixels);
            rightCamera = new LateralOffsetCamera(monoCamera, -0.5f * ipdPixels);
        }
        
        // Left eye in one color...
        gl.glDrawBuffer(GL2.GL_BACK_LEFT);
        if (swapEyes)
            renderer.renderScene(gl, rightCamera);
        else
            renderer.renderScene(gl, leftCamera);

        // ...Right eye, complementary color
        gl.glDrawBuffer(GL2.GL_BACK_RIGHT);
        // Clear depth buffer, but not color buffer
        gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
        if (swapEyes)
            renderer.renderScene(gl, leftCamera);
        else
            renderer.renderScene(gl, rightCamera);
        
        // Restore default color mask
        gl.glDrawBuffer(GL2.GL_BACK);
    }
    
    public static boolean canDisplay(GLDrawable glDrawable) {
            GLCapabilitiesImmutable glCaps = glDrawable.getChosenGLCapabilities();
            return glCaps.getStereo();
    }
    
}
