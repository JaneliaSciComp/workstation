package org.janelia.horta;

import java.awt.Point;

/**
 * Trivial radius estimator intended only for debugging.
 * @author Christopher Bruns
 */
class ConstantRadiusEstimator implements RadiusEstimator {
    private final float radius;
    
    public ConstantRadiusEstimator(float f) {
        radius = f;
    }

    @Override
    public float estimateRadius(Point screenPoint, VolumeProjection image) {
        return radius;
    }
    
}
