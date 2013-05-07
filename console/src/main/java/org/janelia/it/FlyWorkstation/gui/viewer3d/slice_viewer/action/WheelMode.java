package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.MouseWheelListener;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.MouseModalWidget;

public interface WheelMode
extends MouseWheelListener
{
	public MouseModalWidget getComponent();
	public void setComponent(MouseModalWidget widget);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
