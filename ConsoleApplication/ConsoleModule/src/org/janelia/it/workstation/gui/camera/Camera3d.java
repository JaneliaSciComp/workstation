package org.janelia.it.workstation.gui.camera;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.Vec3;


// Camera3d manages rotation, translation, and scale in 3 dimensions.
public interface Camera3d
{
	public Vec3 getFocus(); // Where the camera is pointed, in scene units
	public double getPixelsPerSceneUnit(); // Zoom
	public Rotation3d getRotation(); // Camera orientation
	public boolean incrementFocusPixels(double dx, double dy, double dz); // Translate in screen pixel units
	public boolean incrementFocusPixels(Vec3 offset); // Translate in screen pixel units
    public boolean incrementZoom(double zoomRatio); // Relative zoom
    public boolean resetFocus(); // Move focus to 0,0,0
    public boolean resetRotation(); // Set rotation to X-right, Y-up, Z-out
    public boolean setFocus(double x, double y, double z); // in scene units
    public boolean setFocus(Vec3 focus); // in scene units
    public boolean setRotation(Rotation3d r); // unitless
    public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit); // zoom
}
