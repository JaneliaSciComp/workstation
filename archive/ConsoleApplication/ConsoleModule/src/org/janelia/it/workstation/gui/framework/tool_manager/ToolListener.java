package org.janelia.it.workstation.gui.framework.tool_manager;

import org.janelia.it.workstation.shared.preferences.PrefMgrListener;

public interface ToolListener extends PrefMgrListener {

  //  Adding Tools specific method to the listener
  void toolsChanged();

}