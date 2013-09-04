package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.WheelMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;

public interface MouseModalWidget 
extends MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public void setMouseMode(MouseMode.Mode mode);
	public void setWheelMode(WheelMode.Mode mode);
	public Point2D getPixelOffsetFromCenter(Point2D point);
	public RubberBand getRubberBand();
	public Viewport getViewport();
	public JComponent getComponent();
	public MouseMode getMouseMode();
}
