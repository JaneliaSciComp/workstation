package org.janelia.it.FlyWorkstation.lifecycle;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.openide.modules.OnStart;

/**
 * 
 * @author fosterl
 */
@OnStart
public class Startup implements Runnable {
    public void run() {
        ConsoleApp.newBrowser();
    }
}
