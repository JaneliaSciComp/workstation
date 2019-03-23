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

/**
 *
 * @author cmbruns
 */
public class OrthographicCamera extends AbstractCamera 
{

    public OrthographicCamera(Vantage vantage, Viewport viewport) {
        super(vantage, viewport);
    }
    
    @Override
    protected void updateProjectionMatrix() {
        float halfSize = 0.5f * vantage.getSceneUnitsPerViewportHeight();
        float top = halfSize;
        float right = halfSize * viewport.getAspect();
        float depth = top * 2;
        // System.out.println("top = "+top);
        ConstViewSlab slab = getEffectiveViewSlab();
        projectionMatrix.makeOrthographic(
                -right, right,
                -top, top,
                (1.0f/slab.getzNearRelative()) * depth, // near, positive
                (1.0f - slab.getzFarRelative()) * depth); // far, negative
        projectionMatrixNeedsUpdate = false;
    }
    
    @Override
    protected void updateViewMatrix() {
        float[] f = vantage.getFocusPosition().toArray();
        float[] R = vantage.getRotationInGround().asArray();
        Vector3 foc = new Vector3(vantage.getFocusPosition());
        viewMatrix.identity();
        viewMatrix.multiply(new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                -f[0], -f[1], -f[2], 1));
        viewMatrix.multiply(new Matrix4(
                R[0], R[1], R[2], 0,
                R[3], R[4], R[5], 0,
                R[6], R[7], R[8], 0,
                0, 0, 0, 1
        ));
        viewMatrixNeedsUpdate = false;
    }
}
