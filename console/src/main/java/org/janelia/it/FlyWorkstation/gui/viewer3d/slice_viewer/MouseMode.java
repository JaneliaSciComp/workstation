package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;

// MouseMode manages the current mouse Mode for a Widget, e.g. Pan, Zoom, Rotate...
public interface MouseMode
extends MouseMotionListener, MouseListener
{
	public Cursor getDragCursor();
	public void setDragCursor(Cursor dragCursor);
	public Cursor getHoverCursor();
	public void setHoverCursor(Cursor hoverCursor);
	public Component getComponent();
	public void setComponent(Component widget);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
