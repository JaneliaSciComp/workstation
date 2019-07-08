package org.janelia.workstation.core.api.lifecycle;

import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;

import javax.imageio.ImageIO;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.api.LocalPreferenceMgr;
import org.janelia.workstation.core.api.ServiceMgr;
import org.janelia.workstation.core.api.SessionMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.events.lifecycle.ApplicationOpening;
import org.janelia.workstation.core.logging.LogFormatter;
import org.janelia.workstation.core.logging.NBExceptionHandler;
import org.janelia.workstation.core.util.BrandingConfig;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.modules.OnStart;
import org.openide.modules.Places;
import org.openide.util.NbPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main hook for starting up the Workstation Browser. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 * @author Todd Safford
 * @author Les Foster
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
        System.setProperty("org.janelia.level", "INFO");

        /*
         * Reduce logging level for this class to avoid WARNING spam about null popups from node actions.
         */
        System.setProperty("org.openide.util.Utilities.level", "SEVERE");
        
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

        // Workaround for NetBeans Sierra rendering issues
        findAndRemoveAllResourcesFile();

        // Minor hack for running NetBeans on Windows
        findAndRemoveWindowsSplashFile();

        // Load the branding config so that the user settings are available for logging
        // in the next step (init user session)
        BrandingConfig.getBrandingConfig().validateBrandingConfig();

        try {
            // Set the Look and Feel
            StateMgr.getStateMgr().initLAF();
            // Initialize all singletons so that they are listening on Event Bus
            FileMgr.getFileMgr();
            LocalPreferenceMgr.getInstance();
            DomainMgr.getDomainMgr();
            AccessManager.getAccessManager();
            SessionMgr.getSessionMgr();

        }
        catch (Throwable e) {
            FrameworkAccess.handleException(e);
        }

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
                FrameworkAccess.handleException(e);
            }
        };

        worker.execute();

        LOG.info("Startup sequence complete. Opening the application...");

        // Notify listeners that the application is opening
        ConsoleState.setCurrState(ConsoleState.STARTING_SESSION);
        Events.getInstance().postOnEventBus(new ApplicationOpening());

        // Listen for application closing event
        Events.getInstance().registerOnEventBus(this);
    }

    /**
     * This is part of a workaround for JW-25338 which is rendering issues for a combination of NetBeans 7.4 with Synthetica themes on Mac OS X Sierra.
     *
     * The other part of the workaround prevents this file from being generated in the future, by setting
     * -Dorg.netbeans.core.update.all.resources=never on the startup command line.
     */
    private void findAndRemoveAllResourcesFile() {
        try {
            File evilCachedResourcesFile = Places.getCacheSubfile("all-resources.dat");
            if (evilCachedResourcesFile.exists()) {
                LOG.info("Cached all-resources file "+evilCachedResourcesFile+" exists.  Removing...");
                boolean deleteSuccess = evilCachedResourcesFile.delete();
                if (deleteSuccess) {
                    LOG.info("Successfully removed the all-resources.dat file");
                }
                else {
                    LOG.warn("Could not successfully removed the all-resources.dat file");
                }
            }
            else {
                LOG.debug("Did not find the cached all-resources.dat file ("+evilCachedResourcesFile+"). Continuing...");
            }
        }
        catch (Exception e) {
            LOG.error("Ignoring error trying to exorcise the all-resources.dat", e);
        }
    }

    /**
     * Method to work-around a problem with the NetBeans Windows integration
     */
    private void findAndRemoveWindowsSplashFile() {
        try {
            if (SystemInfo.isWindows) {
                File evilCachedSplashFile = Places.getCacheSubfile("splash.png");
                if (evilCachedSplashFile.exists()) {
                    LOG.info("Cached splash file "+evilCachedSplashFile+" exists.  Removing...");
                    boolean deleteSuccess = evilCachedSplashFile.delete();
                    if (deleteSuccess) {
                        LOG.info("Successfully removed the splash.png file");
                    }
                    else {
                        LOG.warn("Could not successfully removed the splash.png file");
                    }
                }
                else {
                    LOG.debug("Did not find the cached splash file ("+evilCachedSplashFile+").  Continuing...");
                }
            }
        }
        catch (Exception e) {
            LOG.error("Ignoring error trying to exorcise the splash file on Windows", e);
        }
    }

    @Subscribe
    public void systemWillExit(ApplicationClosing closingEvent) {
        LOG.info("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");
        findAndRemoveWindowsSplashFile();
    }
}
