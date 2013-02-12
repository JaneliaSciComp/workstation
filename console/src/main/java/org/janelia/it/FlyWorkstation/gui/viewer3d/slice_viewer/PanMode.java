package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

// Click and drag mouse to drag the image in X and Y
public class PanMode
implements MouseMode 
{
	protected Point point;
	protected Point previousPoint;
	protected Cursor dragCursor;
	protected Cursor hoverCursor;
	protected Cursor currentCursor = hoverCursor;
	protected Component widget;
	protected Camera3d camera;
	
	protected static Cursor createCursor(String fileName, int x, int y) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		ImageIcon icon = Icons.getIcon(fileName);
		Cursor cursor = toolkit.createCustomCursor(icon.getImage(), new Point(x, y), fileName);
		return cursor;
	}
	
	public PanMode() {
		setDragCursor(PanMode.createCursor("grab_closed.png", 8, 8));
		setHoverCursor(PanMode.createCursor("grab_opened.png", 8, 8));
		currentCursor = getHoverCursor();
	}
	
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
				camera.incrementFocusPixels(-dx.x, -dx.y, 0);
			// System.out.println(String.format("Drag: %d, %d", dx.x, dx.y));
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
	public void mouseClicked(MouseEvent arg0) {
		previousPoint = null;
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {
		previousPoint = null;
	}

	@Override
	public void mousePressed(MouseEvent event) {
		currentCursor = dragCursor;
		checkCursor();
		previousPoint = event.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		currentCursor = hoverCursor;
		checkCursor();
		previousPoint = null;
	}
}
