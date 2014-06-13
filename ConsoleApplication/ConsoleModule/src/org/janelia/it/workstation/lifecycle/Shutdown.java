/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
