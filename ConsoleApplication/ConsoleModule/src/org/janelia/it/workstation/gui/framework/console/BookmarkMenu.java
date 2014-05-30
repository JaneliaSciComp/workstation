package org.janelia.it.workstation.gui.framework.console;

import org.janelia.it.workstation.gui.framework.bookmark.BookmarkInfo;
import org.janelia.it.workstation.gui.framework.bookmark.BookmarkListener;
import org.janelia.it.workstation.gui.framework.bookmark.BookmarkMgr;
import org.janelia.it.workstation.gui.framework.bookmark.BookmarkTableDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;


public class BookmarkMenu extends JMenu implements BookmarkListener {

  public Browser browser;
  private MyBrowserListener browserListener = new MyBrowserListener();
  private Entity lastSelection;
  private JMenuItem addBookmarkMI, bookmarkMI;

  public BookmarkMenu(Browser b) {
    this.browser=b;
    browser.getBrowserModel().addBrowserModelListener(browserListener);
    BookmarkMgr.getBookmarkMgr().registerPrefMgrListener(this);
    setText("Bookmark");
    this.setMnemonic('B');

    addBookmarkMI=new JMenuItem("Bookmark Current Selection", 'B');
    addBookmarkMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK, false));
    addBookmarkMI.setEnabled(false);
    addBookmarkMI.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        BookmarkMgr.getBookmarkMgr().addBookmark(new BookmarkInfo(lastSelection));
      }
    });

    bookmarkMI=new JMenuItem("Select Bookmark...", 'r');
    bookmarkMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK, false));
    bookmarkMI.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
         new BookmarkTableDialog(SessionMgr.getMainFrame(), BookmarkMgr.getBookmarkMgr().getBookmarks());
      }
    });
    createMenu();
  }

  public void bookmarksChanged () {
    createMenu();
  }
  public void preferencesChanged() {}

  private void createMenu() {
    removeAll();
    add(addBookmarkMI);
    add(bookmarkMI);
   }


  public void dispose() {
    browser.getBrowserModel().removeBrowserModelListener(browserListener);
  }


  class MyBrowserListener extends BrowserModelListenerAdapter {
    public void browserCurrentSelectionChanged(Entity newSelection){
      lastSelection=newSelection;
//      if (lastSelection!=null && addBookmarkMI!=null && !newSelection.hasNullID()) addBookmarkMI.setEnabled(true);
//      if (lastSelection!=null && addBookmarkMI!=null && newSelection.hasNullID()) addBookmarkMI.setEnabled(false);
      if (lastSelection==null && addBookmarkMI!=null) addBookmarkMI.setEnabled(false);
    }
  }
}
