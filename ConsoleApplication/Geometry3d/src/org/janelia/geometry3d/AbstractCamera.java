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

import org.janelia.console.viewerapi.ComposableObservable;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.ObservableInterface;

/**
 *
 * Base class for OrthographicCamera and PerspectiveCamera
 * 
 * @author Christopher Bruns
 */
public abstract class AbstractCamera implements ObservableInterface
{
    protected final Vantage vantage;
    protected final Viewport viewport;
    // viewMatrix transforms points from ground to camera
    protected final Matrix4 viewMatrix;
    protected boolean viewMatrixNeedsUpdate = true;
    // projection matrix transforms points from camera to frustum
    protected final Matrix4 projectionMatrix;
    protected boolean projectionMatrixNeedsUpdate = true;
    //
    private final ComposableObservable changeObservable; // to trigger listeners when camera viewpoint changes

    /**
     * @param vantage Camera state that could be shared between windows
     * @param viewport Pixel dimensions of this camera's window
     */
    public AbstractCamera(Vantage vantage, Viewport viewport) {
        this.vantage = vantage;
        this.viewport = viewport;
        this.viewMatrix = new Matrix4();
        this.projectionMatrix = new Matrix4();
        this.changeObservable = new ComposableObservable();
        // Vantage affects both view and projection matrices
        vantage.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // System.out.println("Vantage changed");
                viewMatrixNeedsUpdate = true;
                projectionMatrixNeedsUpdate = true;
                changeObservable.setChanged();
                changeObservable.notifyObservers(); // propagate update()
            }
        });
        // Viewport affects only projection matrix
        viewport.getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // System.out.println("Viewport changed");
                projectionMatrixNeedsUpdate = true;
                changeObservable.setChanged();
                changeObservable.notifyObservers(); // propagate update()
            }
        });
    }

    /**
     * 
     * @return Observable from which Observers can be notified of camera 
     * state changes, of either Vantage or Viewport
     */
    public ComposableObservable getChangeObservable() {
        return changeObservable;
    }
    
    /**
     * 
     * @return Matrix that converts from Camera frame to normalized coordinates
     */
    public Matrix4 getProjectionMatrix() {
        if (projectionMatrixNeedsUpdate)
            updateProjectionMatrix();
        return projectionMatrix;
    }

    /**
     * 
     * @return Matrix that converts from World frame to Camera frame
     */
    public Matrix4 getViewMatrix() {
        if (viewMatrixNeedsUpdate)
            updateViewMatrix();
        return viewMatrix;
    }

    /**
     * 
     * @return Shareable window-independent camera state
     */
    public Vantage getVantage() {
        return vantage;
    }

    /**
     * 
     * @return Pixel size, position, and depth of camera window
     */
    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public void setChanged() {
        changeObservable.setChanged();
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
    
    @Override
    public boolean hasChanged() {
        return changeObservable.hasChanged();
    }
    
    protected abstract void updateProjectionMatrix();
    
    protected abstract void updateViewMatrix();
    
}
