package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

// Click and drag mouse to drag the image in X and Y
public class PanMode
implements MouseMode 
{
	protected Point point;
	protected Point previousPoint;
	// TODO closed hand cursor
	protected Cursor dragCursor = new Cursor(Cursor.MOVE_CURSOR);
	protected Cursor hoverCursor = new Cursor(Cursor.HAND_CURSOR);
	protected Cursor currentCursor = hoverCursor;
	protected Component widget;
	protected Camera3d camera;
	
	@Override
	public Camera3d getCamera() {
		return camera;
	}

	@Override
	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

	protected void checkCursor() {
		if (currentCursor == null)
			return;
		if (widget == null)
			return;
		if (widget.getCursor() == currentCursor)
			return;
		widget.setCursor(currentCursor);
		// widget.repaint(); // does not help...
	}
	
	@Override
	public Component getComponent() {
		return widget;
	}

	@Override
	public void setComponent(Component widget) {
		this.widget = widget;
	}

	@Override
	public Cursor getDragCursor() {
		return dragCursor;
	}

	@Override
	public Cursor getHoverCursor() {
		return hoverCursor;
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		currentCursor = dragCursor;
		checkCursor();
		point = event.getPoint();
		if (previousPoint != null) 
		{
			Point p1 = previousPoint;
			Point p2 = point;
			Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
			if (camera != null)
				camera.incrementFocusPixels(dx.x, dx.y, 0);
			// TODO - actually drag
			System.out.println(String.format("Drag: %d, %d", dx.x, dx.y));
		}
		previousPoint = point;
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		currentCursor = hoverCursor;
		checkCursor();
	}

	@Override
	public void setDragCursor(Cursor dragCursor) {
		this.dragCursor = dragCursor;
	}

	@Override
	public void setHoverCursor(Cursor hoverCursor) {
		this.hoverCursor = hoverCursor;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {
		currentCursor = dragCursor;
		checkCursor();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		currentCursor = hoverCursor;
		checkCursor();
	}
}
