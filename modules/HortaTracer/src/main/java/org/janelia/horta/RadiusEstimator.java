package org.janelia.horta;

import java.awt.Point;

/**
 * Interface for methods that estimate the neurite radius as a particular
 * location; generalized so I can experiment with different techniques, including
 * reading from a gpu computation.
 * 
 * @author Christopher Bruns
 */
public interface RadiusEstimator {
    float estimateRadius(Point screenPoint, VolumeProjection image);
}
