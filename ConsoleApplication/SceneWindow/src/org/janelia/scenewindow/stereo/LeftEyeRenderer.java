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
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.camera.BasicFrustumShift;
import org.janelia.geometry3d.camera.ConstFrustumShift;
import org.janelia.geometry3d.camera.ShiftableCamera;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class LeftEyeRenderer implements StereoRenderer 
{
    private float ipdPixels = 80.0f; // TODO - rational choice of stereo parameters

    public LeftEyeRenderer(float ipdPixels) {
        this.ipdPixels = ipdPixels;
    }
    
    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) 
    {
        float shift = -0.5f * ipdPixels;
        if (swapEyes)
            shift = -shift;
        ConstFrustumShift frustumShift = new BasicFrustumShift(shift, 0);
        
        AbstractCamera camera = renderer.getCamera();
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());
        try {
            if (camera instanceof ShiftableCamera)
                ((ShiftableCamera)camera).pushFrustumShift(frustumShift);
            renderer.renderScene(gl, camera);
        }
        finally {
            if (camera instanceof ShiftableCamera)
                ((ShiftableCamera)camera).popFrustumShift();            
        }
    }
}
