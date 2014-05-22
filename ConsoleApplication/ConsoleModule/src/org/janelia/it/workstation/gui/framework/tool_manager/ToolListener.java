package org.janelia.it.workstation.gui.framework.tool_manager;

public interface ToolListener extends org.janelia.it.workstation.shared.preferences.PrefMgrListener {

  //  Adding Tools specific method to the listener
  void toolsChanged();

}