package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.MouseWheelListener;

public interface WheelMode
extends MouseWheelListener
{
    enum Mode {
        ZOOM,
        SCAN
    }
	public org.janelia.it.workstation.gui.slice_viewer.MouseModalWidget getWidget();
	public void setWidget(org.janelia.it.workstation.gui.slice_viewer.MouseModalWidget widget, boolean updateCursor);
	org.janelia.it.workstation.gui.camera.Camera3d getCamera();
	void setCamera(org.janelia.it.workstation.gui.camera.Camera3d camera);
}
