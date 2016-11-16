package org.janelia.it.workstation.browser.api.lifecycle;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ServiceMgr;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationOpening;
import org.janelia.it.workstation.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.browser.nb_action.NavigateForward;
import org.janelia.it.workstation.browser.util.BrandingConfig;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.openide.modules.OnStart;
import org.openide.util.actions.CallableSystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main hook for starting up the Workstation Browser. 
 *  
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@OnStart
public class Startup implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);
    
    private static boolean brandingValidationException = false;
    
    @Override
    public void run() {
        
        // Tie NetBeans's error handling popup to the workstation's error handler
        java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());

        // Override the default formatters with the custom formatter
        LogFormatter formatter = new LogFormatter(); // Custom formatter
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(""); // Root logger
        java.util.logging.Handler[] handlers = logger.getHandlers();
        for (java.util.logging.Handler handler : handlers) {
            handler.setFormatter(formatter);
        }
        
        // Create the main console app frame
        ConsoleApp app = ConsoleApp.getConsoleApp();
        
        // Set the Look and Feel
        StateMgr.getStateMgr().initLAF();

        // Load the branding config so that the user settings are available for logging 
        // in the next step (init user session)
        try {
            BrandingConfig.getBrandingConfig().validateBrandingConfig();
        }
        catch (Throwable t) {
            log.error("Error validating branding config", t);
            // Save this error state so that it can be shown to the user later, once the MainWindow is visible.
            brandingValidationException = true;
        }
        
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

    public static boolean isBrandingValidationException() {
        return brandingValidationException;
    }
}
