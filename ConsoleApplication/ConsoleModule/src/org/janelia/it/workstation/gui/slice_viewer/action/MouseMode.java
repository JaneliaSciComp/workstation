package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.Cursor;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

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
	public org.janelia.it.workstation.gui.slice_viewer.MouseModalWidget getWidget();
	org.janelia.it.workstation.gui.camera.Camera3d getCamera();
	void setCamera(org.janelia.it.workstation.gui.camera.Camera3d camera);
	public String getToolTipText();
    public org.janelia.it.workstation.gui.slice_viewer.MenuItemGenerator getMenuItemGenerator();
	void setWidget(org.janelia.it.workstation.gui.slice_viewer.MouseModalWidget widget, boolean updateCursor);
}
