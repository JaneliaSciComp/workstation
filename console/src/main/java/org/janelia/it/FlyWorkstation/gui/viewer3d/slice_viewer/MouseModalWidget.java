package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.WheelMode;

public interface MouseModalWidget 
extends MouseListener, MouseMotionListener, MouseWheelListener
{
	public MouseMode getMouseMode();
	public void setMouseMode(MouseMode mode);
	public WheelMode getWheelMode();
	public void setWheelMode(WheelMode mode);
	public Cursor getCursor();
	public void setCursor(Cursor cursor);
	public Point2D getPixelOffsetFromCenter(Point2D point);
	public RubberBand getRubberBand();
	// public Dimension getViewportSize();
	public Viewport getViewport();
	public JComponent getComponent();
}
