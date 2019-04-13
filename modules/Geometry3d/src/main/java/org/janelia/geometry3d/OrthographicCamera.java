
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
