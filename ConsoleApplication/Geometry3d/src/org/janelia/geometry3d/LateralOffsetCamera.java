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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import org.janelia.geometry3d.camera.BasicFrustumShift;
import org.janelia.geometry3d.camera.ConstFrustumShift;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.geometry3d.camera.GeneralCamera;

/**
 * Left or right eye camera, based on a centered parent camera
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class LateralOffsetCamera extends PerspectiveCamera 
implements GeneralCamera
{
    private final PerspectiveCamera parentCamera;
    // private final float offsetPixels;
    private final float relX;
    private final float relY;
    private final float relWidth;
    private final float relHeight;
    private final Deque<ConstFrustumShift> shiftStack = new LinkedList<>();
    
    public LateralOffsetCamera(Vantage vantage, Viewport viewport) 
    {
        super(vantage, viewport);
        this.relX = 0f; this.relY = 0f;
        this.relWidth = 1.0f; this.relHeight = 1.0f;
        parentCamera = null;
        shiftStack.push(new BasicFrustumShift(0, 0));
    }
    
    public LateralOffsetCamera(final PerspectiveCamera parentCamera, 
            // Subset viewport
            float offsetPixels, float relX, float relY, 
            float relWidth, float relHeight) 
    {
        super(parentCamera.getVantage(), new Viewport());
        shiftStack.push(new BasicFrustumShift(offsetPixels, 0));
        // this.offsetPixels = offsetPixels;
        this.parentCamera = parentCamera;
        this.relX = relX; this.relY = relY;
        this.relWidth = relWidth; this.relHeight = relHeight;
        updateViewport();
        parentCamera.getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                updateViewport();
                setFovRadians(parentCamera.getFovRadians());
                getChangeObservable().notifyObservers(); // propagate update()
            }
        });
    }

    private void updateViewport() {
        Viewport vp0 = parentCamera.getViewport();
        viewport.setOriginXPixels(vp0.getOriginXPixels() + (int)(relX * vp0.getWidthPixels()));
        viewport.setOriginYPixels(vp0.getOriginYPixels() + (int)(relY * vp0.getHeightPixels()));
        viewport.setWidthPixels( (int)(relWidth * vp0.getWidthPixels()) );
        viewport.setHeightPixels( (int)(relHeight * vp0.getHeightPixels()) );
    }
    
    public LateralOffsetCamera(final PerspectiveCamera parentCamera, float offsetPixels) {
        super(parentCamera.getVantage(), parentCamera.getViewport());
        this.relX = 0f; this.relY = 0f;
        this.relWidth = 1.0f; this.relHeight = 1.0f;
        this.parentCamera = parentCamera;
        shiftStack.push(new BasicFrustumShift(offsetPixels, 0));
        // this.offsetPixels = offsetPixels;
        parentCamera.getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                setFovRadians(parentCamera.getFovRadians());
                getChangeObservable().notifyObservers(); // propagate update()
            }
        });
    }

    @Override
    protected void updateProjectionMatrix() 
    {
        final ConstFrustumShift shift = shiftStack.peek();
        final float eyeShiftSceneX = shift.getShiftXPixels() * vantage.getSceneUnitsPerViewportHeight() 
                / viewport.getHeightPixels();
        final float eyeShiftSceneY = shift.getShiftYPixels() * vantage.getSceneUnitsPerViewportHeight() 
                / viewport.getHeightPixels();
        final ConstViewSlab slab = getEffectiveViewSlab();
        final float focusDistance = getCameraFocusDistance();
        final float zNear = slab.getzNearRelative() * focusDistance;
        final float zFar = slab.getzFarRelative() * focusDistance;
        final float frustumShiftX = 
                eyeShiftSceneX * slab.getzNearRelative();
        final float frustumShiftY = 
                eyeShiftSceneY * slab.getzFarRelative();
                // 0.0f;
        final float top = zNear * (float)Math.tan(0.5 * getFovRadians());
        final float right = viewport.getAspect() * top;
        // The centering translation should be on modelview (view) matrix,
        // so specular reflections would adjust correctly
        // pj.translate(new Vector3(eyeShiftScene, 0, 0)); // Do this in view matrix
        projectionMatrix.makeFrustum(
        // projectionMatrix.makeFrustum(
                -right + frustumShiftX, right + frustumShiftX,
                -top + frustumShiftY, top + frustumShiftY,
                zNear, zFar);
        // System.out.println("projectionMatrix 1 = "+projectionMatrix);
        // projectionMatrix.translate(new Vector3(eyeShiftScene, 0, 0));
        // System.out.println("projectionMatrix 2 = "+projectionMatrix);
        // System.out.println("eye shift = "+eyeShiftScene);
        projectionMatrixNeedsUpdate = false;
    }
    
    @Override
    protected void updateViewMatrix() {
        // TODO - viewer-fixed lights not at infinity need this translation too...
        if (parentCamera != null)
            viewMatrix.copy(parentCamera.getViewMatrix());
        else
            super.updateViewMatrix();
        final ConstFrustumShift shift = shiftStack.peek();
        float eyeShiftSceneX = shift.getShiftXPixels() * vantage.getSceneUnitsPerViewportHeight() 
                / viewport.getHeightPixels();
        float eyeShiftSceneY = shift.getShiftYPixels() * vantage.getSceneUnitsPerViewportHeight() 
                / viewport.getHeightPixels();
        viewMatrix.translate(new Vector3(eyeShiftSceneX, eyeShiftSceneY, 0));
        viewMatrixNeedsUpdate = false;
    }

    @Override
    public void pushFrustumShift(ConstFrustumShift shift) {
        viewMatrixNeedsUpdate = true;
        projectionMatrixNeedsUpdate = true;
        shiftStack.push(shift);
    }

    @Override
    public ConstFrustumShift popFrustumShift() {
        ConstFrustumShift result = shiftStack.pop();
        viewMatrixNeedsUpdate = true;
        projectionMatrixNeedsUpdate = true;
        return result;
    }
}
