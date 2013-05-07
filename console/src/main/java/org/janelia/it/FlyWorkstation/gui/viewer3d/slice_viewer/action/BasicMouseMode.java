package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;

public class BasicMouseMode implements MouseMode 
{
	public Cursor getAltCursor() {
		return altCursor;
	}

	public void setAltCursor(Cursor altCursor) {
		this.altCursor = altCursor;
	}

	protected Point point;
	protected Point previousPoint;
	protected Cursor dragCursor; // shown when mouse button pressed
	protected Cursor hoverCursor; // shown when mouse button released
	protected Cursor altCursor; // shown when shift/ctrl pressed
	protected Cursor currentCursor = hoverCursor;
	protected MouseModalWidget widget;
	protected Camera3d camera;

	
	public static Cursor createCursor(String fileName, int x, int y) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		ImageIcon icon = Icons.getIcon(fileName);
		Cursor cursor = toolkit.createCustomCursor(icon.getImage(), new Point(x, y), fileName);
		return cursor;
	}

	protected void checkCursor(Cursor newCursor) {
		if (newCursor == null)
			return;
		currentCursor = newCursor;
		if (widget == null)
			return;
		if (widget.getCursor() == currentCursor)
			return;
		widget.setCursor(currentCursor);
	}
	
	public Point getPoint() {
		return point;
	}

	public Point getPreviousPoint() {
		return previousPoint;
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		checkCursor(dragCursor);
		setPreviousPoint(getPoint());
		setPoint(event.getPoint());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		checkCursor(hoverCursor);
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		setPoint(event.getPoint());
		setPreviousPoint(null);
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent event) {
		setPoint(null);
		setPoint(event.getPoint());
		setPreviousPoint(null);
	}

	@Override
	public void mousePressed(MouseEvent event) {
		checkCursor(dragCursor);
		setPoint(event.getPoint());
		setPreviousPoint(null);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		checkCursor(hoverCursor);
		setPoint(event.getPoint());
		setPreviousPoint(null);
	}

	@Override
	public Cursor getDragCursor() {
		return dragCursor;
	}

	@Override
	public void setDragCursor(Cursor dragCursor) {
		this.dragCursor = dragCursor;
	}

	@Override
	public Cursor getHoverCursor() {
		return this.hoverCursor;
	}

	@Override
	public void setHoverCursor(Cursor hoverCursor) {
		this.hoverCursor = hoverCursor;
		if (this.currentCursor == null)
			this.currentCursor = getHoverCursor();
	}

	@Override
	public MouseModalWidget getComponent() {
		return widget;
	}

	@Override
	public void setComponent(MouseModalWidget widget) {
		if (this.widget == widget)
			return;
		this.widget = widget;
		checkCursor(currentCursor);
	}

	@Override
	public Camera3d getCamera() {
		return camera;
	}

	@Override
	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public void setPreviousPoint(Point previousPoint) {
		this.previousPoint = previousPoint;
	}

}
