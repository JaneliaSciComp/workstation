package org.janelia.it.workstation.gui.framework.bookmark;

import org.janelia.it.workstation.shared.preferences.PrefMgrListener;

public interface BookmarkListener extends PrefMgrListener {

  //  Adding Bookmark specific method to the listener
  void bookmarksChanged();

}