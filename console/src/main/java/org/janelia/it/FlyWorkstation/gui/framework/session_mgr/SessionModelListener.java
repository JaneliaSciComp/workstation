package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;


/**
* Interface for objects which want to track the addition and
*   destruction of browser windows.
*
* Initially written by: Peter Davies
*/
public interface SessionModelListener extends GenericModelListener {
    /**
    * The browserModel sent will be the Model associated with the
    * new browser.
    */
    public void browserAdded(BrowserModel browserModel);

    /**
    * The browserModel sent will be the Model associated with the
    * disposed Browser
    */
    public void browserRemoved(BrowserModel browserModel);

    /**
     * Called when the session is about to exit
     */
    public void sessionWillExit();
}
