/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.lifecycle;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.openide.windows.OnShowing;

/**
 *
 * @author fosterl
 */
@OnShowing
public class ShowingHook implements Runnable {
    public void run() {
        SessionMgr.getBrowser().supportMenuProcessing();
    }
}
