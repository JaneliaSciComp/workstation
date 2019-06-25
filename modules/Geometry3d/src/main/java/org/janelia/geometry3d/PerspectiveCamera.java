
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
