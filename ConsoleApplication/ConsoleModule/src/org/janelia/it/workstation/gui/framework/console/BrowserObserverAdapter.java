package org.janelia.it.workstation.gui.framework.console;

/**
 * Title:        Genome Browser Client
 * Description:  This project is for JBuilder 4.0
 * @author
 * @version $Id: BrowserObserverAdapter.java,v 1.2 2011/03/08 16:16:43 saffordt Exp $
 */

public abstract class BrowserObserverAdapter implements BrowserObserver {

  public void openBrowserCountChanged(int browserCount) {  }
//  public void masterEditorChanged(String newMasterEditor, boolean subEditorAvailable) {  }
//  public void editorSpecificMenusChanged(JMenuItem[] menus) {  }

}