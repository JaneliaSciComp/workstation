
package org.janelia.geometry3d;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.janelia.geometry3d.camera.BasicViewSlab;
import org.janelia.geometry3d.camera.ConstRotation;
import org.janelia.geometry3d.camera.ConstVantage;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.geometry3d.camera.ConstViewport;
import org.janelia.geometry3d.camera.SlabbableCamera;
import org.janelia.geometry3d.camera.VantageableCamera;
import org.janelia.geometry3d.camera.ViewportableCamera;

/**
 *
 * Base class for OrthographicCamera and PerspectiveCamera
 * 
 * @author Christopher Bruns
 */
public abstract class AbstractCamera implements ObservableInterface, 
                                                VantageableCamera,
                                                ViewportableCamera,
                                                SlabbableCamera {
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
    //
    private final Deque<ConstViewSlab> internalSlabStack = new LinkedList<>();

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
        this.vantage.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                viewMatrixNeedsUpdate = true;
                projectionMatrixNeedsUpdate = true;
                changeObservable.setChanged();
                changeObservable.notifyObservers(); // propagate update()
            }
        });
        // Viewport affects only projection matrix
        this.viewport.getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
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
    @Override
    public Vantage getVantage() {
        return vantage;
    }

    /**
     * 
     * @return Pixel size, position, and depth of camera window
     */
    @Override
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
    public void notifyObservers(Object arg) {
        changeObservable.notifyObservers(arg);
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
    
    @Override
    public void setViewport(ConstViewport viewport) 
    {
        this.viewport.setOriginXPixels(viewport.getOriginXPixels());
        this.viewport.setOriginYPixels(viewport.getOriginYPixels());
        this.viewport.setWidthPixels(viewport.getWidthPixels());
        this.viewport.setHeightPixels(viewport.getHeightPixels());
    }

    @Override
    public ConstViewSlab getNominalViewSlab() {
        return this.viewport;
    }

    @Override
    public void setNominalViewSlab(ConstViewSlab slab) {
        this.viewport.setzNearRelative(slab.getzNearRelative());
        this.viewport.setzFarRelative(slab.getzFarRelative());
    }

    @Override
    public void pushInternalViewSlab(ConstViewSlab slab) {
        // create a new instance so I can be certain of implementation of equals()
        ConstViewSlab previousInternalSlab = new BasicViewSlab(getEffectiveViewSlab());
        if (! previousInternalSlab.equals(slab)) {
            projectionMatrixNeedsUpdate = true;
        }
        internalSlabStack.push(new BasicViewSlab(slab));
    }

    ConstViewSlab getEffectiveViewSlab() {
        ConstViewSlab result = this.viewport;
        if (! internalSlabStack.isEmpty())
            result = internalSlabStack.peek();
        return result;
    }
    
    @Override
    public ConstViewSlab popInternalViewSlab() {
        if (internalSlabStack.isEmpty()) // should not happen, but carry on anyway
            return new BasicViewSlab(this.viewport);
        ConstViewSlab result = new BasicViewSlab(internalSlabStack.pop());
        // Check for change
        if (! result.equals(getEffectiveViewSlab()))
            projectionMatrixNeedsUpdate = true;
        return result;
    }

    @Override
    public void setVantage(ConstVantage vantage) {
        if (this.vantage == vantage)
            return;
        ConstVector3 foc = vantage.getFocusAsVector3();
        this.vantage.setFocus(foc.getX(), foc.getY(), foc.getZ());
        float zoom = vantage.getSceneUnitsPerViewportHeight();
        this.vantage.setSceneUnitsPerViewportHeight(zoom);
        ConstRotation rot = vantage.getRotation();
        this.vantage.setRotationInGround(rot);
    }
    
    protected abstract void updateProjectionMatrix();
    
    protected abstract void updateViewMatrix();

}
