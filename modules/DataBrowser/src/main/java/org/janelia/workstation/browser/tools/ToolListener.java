package org.janelia.workstation.browser.tools;

import org.janelia.workstation.browser.tools.preferences.PrefMgrListener;

public interface ToolListener extends PrefMgrListener {

  //  Adding Tools specific method to the listener
  void toolsChanged();

}