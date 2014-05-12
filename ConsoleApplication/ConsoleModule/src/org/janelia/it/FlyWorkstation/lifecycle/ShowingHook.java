/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.lifecycle;

import java.awt.Component;
import java.awt.Container;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import org.janelia.it.FlyWorkstation.gui.framework.progress_meter.WorkerProgressMeter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

/**
 * This carries out tasks that must be done at startup, but may only be done 
 * when the application is ready to show.
 * 
 * @author fosterl
 */
@OnShowing
public class ShowingHook implements Runnable {
    public void run() {
        JFrame frame = (JFrame) WindowManager.getDefault().getMainWindow();
        String title = ConsoleProperties.getString("console.Title") + " " + ConsoleProperties.getString("console.versionNumber");
        frame.setTitle( title );
        SessionMgr.getBrowser().supportMenuProcessing();
    }

}
