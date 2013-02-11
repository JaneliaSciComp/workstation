package org.janelia.it.FlyWorkstation.gui.viewer3d.camera;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.QtSignal;

public interface ObservableCamera3d
extends Camera3d
{
	public abstract QtSignal<Object> getViewChangedSignal();
}
