package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Vector;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.MenuItemGenerator;
import org.janelia.it.workstation.gui.large_volume_viewer.MouseModalWidget;
import org.janelia.it.workstation.gui.large_volume_viewer.RubberBand;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;

public class ZoomMode 
implements WheelMode, MouseMode
{
	protected boolean centerOnCursor = false;
	BasicMouseMode mode = new BasicMouseMode();

	public ZoomMode() {
		setHoverCursor(BasicMouseMode.createCursor("magnify_plus_cursor.png", 8, 8));
		setDragCursor(BasicMouseMode.createCursor("crosshair.png", 7, 7));
		setAltCursor(BasicMouseMode.createCursor("magnify_minus_cursor.png", 8, 8));
	}

	@Override
	public String getToolTipText() {
		return "Drag to zoom in.";
	}

	@Override
	public MouseModalWidget getWidget() {
		return mode.getWidget();
	}

	@Override
	public Camera3d getCamera() {
		return mode.getCamera();
	}

	public boolean isCenterOnCursor() {
		return centerOnCursor;
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		mode.mouseDragged(event);
		if (event.isPopupTrigger())
			return;
		if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			getWidget().getRubberBand().setEndPoint(event.getPoint());
		}
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mode.mouseMoved(event);
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
		// Don't zoom and context menu at the same time
		if (event.isPopupTrigger())
			return;
		if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			RubberBand rb = getWidget().getRubberBand();
			rb.setStartPoint(event.getPoint());
			rb.setEndPoint(event.getPoint());
			rb.setVisible(true);
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		mode.mouseReleased(event);
		RubberBand rb = getWidget().getRubberBand();
		if (! rb.isVisible())
			return;
		rb.setVisible(false);
		if (event.isPopupTrigger())
			return;
		Point p0 = rb.getStartPoint();
		Point p1 = rb.getEndPoint();
		// Don't zoom if box is too small
		int dx = Math.abs(p0.x - p1.x);
		int dy = Math.abs(p0.y - p1.y);
		int m = Math.max(dx, dy);
		if (m < 6) // rubber band box must be at least this many pixels
			return; // Box too small
		// un-zoom with keyboard modifier
		boolean isModified = ((event.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0);
		isModified |= ((event.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0);
		double zoomRatio = 1.0;
		if (isModified) {
			zoomRatio = 1.41421;
		}
		else {
			Viewport vp = getWidget().getViewport();
			int w = vp.getWidth();
			int h = vp.getHeight();
			zoomRatio = Math.max(dx/(double)(w), dy/(double)(h));
		}
		// recenter
		double cx = 0.5 * (p0.x + p1.x);
		double cy = 0.5 * (p0.y + p1.y);
		Point2D newCenter = getWidget().getPixelOffsetFromCenter(new Point2D.Double(cx, cy));
		getCamera().incrementFocusPixels(newCenter.getX(), newCenter.getY(), 0.0);
		// zoom
		getCamera().incrementZoom(1.0/zoomRatio);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) 
	{
		Camera3d camera = getCamera();
		if (camera == null)
			return;
		int notches = event.getWheelRotation();
		if (notches == 0)
			return;
		// compromise between sensitive wheel on my laptop and less sensitive wheel on my workstation
		double zoomRatio = Math.pow(2.0, -notches/20.0);
		camera.incrementZoom(zoomRatio);
		if (isCenterOnCursor()) {
			Point2D dx = getWidget().getPixelOffsetFromCenter(event.getPoint());
			Vec3 dv = new Vec3(dx.getX(), dx.getY(), 0.0);
			dv = dv.times(zoomRatio - 1.0);
			camera.incrementFocusPixels(dv);
		}
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
	}

	public void setCenterOnCursor(boolean centerOnCursor) {
		this.centerOnCursor = centerOnCursor;
	}

	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		mode.setWidget(widget, updateCursor);
	}


	@Override
	public Cursor getDragCursor() {
		return mode.getDragCursor();
	}

	@Override
	public void setDragCursor(Cursor dragCursor) {
		mode.setDragCursor(dragCursor);
	}

	@Override
	public Cursor getHoverCursor() {
		return mode.getHoverCursor();
	}

	@Override
	public void setHoverCursor(Cursor hoverCursor) {
		mode.setHoverCursor(hoverCursor);
	}

	@Override
	public Cursor getAltCursor() {
		return mode.getAltCursor();
	}

	@Override
	public void setAltCursor(Cursor altCursor) {
		mode.setAltCursor(altCursor);
	}

    @Override
    public MenuItemGenerator getMenuItemGenerator() {
        return new MenuItemGenerator() {
            @Override
            public List<JMenuItem> getMenus(MouseEvent event) {
                return new Vector<JMenuItem>(); // empty list
            }
        };
    }

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

}
