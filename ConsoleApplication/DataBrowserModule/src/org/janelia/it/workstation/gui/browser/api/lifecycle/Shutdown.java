package org.janelia.it.workstation.gui.browser.api.lifecycle;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.modules.OnStop;

/**
 * This hook declares itself to be called when NetBeans RCP/Platform decides
 * to exit (in response to users clicking an 'Exit' button).
 *
 * @author fosterl
 */
@OnStop
public class Shutdown implements Runnable {
    public void run() {
        SessionMgr.getSessionMgr().systemWillExit();
    }
}
