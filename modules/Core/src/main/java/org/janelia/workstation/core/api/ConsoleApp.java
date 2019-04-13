package org.janelia.workstation.core.api;

import java.io.File;
import java.security.ProtectionDomain;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.lifecycle.ConsoleState;
import org.janelia.workstation.core.api.lifecycle.GracefulBrick;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.util.BrandingConfig;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.openide.LifecycleManager;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class for the workstation client, invoked by the NetBeans Startup hook. 
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleApp {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleApp.class);

    // Singleton
    private static ConsoleApp instance;
    public static synchronized ConsoleApp getConsoleApp() {
        if (instance==null) {
            instance = new ConsoleApp();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private static boolean brandingValidationException = false;

    private ConsoleApp() {

        LOG.info("Initializing Console Application");
        LOG.debug("Java version: " + System.getProperty("java.version"));
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        LOG.debug("Code Source: "+pd.getCodeSource().getLocation());

        // Workaround for NetBeans Sierra rendering issues
        findAndRemoveAllResourcesFile();

        // Minor hack for running NetBeans on Windows
        findAndRemoveWindowsSplashFile();

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
        LOG.info("Initializing Session");
        ConsoleState.setCurrState(ConsoleState.STARTING_SESSION);

        try {
            // Init singletons so that they're initialized by a single thread
            LocalPreferenceMgr.getInstance();
            AccessManager.getAccessManager();
            DomainMgr.getDomainMgr();
            FileMgr.getFileMgr();
            SessionMgr.getSessionMgr();

            // Set the Look and Feel
            StateMgr.getStateMgr().initLAF();

            // Auto-login if credentials were saved from a previous session
            AccessManager.getAccessManager().loginUsingSavedCredentials();

            // Check for potential remote brick
            try {
                GracefulBrick uninstaller = new GracefulBrick();
                uninstaller.brickAndUninstall();
            }
            catch (Exception e) {
                FrameworkImplProvider.handleException(e);
            }
        }
        catch (Throwable e) {
            FrameworkImplProvider.handleException(e);
            LifecycleManager.getDefault().exit(0);
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
                FrameworkImplProvider.handleException(e);
            }
        };

        worker.execute();
    }

    public static boolean isBrandingValidationException() {
        return brandingValidationException;
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
