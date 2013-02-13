package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

// Click and drag mouse to drag the image in X and Y
public class PanMode
implements MouseMode 
{
	BasicMouseMode mode = new BasicMouseMode();
	
	public PanMode() {
		setHoverCursor(BasicMouseMode.createCursor("grab_opened.png", 8, 8));
		setDragCursor(BasicMouseMode.createCursor("grab_closed.png", 8, 8));
	}
	
	@Override
	public Camera3d getCamera() {
		return mode.getCamera();
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
	}

	@Override
	public MouseModalWidget getComponent() {
		return mode.getComponent();
	}

	@Override
	public void setComponent(MouseModalWidget widget) {
		mode.setComponent(widget);
	}

	@Override
	public Cursor getDragCursor() {
		return mode.getDragCursor();
	}

	@Override
	public Cursor getHoverCursor() {
		return mode.getHoverCursor();
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		mode.mouseDragged(event);
		if (mode.getPreviousPoint() == null)
			return;
		if (mode.getPoint() == null)
			return;
		Point p1 = mode.getPreviousPoint();
		Point p2 = mode.getPoint();
		Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
		if (getCamera() != null)
			getCamera().incrementFocusPixels(-dx.x, -dx.y, 0);
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mode.mouseMoved(event);
	}

	@Override
	public void setDragCursor(Cursor dragCursor) {
		mode.setDragCursor(dragCursor);
	}

	@Override
	public void setHoverCursor(Cursor hoverCursor) {
		mode.setHoverCursor(hoverCursor);
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		mode.mouseClicked(event);
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		mode.mouseEntered(event);
	}

	@Override
	public void mouseExited(MouseEvent event) {
		mode.mouseExited(event);
	}

	@Override
	public void mousePressed(MouseEvent event) {
		mode.mousePressed(event);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		mode.mouseReleased(event);
	}

	@Override
	public Cursor getAltCursor() {
		return mode.getAltCursor();
	}

	@Override
	public void setAltCursor(Cursor altCursor) {
		mode.setAltCursor(altCursor);
	}
}
