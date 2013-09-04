package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.Cursor;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.MenuItemGenerator;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.MouseModalWidget;

// MouseMode manages the current mouse Mode for a Widget, e.g. Pan, Zoom, Rotate...
public interface MouseMode
extends MouseMotionListener, MouseListener, KeyListener
{
    static enum Mode {
        PAN,
        TRACE,
        ZOOM
    };
	public Cursor getAltCursor();
	public void setAltCursor(Cursor altCursor);
	public Cursor getDragCursor();
	public void setDragCursor(Cursor dragCursor);
	public Cursor getHoverCursor();
	public void setHoverCursor(Cursor hoverCursor);
	public MouseModalWidget getWidget();
	Camera3d getCamera();
	void setCamera(Camera3d camera);
	public String getToolTipText();
    public MenuItemGenerator getMenuItemGenerator();
	void setWidget(MouseModalWidget widget, boolean updateCursor);
}
