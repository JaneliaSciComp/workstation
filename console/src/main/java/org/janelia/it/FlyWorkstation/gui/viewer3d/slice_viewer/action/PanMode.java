package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Point;
import java.awt.event.MouseEvent;

// Click and drag mouse to drag the image in X and Y
public class PanMode
extends BasicMouseMode 
{
	public PanMode() {
		setHoverCursor(BasicMouseMode.createCursor("grab_opened.png", 8, 8));
		setDragCursor(BasicMouseMode.createCursor("grab_closed.png", 8, 8));
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
		Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
		if (getCamera() != null)
			getCamera().incrementFocusPixels(-dx.x, -dx.y, 0);
	}
}
