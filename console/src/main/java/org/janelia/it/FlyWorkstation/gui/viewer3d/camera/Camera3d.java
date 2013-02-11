package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public interface Camera3d
{
	public Vec3 getFocus();
	public double getPixelsPerSceneUnit();
	public Rotation getRotation();
	public boolean incrementFocusPixels(int dx, int dy, int dz);
    public boolean incrementZoom(float zoomRatio);
    public boolean resetFocus();
    public boolean resetRotation();
    public boolean setFocus(Vec3 f);
    public boolean setRotation(Rotation r);
    public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit);
}
