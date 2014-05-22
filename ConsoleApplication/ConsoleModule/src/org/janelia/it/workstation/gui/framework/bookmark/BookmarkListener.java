package org.janelia.it.workstation.gui.framework.bookmark;

public interface BookmarkListener extends org.janelia.it.workstation.shared.preferences.PrefMgrListener {

  //  Adding Bookmark specific method to the listener
  void bookmarksChanged();

}