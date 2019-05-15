package org.janelia.workstation.core.api.lifecycle;

import java.io.IOException;
import java.util.logging.LogManager;

import javax.imageio.ImageIO;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationOpening;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.api.ConsoleApp;
import org.janelia.workstation.core.logging.LogFormatter;
import org.janelia.workstation.core.logging.NBExceptionHandler;
import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;
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
        System.setProperty("org.janelia.it.level", "INFO");
        
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

        // Put the app name in the Mac OS X menu bar
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", SystemInfo.appName);

        // Stretch NetBeans tabs across entire width of window. This allows us to show more of the long window titles.
        System.setProperty("winsys.stretching_view_tabs", "true");

        // Nicer to shutdown by closing all windows individually instead of just sending a System.exit(0) to the application
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

        // Work-around for NetBeans/OSXSierra bug which causes display issues if a resources cache file is loaded
        System.setProperty("org.netbeans.core.update.all.resources", "never");

        // Override the NetBeans default of "system-wide proxy", because it causes performance problems with VPN clients
        int proxyPref = NbPreferences.root().node("org/netbeans/core").getInt("proxyType", -1);
        if (proxyPref==-1) {
            LOG.info("Defaulting to direct non-proxy connection");
            NbPreferences.root().node("org/netbeans/core").putInt("proxyType", 0);
        }

        // disable ImageIO file caching for improved performance
        ImageIO.setUseCache(false);

        // Initialize the application
        ConsoleApp.getConsoleApp();

        // Notify listeners that the application is opening
        Events.getInstance().postOnEventBus(new ApplicationOpening());
    }
}
