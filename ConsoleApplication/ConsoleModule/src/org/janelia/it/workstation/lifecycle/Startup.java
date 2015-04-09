package org.janelia.it.workstation.lifecycle;

import org.janelia.it.workstation.gui.application.ConsoleApp;
import org.openide.modules.OnStart;

/**
 * This is run at startup.
 *
 * @author fosterl
 */
@OnStart
public class Startup implements Runnable {
    @Override
    public void run() {
        // Tie NetBeans's error handling popup to the workstation's error handler
        java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());
        // Create the browser
        ConsoleApp.newBrowser();
    }
}
