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

import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.ComposableObservable;
import java.util.Collection;
import java.util.Observer;

/**
 * Viewport-independent, camera-type-independent camera representation,
 * that could be shared among multiple viewers.
 * 
 * @author cmbruns
 */
public class Vantage 
implements CompositeObject3d, ObservableInterface
{
    private float sceneUnitsPerViewportHeight; // Zoom level
    private final Vector3 focusPosition = new Vector3(0,0,0); // Location of subject in GL units
    private final Rotation rotationInGround = new Rotation(); // Orientation of camera
    private final ComposableObservable changeObservable = new ComposableObservable();
    private final CompositeObject3d object3D; // delegate object 3D
    
    private final Rotation defaultRotation = new Rotation();
    private final Vector3 defaultFocus = new Vector3(0, 0, 0);
    private float defaultSceneUnitsPerViewportHeight = 100f;
    private boolean constrainedToUpDirection = false;
    private final Vector3 upInWorld = new Vector3(0, 1, 0);
    // per-axis scale factors to meet Mouse Light customer requirement
    private final Vector3 worldScaleHack = new Vector3(1, 1, 1);

    public Vantage(Object3d parent) {
        this.object3D = new BasicObject3D(parent);
        this.sceneUnitsPerViewportHeight = 2.0f;
    }

    /** Adjust current view to showcase a particular region
     * 
     * @param boundingBox 
     */
    public void centerOn(Box3 boundingBox) {
        setFocusPosition(boundingBox.getCentroid());
        Vector3 vscale = new Vector3(boundingBox.max).sub(boundingBox.min);
        float maxSize = Math.max(vscale.getX(), vscale.getY());
        maxSize = Math.max(maxSize, vscale.getZ());
        maxSize = Math.max(maxSize, 0.01f);
        setSceneUnitsPerViewportHeight(maxSize);
    }
    
    /**
     * Adjust defaults so "reset" highlights a particular bounding box
     * @param boundingBox 
     */
    public void setDefaultBoundingBox(Box3 boundingBox) {
        setDefaultFocus(boundingBox.getCentroid());
        Vector3 vscale = new Vector3(boundingBox.max).sub(boundingBox.min);
        float maxSize = Math.max(vscale.getX(), vscale.getY());
        maxSize = Math.max(maxSize, vscale.getZ());
        maxSize = Math.max(maxSize, 0.01f);
        setDefaultSceneUnitsPerViewportHeight(maxSize);        
    }
    
    public Vector3 getDefaultFocus() {
        return defaultFocus;
    }

    public Rotation getDefaultRotation() {
        return defaultRotation;
    }

    public float getDefaultSceneUnitsPerViewportHeight() {
        return defaultSceneUnitsPerViewportHeight;
    }

    public void setDefaultFocus(Vector3 defaultFocus) {
        this.defaultFocus.copy(defaultFocus);
    }

    public void setDefaultRotation(Rotation defaultRotation) {
        this.defaultRotation.copy(defaultRotation);
    }

    public void setDefaultSceneUnitsPerViewportHeight(float sceneUnitsPerViewportHeight) {
        this.defaultSceneUnitsPerViewportHeight = sceneUnitsPerViewportHeight;
    }

    @Override
    public void setChanged() {
        changeObservable.setChanged();
    }

    public Vector3 getFocusPosition() {
        return focusPosition;
    }

    public Rotation getRotationInGround() {
        return rotationInGround;
    }

    public boolean setRotationInGround(Rotation rotationInGround) 
    {
        if (this.rotationInGround.equals(rotationInGround))
            return false; // unchanged
        this.rotationInGround.copy(rotationInGround);
        if (isConstrainedToUpDirection()) {
            constrainUp();
        }
        changeObservable.setChanged();
        return true;
    }
    
    public boolean setFocusPosition(Vector3 focusPosition) 
    {
        if (this.focusPosition.equals(focusPosition))
            return false; // unchanged
        this.focusPosition.copy(focusPosition);
        changeObservable.setChanged();
        // System.out.println("New Vantage focus = "+focusPosition);
        return true;
    }
    
    public float getSceneUnitsPerViewportHeight() {
        return sceneUnitsPerViewportHeight;
    }

    public boolean setSceneUnitsPerViewportHeight(float sceneUnitsPerViewportHeight) {
        if(sceneUnitsPerViewportHeight == this.sceneUnitsPerViewportHeight)
            return false;
        this.sceneUnitsPerViewportHeight = sceneUnitsPerViewportHeight;
        changeObservable.setChanged();
        return true;
    }

    public Vector3 getWorldScaleHack()
    {
        return worldScaleHack;
    }

    public boolean setWorldScaleHack(float xScale, float yScale, float zScale) {
        Vector3 newScale = new Vector3(xScale, yScale, zScale);
        return setWorldScaleHack(newScale);
    }
    
    public boolean setWorldScaleHack(Vector3 newScale) {
        if (newScale.equals(worldScaleHack))
            return false; // no change
        worldScaleHack.copy(newScale);
        setChanged();
        return true;
    }
    
    @Override
    public Object3d addChild(Object3d child) {
        object3D.addChild(child);
        return this;
    }

    @Override
    public Object3d getParent() {
        return object3D.getParent();
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
        return object3D.getChildren();
    }

    @Override
    public Matrix4 getTransformInWorld() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix4 getTransformInParent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isVisible() {
        return object3D.isVisible();
    }

    @Override
    public Object3d setVisible(boolean isVisible) {
        object3D.setVisible(isVisible);
        return this;
    }

    @Override
    public String getName() {
        return object3D.getName();
    }

    @Override
    public Object3d setName(String name) {
        object3D.setName(name);
        return this;
    }

    @Override
    public Object3d setParent(Object3d parent) {
        object3D.setParent(parent);
        return this;
    }
    
    public void resetFocus() {
        setFocusPosition(defaultFocus);
    }

    public void resetRotation() {
        setRotationInGround(defaultRotation);
    }
    
    public void resetScale() {
        setSceneUnitsPerViewportHeight(defaultSceneUnitsPerViewportHeight);
    }
    
    public void resetView() {
        resetFocus();
        resetRotation();
        resetScale();
    }

    @Override
    public void notifyObservers() {
        changeObservable.notifyObservers();
    }

    @Override
    public void addObserver(Observer observer) {
        changeObservable.addObserver(observer);
    }
    
    @Override
    public void deleteObserver(Observer observer) {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers() {
        changeObservable.deleteObservers();
    }  

    public boolean isConstrainedToUpDirection() {
        return constrainedToUpDirection;
    }

    public boolean setConstrainedToUpDirection(boolean constrainedToUpDirection) {
        if (constrainedToUpDirection == this.constrainedToUpDirection)
            return false;
        this.constrainedToUpDirection = constrainedToUpDirection;
        changeObservable.setChanged();
        return true;
    }

    public Vector3 getUpInWorld() {
        return upInWorld;
    }

    public boolean setUpInWorld(Vector3 upInWorld) {
        if (this.upInWorld.equals(upInWorld))
            return false;
        this.upInWorld.copy(upInWorld);
        if (isConstrainedToUpDirection()) {
            constrainUp();
        }
        return true;
    }

    private boolean constrainUp() 
    {
        Vector3 worldUpInCamera = new Rotation(rotationInGround).transpose().multiply(upInWorld).normalize();
        Vector3 inYZPlaneWorldUpInCamera = new Vector3( // where world Y should be TODO
                0.0f,
                Math.max(0.0f, worldUpInCamera.getY()),
                worldUpInCamera.getZ()).normalize();
        float cosineCorrection = worldUpInCamera.dot(inYZPlaneWorldUpInCamera);
        // System.out.println(cosineCorrection);
        if (cosineCorrection > 0.99999) 
        {
            return false; // no change
        }
        // if (cosineCorrection < 1.0f) {
        // if (cosineCorrection < 0.99999) { // different enough to require correction
        // System.out.println("cos angle = "+cosineCorrection);
        float angle = (float)Math.acos(cosineCorrection);
        Vector3 axis = new Vector3(worldUpInCamera);
        axis = axis.cross(inYZPlaneWorldUpInCamera);
        if (axis.lengthSquared() <= 0) 
        {
            return false; // numerically hard to correct
        }
        axis = axis.normalize();
        Rotation correction = new Rotation().setFromAxisAngle(axis, -angle);
        rotationInGround.multiply(correction);
        changeObservable.setChanged();
        return true;
    }
    
}
