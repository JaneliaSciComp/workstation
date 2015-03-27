package org.janelia.it.workstation.lifecycle;

import javax.swing.JFrame;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.openide.windows.OnShowing;

/**
 * This carries out tasks that must be done at startup, but may only be done 
 * when the application is ready to show.
 * 
 * @author fosterl
 */
@OnShowing
public class ShowingHook implements Runnable {
    public void run() {
        JFrame frame = WindowLocator.getMainFrame();
        String title = ConsoleProperties.getString("console.Title") + " " + ConsoleProperties.getString("console.versionNumber");
        frame.setTitle( title );
        SessionMgr.getBrowser().supportMenuProcessing();
    }

}
