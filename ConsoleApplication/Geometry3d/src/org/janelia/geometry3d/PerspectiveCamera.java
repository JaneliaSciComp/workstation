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
package org.janelia.geometry3d;

import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.geometry3d.camera.ConstViewport;

/**
 * PerspectiveCamera manages a projection matrix.
 * 
 * @author brunsc
 */
public class PerspectiveCamera extends AbstractCamera
{
    private float fovYRadians = 0.3f;

    public PerspectiveCamera(Vantage vantage, Viewport viewport) {
        super(vantage, viewport);
    }
    
    public float getCameraFocusDistance() {
        return 0.5f * vantage.getSceneUnitsPerViewportHeight() 
                / (float) Math.tan( 0.5 * fovYRadians );
    }
    
    @Override
    protected void updateProjectionMatrix() {
        float focusDistance = getCameraFocusDistance();
        ConstViewSlab slab = getEffectiveViewSlab();
        projectionMatrix.makePerspective(
                fovYRadians, 
                viewport.getAspect(), 
                slab.getzNearRelative() * focusDistance,
                slab.getzFarRelative() * focusDistance );
        projectionMatrixNeedsUpdate = false;
        // System.out.println("Projection matrix updated");
    }

    @Override
    protected void updateViewMatrix() {
        // Translate by focus distance
        float focusDistance = getCameraFocusDistance();

        viewMatrix.identity();
        viewMatrix.translate(new Vector3(vantage.getFocusPosition()).negate());
        
        // Hack to rescale mouse light brain images to hide anisotropic point spread function.
        Vector3 s = vantage.getWorldScaleHack();
        viewMatrix.scale(s.getX(), s.getY(), s.getZ());
        
        viewMatrix.rotate(vantage.getRotationInGround());
        viewMatrix.translate(new Vector3(0, 0, -focusDistance));
    }
    
    public float getFovRadians() {
        return fovYRadians;
    }

    public void setFovRadians(float fovYRadians) {
        if (this.fovYRadians == fovYRadians)
            return;
        this.fovYRadians = fovYRadians;
        projectionMatrixNeedsUpdate = true;
        getChangeObservable().setChanged();
    }

}
