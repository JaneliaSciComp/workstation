package org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

// Camera3d manages rotation, translation, and scale in 3 dimensions.
public interface Camera3d
{
	public Vec3 getFocus(); // Where the camera is pointed, in scene units
	public double getPixelsPerSceneUnit(); // Zoom
	public Rotation getRotation(); // Camera orientation
	public boolean incrementFocusPixels(double dx, double dy, double dz); // Translate in screen pixel units
	public boolean incrementFocusPixels(Vec3 offset); // Translate in screen pixel units
    public boolean incrementZoom(double zoomRatio); // Relative zoom
    public boolean resetFocus(); // Move focus to 0,0,0
    public boolean resetRotation(); // Set rotation to X-right, Y-up, Z-out
    public boolean setFocus(double x, double y, double z); // in screen units
    public boolean setFocus(Vec3 focus); // in screen units
    public boolean setRotation(Rotation r); // unitless
    public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit); // zoom
}
