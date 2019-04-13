package org.janelia.horta;

import java.awt.Component;
import java.awt.geom.Point2D;
import org.janelia.geometry3d.Vector3;

/**
 *
 * @author Christopher Bruns
 */
public interface VolumeProjection {
    public Component getMouseableComponent();
    public double getIntensity(Point2D xy);
    public Vector3 worldXyzForScreenXy(Point2D xy);
    public Vector3 worldXyzForScreenXyInPlane(Point2D xy);
    public float getPixelsPerSceneUnit();
    public boolean isNeuronModelAt(Point2D xy);
    public boolean isVolumeDensityAt(Point2D xy);
}
