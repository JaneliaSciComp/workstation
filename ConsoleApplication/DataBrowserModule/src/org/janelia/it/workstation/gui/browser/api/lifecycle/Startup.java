package org.janelia.it.workstation.gui.browser.api.lifecycle;

import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.janelia.it.workstation.gui.browser.api.ServiceMgr;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.lifecycle.ApplicationOpening;
import org.janelia.it.workstation.gui.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.gui.browser.nb_action.NavigateForward;
import org.janelia.it.workstation.gui.browser.workers.SimpleWorker;
import org.openide.modules.OnStart;
import org.openide.util.actions.CallableSystemAction;

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
        ConsoleApp app = ConsoleApp.getConsoleApp();
        
        // Set the Look and Feel
        StateMgr.getStateMgr().initLAF();
        
        // Begin the user's session
        app.initSession();
                
        // Do some things in the background
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
                ConsoleApp.handleException(e);
            }
        };

        worker.execute();

        // Disable the navigation actions until there is some history to navigate
        CallableSystemAction.get(NavigateBack.class).setEnabled(false);
        CallableSystemAction.get(NavigateForward.class).setEnabled(false);

        // Notify listeners that the application is opening
        Events.getInstance().postOnEventBus(new ApplicationOpening());
    }
}
