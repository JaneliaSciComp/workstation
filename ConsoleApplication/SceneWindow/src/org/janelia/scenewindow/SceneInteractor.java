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
package org.janelia.scenewindow;

import org.janelia.geometry3d.Vector3;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Viewport;

/**
 *
 * @author brunsc
 */
public abstract class SceneInteractor 
{
    protected final AbstractCamera camera;
    protected final Component component;
    private final Vector3 xAxis = new Vector3(1, 0, 0);
    
    public SceneInteractor(AbstractCamera camera, Component component) {
        this.camera = camera;
        this.component = component;
    }
    
    public abstract String getToolTipText();
    
    public void notifyObservers() {
        camera.getVantage().notifyObservers();
    }
    
    public boolean panPixels(int dx, int dy, int dz) {
        if ((dx == 0) && (dy == 0) && (dz == 0))
            return false;
        float scale = camera.getVantage().getSceneUnitsPerViewportHeight() 
                / component.getHeight();
        return panSceneUnits(new Vector3(dx, dy, dz).multiplyScalar(scale));
    }
    
    public boolean panSceneUnits(Vector3 pan) {
        if (pan.lengthSquared() == 0)
            return false;
        Vantage v = camera.getVantage();
        Vector3 newF = new Vector3(v.getFocusPosition());
        // Rotate focus vector to match current view
        Vector3 dF = new Vector3(pan);
        Rotation R = new Rotation(v.getRotationInGround());
        dF.applyRotation(R);
        newF.add(dF);
        return v.setFocusPosition(newF);
    }
    
    private Rotation rotatePixelsRotation(int dx, int dy, float radiansPerScreen) {
        if ((dx == 0) && (dy == 0))
            return null;
        Vector3 axis = new Vector3(-dy, dx, 0); // Rotate about this axis
        float amount = axis.length(); // Longer drag means more rotation
        axis.multiplyScalar(1.0f/amount); // normalize axis length to 1.0
        Viewport vp = camera.getViewport();
        float windowSize = 0.5f * (vp.getHeightPixels() + vp.getWidthPixels());
        amount /= windowSize; // convert pixels to screens
        amount *= radiansPerScreen; // convert screens to radians
        Vantage v = camera.getVantage();
        Rotation newR = new Rotation(v.getRotationInGround());
        newR.multiply(new Rotation().setFromAxisAngle(axis, amount).transpose());

        return newR;
    }
    
    /**
     * Like rotatePixels, but restricts world-Y axis to upper half of camera YZ plane
     * @param dx
     * @param dy
     * @param radiansPerScreen
     * @return true if the camera position changed
     */
    public boolean orbitPixels(int dx, int dy, float radiansPerScreen) 
    {
        if ((dx == 0) && (dy == 0))
            return false;
        
        Viewport vp = camera.getViewport();
        float windowSize = 0.5f * (vp.getHeightPixels() + vp.getWidthPixels());
        float dAzimuth = -dx * radiansPerScreen / windowSize;
        float dElevation = dy * radiansPerScreen / windowSize;
        Vector3 upInWorld = camera.getVantage().getUpInWorld();
        Rotation rotAz = new Rotation().setFromAxisAngle(upInWorld, dAzimuth);
        Rotation rotEl = new Rotation().setFromAxisAngle(xAxis, dElevation);
        Rotation newR = new Rotation(camera.getVantage().getRotationInGround());
        newR.multiply(rotEl);
        newR.copy(new Rotation(rotAz).multiply(newR));
        // System.out.println(dAzimuth+", "+dElevation);

        // NOTE: Constraint to up direction has been moved to Vantage class
        
        Vantage v = camera.getVantage();
        // System.out.println(newR);
        return v.setRotationInGround(newR);
    }
    
    public boolean recenterOnMouse(MouseEvent event) {
        Viewport vp = camera.getViewport();
        int centerX = vp.getOriginXPixels() + vp.getWidthPixels() / 2;
        int centerY = vp.getOriginYPixels() + vp.getHeightPixels() / 2;
        int dx = centerX - event.getX();
        int dy = centerY - event.getY();
        return panPixels(-dx, dy, 0);
    }
    

    
    public boolean rotatePixels(int dx, int dy, float radiansPerScreen) {
        Rotation newR = rotatePixelsRotation(dx, dy, radiansPerScreen);
        if (newR == null)
            return false;
        Vantage v = camera.getVantage();
        return v.setRotationInGround(newR);
    }
    
    /**
     * @param zoomOutRatio Zoom factor. Positive number. 1.0 means no change.
     *  0.5 zooms in by a factor of 2. 2.0 zooms out by a factor of 2.
     * @return true if the camera scale changed
     */
    public boolean zoomOut(float zoomOutRatio) {
        Vantage vantage = camera.getVantage();
        float newScale = zoomOutRatio * vantage.getSceneUnitsPerViewportHeight();
        // System.out.println("zoom out "+zoomOutRatio+", "+newScale);
        return vantage.setSceneUnitsPerViewportHeight(newScale);
    }
    
    public boolean zoomMouseWheel(MouseWheelEvent event, float sensitivity) 
    {
	// Use Google maps convention of scroll wheel up to zoom in.
        // (Even though that makes no sense...)
        int notches = event.getWheelRotation();
        float zoomRatio = (float) Math.pow(2.0, -notches * sensitivity);
        return zoomOut(zoomRatio);
    }
 
}
