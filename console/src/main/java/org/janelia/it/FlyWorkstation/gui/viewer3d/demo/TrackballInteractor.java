package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.UnitVec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;

/**
 * TrackballInteractor reads mouse events from a Component, and
 * uses the results to modify a Camera3d.
 * 
 * Drag to rotate.
 * Shift-drag to translate.
 * Middle-drag to translate.
 * Scroll-wheel to zoom.
 * Double-click to recenter on point under cursor.
 * 
 * @author brunsc
 *
 */
public class TrackballInteractor 
implements MouseListener, MouseMotionListener, MouseWheelListener 
{
	private Camera3d camera;
	private Component component;
	private Point point;
	private Point previousPoint;

	public TrackballInteractor(Component component, Camera3d camera) {
		this.camera = camera;
		this.component = component;
		component.addMouseListener(this);
		component.addMouseMotionListener(this);
		component.addMouseWheelListener(this);
	}

	public Camera3d getCamera() {
		return camera;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		// Use Google maps convention of scroll wheel up to zoom in.
		// (Even though that makes no sense...)
		int notches = event.getWheelRotation();
		double zoomRatio = Math.pow(2.0, -notches/5.0);
		camera.setPixelsPerSceneUnit(zoomRatio*camera.getPixelsPerSceneUnit());
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		// Ignore right click
		if ((event.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			return;
		// Drag to rotate
		// shift-drag or middle-drag to translate
		boolean doRotate = true;
		if ((event.getModifiers() & InputEvent.BUTTON2_MASK) != 0)
			doRotate = false;
		if (event.isShiftDown())
			doRotate = false;
		previousPoint = point;
		point = event.getPoint();
		if (previousPoint == null)
			return;
		if (doRotate)
			rotate();
		else
			translate();
	}

	@Override
	public void mouseMoved(MouseEvent event) {}

	@Override
	public void mouseClicked(MouseEvent event) {
		// Ignore right click
		if ((event.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			return;
		previousPoint = null;
		point = event.getPoint();
		// double click to center on point
		if (event.getClickCount() == 2) {
			double cx = component.getWidth()/2.0;
			double cy = component.getHeight()/2.0;
			Vec3 dx = new Vec3(point.x-cx, point.y-cy, 0);
			dx = dx.times(1.0/camera.getPixelsPerSceneUnit());
			dx = camera.getRotation().times(dx);
			camera.setFocus(camera.getFocus().plus(dx));
		}
	}

	@Override
	public void mouseEntered(MouseEvent event) {}

	@Override
	public void mouseExited(MouseEvent event) {}

	@Override
	public void mousePressed(MouseEvent event) {
		// Ignore right click
		if ((event.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			return;
		previousPoint = null;
		point = event.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		previousPoint = null;
		point = event.getPoint();
	}
	
	private void rotate() {
		if (point == null)
			return;
		if (previousPoint == null)
			return;
		if (camera == null)
			return;
		Point p1 = previousPoint;
		Point p2 = point;
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		double dragDistance = Math.sqrt(dx*dx + dy*dy);
		if (dragDistance <= 0)
			return;
		UnitVec3 rotationAxis = new UnitVec3(dy, -dx, 0);
		int w = component.getWidth();
		int h = component.getHeight();
		double windowSize = Math.sqrt(w*w + h*h);
		if (windowSize <= 0)
			return;
		// Drag across the entire window to rotate all the way around
		double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;
		Rotation3d rotation = new Rotation3d().setFromAngleAboutUnitVector(
				rotationAngle, rotationAxis);
        camera.setRotation(camera.getRotation().times( rotation.transpose() ) );
	}

	private void translate() {
		if (point == null)
			return;
		if (previousPoint == null)
			return;
		if (camera == null)
			return;
		Point p1 = previousPoint;
		Point p2 = point;
		// Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
		Vec3 dx = new Vec3(p2.x - p1.x, p2.y - p1.y, 0.0);
	    Vec3 oldFocus = camera.getFocus();
	    // How much to move camera focus?
	    Vec3 dFocus = new Vec3(-dx.x(), -dx.y(), -dx.z());
	    // Convert from pixels to scene units
	    dFocus = dFocus.times(1.0/camera.getPixelsPerSceneUnit());
	    dFocus = camera.getRotation().times(dFocus);
	    // Nudge focus
	    Vec3 newFocus = oldFocus.plus(dFocus);
	    camera.setFocus(newFocus);		
	}

}
