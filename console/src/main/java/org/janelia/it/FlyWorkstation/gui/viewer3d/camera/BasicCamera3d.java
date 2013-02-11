package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public class BasicCamera3d
implements Camera3d
{
	// View center
    private Vec3 focus = new Vec3(0,0,0); // in scene units
    private Rotation rotation = new Rotation();
    private double pixelsPerSceneUnit = 1.0; // zoom

	public Vec3 getFocus() {
		return focus;
	}

	public double getPixelsPerSceneUnit() {
		return pixelsPerSceneUnit;
	}

	public Rotation getRotation() {
		return rotation;
	}
	
    @Override
	public boolean incrementFocusPixels(int x, int y, int z) {
		Vec3 v = new Vec3(x, y, z);
		v = v.times(1.0 / pixelsPerSceneUnit);
		return setFocus(focus.plus(v));
	}
	
    public boolean incrementZoom(float zoomRatio) {
    		if (zoomRatio == 1.0)
    			return false; // no change
    		if (zoomRatio <= 0.0)
    			return false; // impossible
    		pixelsPerSceneUnit *= zoomRatio;
    		return true;
    }
    
    public boolean resetFocus() {
    		return setFocus(new Vec3(0,0,0));
    }
    
    public boolean resetRotation() {
    		return setRotation(new Rotation());
    }
    
    public boolean setFocus(Vec3 f) {
		if (f == focus)
			return false; // no change
		focus = f;
		return true;
    }

    public boolean setRotation(Rotation r) {
		if (r == rotation)
			return false; // no change
		rotation = r;
		return true;
    }

	public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
		if (this.pixelsPerSceneUnit == pixelsPerSceneUnit)
			return false; // no change
		this.pixelsPerSceneUnit = pixelsPerSceneUnit;
		return true;
	}
}
