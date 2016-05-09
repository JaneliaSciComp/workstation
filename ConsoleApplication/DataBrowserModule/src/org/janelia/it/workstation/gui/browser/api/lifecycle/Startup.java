package org.janelia.it.workstation.gui.browser.api.lifecycle;

import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.janelia.it.workstation.gui.browser.api.ServiceMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.modules.OnStart;

/**
 * The main hook for starting up the Workstation Browser. 
 *  
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@OnStart
public class Startup implements Runnable {
    @Override
    public void run() {
        
        // Tie NetBeans's error handling popup to the workstation's error handler
        java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());
        
        // Create the main console app frame
        ConsoleApp.getConsoleApp();
        
        // Once the main frame is visible, we can do some things in the background
        SimpleWorker worker = new SimpleWorker() {
                
            @Override
            protected void doStuff() throws Exception {
                // Initialize the services
                ServiceMgr.getServiceMgr().initServices();
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        };

        worker.execute();
    }
}
