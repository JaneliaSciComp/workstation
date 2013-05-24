package org.janelia.it.FlyWorkstation.gui.framework.bookmark;

import org.janelia.it.FlyWorkstation.shared.preferences.PrefMgrListener;

public interface BookmarkListener extends PrefMgrListener {

  //  Adding Bookmark specific method to the listener
  void bookmarksChanged();

}