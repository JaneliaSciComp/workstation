package org.janelia.workstation.gui.camera;

import org.janelia.workstation.geom.Rotation3d;
import org.janelia.workstation.geom.Vec3;

// Camera3d manages rotation, translation, and scale in 3 dimensions.
public interface Camera3d
{
    Vec3 getFocus(); // Where the camera is pointed, in scene units
    double getPixelsPerSceneUnit(); // Zoom
    Rotation3d getRotation(); // Camera orientation
    boolean incrementFocusPixels(double dx, double dy, double dz); // Translate in screen pixel units
    boolean incrementFocusPixels(Vec3 offset); // Translate in screen pixel units
    boolean incrementZoom(double zoomRatio); // Relative zoom
    boolean resetFocus(); // Move focus to 0,0,0
    boolean resetRotation(); // Set rotation to X-right, Y-up, Z-out
    boolean setFocus(double x, double y, double z); // in scene units
    boolean setFocus(Vec3 focus); // in scene units
    boolean setRotation(Rotation3d r); // unitless
    boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit); // zoom
}
