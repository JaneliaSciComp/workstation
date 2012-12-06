package org.janelia.it.FlyWorkstation.gui.framework.tool_manager;

import org.janelia.it.FlyWorkstation.shared.preferences.PrefMgrListener;

public interface ToolListener extends PrefMgrListener {

  //  Adding Tools specific method to the listener
  void toolsChanged();

}