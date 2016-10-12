package org.janelia.it.workstation.gui.browser.tools;

import org.janelia.it.workstation.gui.browser.tools.preferences.PrefMgrListener;

public interface ToolListener extends PrefMgrListener {

  //  Adding Tools specific method to the listener
  void toolsChanged();

}