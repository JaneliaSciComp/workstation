package org.janelia.it.workstation.lifecycle;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.modules.OnStop;

/**
 *
 * @author fosterl
 */
@OnStop
public class Shutdown implements Runnable {
    public void run() {
        SessionMgr.getSessionMgr().systemExit();
    }
}
