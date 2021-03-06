package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import org.janelia.workstation.geom.Rotation3d;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.gui.large_volume_viewer.MenuItemGenerator;
import org.janelia.workstation.gui.large_volume_viewer.MouseModalWidget;
import org.janelia.workstation.gui.viewer3d.interfaces.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicMouseMode implements MouseMode
{
    private static final Logger log = LoggerFactory.getLogger(BasicMouseMode.class);
    
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
	protected Rotation3d viewerInGround = new Rotation3d();
	
	public static Cursor createCursor(String fileName, int x, int y) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		ImageIcon icon = Icons.getIcon(fileName);
		// Put 16x16 cursor inside Windows 32x32 cursor
		// to avoid "Hamburger Helper" look.
		Image iconImage = icon.getImage();
		Dimension imageSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
		Dimension cursorSize = toolkit.getBestCursorSize(imageSize.width, imageSize.height);
		if (! cursorSize.equals(imageSize)) {
			int w = (int)cursorSize.width;
			int h = (int)cursorSize.height;
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics g = bi.createGraphics();
			g.setColor(new Color(0f, 0f, 0f, 0f));
			g.fillRect(0, 0, w, h);
			icon.paintIcon(null, g, 0, 0);
			iconImage = bi;
		}
		Cursor cursor = toolkit.createCustomCursor(iconImage, new Point(x, y), fileName);
		return cursor;
	}

	protected void checkCursor(Cursor newCursor) {
		if (newCursor == null)
			return;
		currentCursor = newCursor;
		if (widget == null)
			return;
		if (widget.getComponent().getCursor() == currentCursor)
			return;
		widget.getComponent().setCursor(currentCursor);
	}
	
	public Point getPoint() {
		return point;
	}

	public Point getPreviousPoint() {
		return previousPoint;
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			checkCursor(dragCursor);
		}
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
		widget.getComponent().requestFocusInWindow();
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
		if ((event.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			checkCursor(dragCursor);
		}
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
	public MouseModalWidget getWidget() {
		return widget;
	}

	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		if (this.widget == widget)
			return;
		this.widget = widget;
		if (updateCursor)
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
	
	public Rotation3d getViewerInGround() {
        return viewerInGround;
    }

    public void setViewerInGround(Rotation3d viewerInGround) {
        this.viewerInGround = viewerInGround;
    }

    public Vec3 worldFromPixel(Point pixel) {
        boolean debug = false;
		// Initialize to screen space position
		Vec3 result = new Vec3(pixel.getX(), pixel.getY(), 0);
		if (debug) log.info("Mouse: "+result);
		// Normalize to screen viewport center
		Viewport vp = getWidget().getViewport();
		// TODO - test non-zero origins
		result = result.minus(new Vec3(vp.getOriginX(), vp.getOriginY(), 0)); // origin
		if (debug) log.info("After subtracting VP origin: "+result);
		result = result.minus(new Vec3(vp.getWidth()/2.0, vp.getHeight()/2.0, 0)); // center
		if (debug) log.info("After subtracting VP center: "+result);
		// Convert from pixel units to world units
		result = result.times(1.0/getCamera().getPixelsPerSceneUnit());
		if (debug) log.info("After converting to world units: "+result);
		// Apply viewer orientation, e.g. X/Y/Z orthogonal viewers
		result = viewerInGround.times(result);
		if (debug) log.info("After viewer in ground: "+result);
		// TODO - apply rotation, but only for rotatable viewers, UNLIKE large volume viewer
		// Apply camera focus
		result = result.plus(getCamera().getFocus());
		if (debug) log.info("After adding focus: "+result);
		// System.out.println(pixel + ", " + getCamera().getFocus() + ", " + result);
		return result;
	}
	
	public Point pixelFromWorld(Vec3 v) {
		Vec3 result = v;
		result = result.minus(getCamera().getFocus());
		// Apply viewer orientation, e.g. X/Y/Z orthogonal viewers
		result = viewerInGround.inverse().times(result);
		result = result.times(getCamera().getPixelsPerSceneUnit());
		// TODO - apply rotation, but only for rotatable viewers, UNLIKE large volume viewer
		Viewport vp = getWidget().getViewport();
		result = result.plus(new Vec3(vp.getWidth()/2.0, vp.getHeight()/2.0, 0));
		result = result.plus(new Vec3(vp.getOriginX(), vp.getOriginY(), 0));
		return new Point((int)Math.round(result.getX()), (int)Math.round(result.getY()));
	}

	@Override
	public String getToolTipText() {
		return null;
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
