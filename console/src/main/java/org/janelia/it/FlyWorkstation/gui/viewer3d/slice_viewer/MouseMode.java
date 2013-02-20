package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;

// MouseMode manages the current mouse Mode for a Widget, e.g. Pan, Zoom, Rotate...
public interface MouseMode
extends MouseMotionListener, MouseListener
{
	public Cursor getAltCursor();
	public void setAltCursor(Cursor altCursor);
	public Cursor getDragCursor();
	public void setDragCursor(Cursor dragCursor);
	public Cursor getHoverCursor();
	public void setHoverCursor(Cursor hoverCursor);
	public MouseModalWidget getComponent();
	public void setComponent(MouseModalWidget widget);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
