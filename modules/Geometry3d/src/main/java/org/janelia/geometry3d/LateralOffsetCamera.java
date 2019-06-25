
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
public class LateralOffsetCamera extends PerspectiveCamera implements GeneralCamera {
    private final PerspectiveCamera parentCamera;
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
        final float eyeShiftSceneX;
        final float eyeShiftSceneY;
        if (viewport.getHeightPixels() == 0) {
            eyeShiftSceneX = 0;
            eyeShiftSceneY = 0;
        } else {
            eyeShiftSceneX = shift.getShiftXPixels() * vantage.getSceneUnitsPerViewportHeight() / viewport.getHeightPixels();
            eyeShiftSceneY = shift.getShiftYPixels() * vantage.getSceneUnitsPerViewportHeight() / viewport.getHeightPixels();
        }
        final ConstViewSlab slab = getEffectiveViewSlab();
        final float focusDistance = getCameraFocusDistance();
        final float zNear = slab.getzNearRelative() * focusDistance;
        final float zFar = slab.getzFarRelative() * focusDistance;
        final float frustumShiftX = eyeShiftSceneX * slab.getzNearRelative();
        final float frustumShiftY = eyeShiftSceneY * slab.getzFarRelative();

        final float top = zNear * (float)Math.tan(0.5 * getFovRadians());
        final float right = viewport.getAspect() * top;
        // The centering translation should be on modelview (view) matrix,
        // so specular reflections would adjust correctly
        // pj.translate(new Vector3(eyeShiftScene, 0, 0)); // Do this in view matrix
        projectionMatrix.makeFrustum(
                -right + frustumShiftX, right + frustumShiftX,
                -top + frustumShiftY, top + frustumShiftY,
                zNear, zFar);
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
        float eyeShiftSceneX;
        float eyeShiftSceneY;
        if (viewport.getHeightPixels() == 0) {
            eyeShiftSceneX = 0;
            eyeShiftSceneY = 0;
        } else {
            eyeShiftSceneX = shift.getShiftXPixels() * vantage.getSceneUnitsPerViewportHeight() / viewport.getHeightPixels();
            eyeShiftSceneY = shift.getShiftYPixels() * vantage.getSceneUnitsPerViewportHeight() / viewport.getHeightPixels();
        }
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
