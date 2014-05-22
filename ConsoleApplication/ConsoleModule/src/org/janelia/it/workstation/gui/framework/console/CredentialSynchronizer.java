package org.janelia.it.workstation.gui.framework.console;

/**
 * Created by IntelliJ IDEA.
 * This puts login and password info into the console properties.  Checking the login
 * save checkbox writes out to the session-persistent collection object.
 * This code formerly resided in the FileMenu which has been removed in favor
 * of the NetBeans framework-based menu.
 *
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 */
public class CredentialSynchronizer {

    public void synchronize( Browser browser ) {
        browser.getBrowserModel().setModelProperty("LOGIN", org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty("LOGIN"));
        browser.getBrowserModel().setModelProperty("PASSWORD", org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty("PASSWORD"));

    }

}
