package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

// Click and drag mouse to drag the image in X and Y
public class PanMode
extends BasicMouseMode 
{
    // Viewer orientation relative to canonical orientation.
    // Canonical orientation is x-right, y-down, z-away
    Rotation rotation = new Rotation();

    public PanMode() {
		setHoverCursor(BasicMouseMode.createCursor("grab_opened.png", 8, 8));
		setDragCursor(BasicMouseMode.createCursor("grab_closed.png", 8, 8));
	}
	
	public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    @Override
	public void mouseDragged(MouseEvent event) {
		super.mouseDragged(event);
		if (getPreviousPoint() == null)
			return;
		if (getPoint() == null)
			return;
		Point p1 = getPreviousPoint();
		Point p2 = getPoint();
		// Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
		Vec3 dx = new Vec3(p2.x - p1.x, p2.y - p1.y, 0.0);
		dx = rotation.times(dx);
		if (getCamera() != null)
			getCamera().incrementFocusPixels(-dx.x(), -dx.y(), -dx.z());
	}
}
