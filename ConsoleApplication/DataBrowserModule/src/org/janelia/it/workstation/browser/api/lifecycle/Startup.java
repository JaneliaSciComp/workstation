package org.janelia.it.workstation.browser.api.lifecycle;

import java.io.IOException;
import java.util.logging.LogManager;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.ServiceMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationOpening;
import org.janelia.it.workstation.browser.logging.LogFormatter;
import org.janelia.it.workstation.browser.logging.NBExceptionHandler;
import org.janelia.it.workstation.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.browser.nb_action.NavigateForward;
import org.janelia.it.workstation.browser.util.BrandingConfig;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;
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

    private static final Logger LOG = LoggerFactory.getLogger(Startup.class);
    
    private static boolean brandingValidationException = false;
    
    @Override
    public void run() {

        // Configure default logging levels
        /*
         * level jul_name    slf4j_name
         * 3000  USER_ERROR 
         * 2000  USER_WARN 
         * 1000  SEVERE      "error"
         *  900  WARNING     "warn"
         *  800  INFO        "info"
         *  700  CONFIG  
         *  500  FINE        "debug"
         *  400  FINER 
         *  300  FINEST      "trace"
         *       ALL
         */
        System.setProperty("org.janelia.it.level", "FINE");
        //System.setProperty("org.janelia.it.workstation.browser.gui.dialogs.download.level", "FINEST");
        
        try {
            // Re-read the configuration to parse the system properties we just defined
            LogManager.getLogManager().readConfiguration();
        }
        catch (IOException e) {
            LOG.error("Problem encountered configuring logging levels", e);
        }
        
        // Get root logger
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(""); 
        
        // Tie NetBeans's error handling popup to the workstation's error handler
        logger.addHandler(new NBExceptionHandler());

        // Override the default formatters with the custom formatter
        LogFormatter formatter = new LogFormatter(); // Custom formatter
        for (java.util.logging.Handler handler : logger.getHandlers()) {
            handler.setFormatter(formatter);
        }

        // Override the NetBeans default of "system-wide proxy", because it causes performance problems with VPN clients
        int proxyPref = NbPreferences.root().node("org/netbeans/core").getInt("proxyType", -1);
        if (proxyPref==-1) {
            LOG.info("Defaulting to direct non-proxy connection");
            NbPreferences.root().node("org/netbeans/core").putInt("proxyType", 0);
        }

        // Create the main console app frame
        ConsoleApp app = ConsoleApp.getConsoleApp();
        
        // Load the branding config so that the user settings are available for logging 
        // in the next step (init user session)
        try {
            BrandingConfig.getBrandingConfig().validateBrandingConfig();
        }
        catch (Throwable t) {
            LOG.error("Error validating branding config", t);
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
