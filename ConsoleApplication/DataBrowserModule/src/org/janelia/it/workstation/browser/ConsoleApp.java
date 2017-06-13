package org.janelia.it.workstation.browser;

import java.io.File;
import java.security.ProtectionDomain;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.api.LocalPreferenceMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.browser.gui.dialogs.GiantFiberSearchDialog;
import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog;
import org.janelia.it.workstation.browser.gui.dialogs.PatternSearchDialog;
import org.janelia.it.workstation.browser.gui.dialogs.ReleaseNotesDialog;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.ImageCache;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.openide.LifecycleManager;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * This is the main class for the workstation client, invoked by the NetBeans Startup hook. 
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleApp {

    private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);
    
    // Singleton
    private static ConsoleApp instance;
    public static synchronized ConsoleApp getConsoleApp() {
        if (instance==null) {
            instance = new ConsoleApp();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private final String appName;
    private final String appVersion;
    private final ImageCache imageCache;
    
    // Lazily initialized
    private PatternSearchDialog patternSearchDialog;
    private GiantFiberSearchDialog fiberSearchDialog;
    private ReleaseNotesDialog releaseNotesDialog;
    
    public ConsoleApp() {

        log.info("Initializing Console Application");
        
        this.appName = ConsoleProperties.getString("console.Title");
        this.appVersion = ConsoleProperties.getString("console.versionNumber");

        log.debug("Java version: " + System.getProperty("java.version"));
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        log.debug("Code Source: "+pd.getCodeSource().getLocation());
                
        // Put the menu bar on the application window, instead of in the Mac OS X menu bar
//        System.setProperty("apple.laf.useScreenMenuBar", "false");

        // Put the app name in the Mac OS X menu bar
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        
        // Stretch NetBeans tabs across entire width of window. This allows us to show more of the long window titles.
        System.setProperty("winsys.stretching_view_tabs", "true"); 
        
        // Nicer to shutdown by closing all windows individually instead of just sending a System.exit(0) to the application
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

        // Work-around for NetBeans/OSXSierra bug which causes display issues if a resources cache file is loaded
        System.setProperty("org.netbeans.core.update.all.resources", "never");
        
        // Init in-memory image cache
        this.imageCache = new ImageCache();

        // Workaround for NetBeans Sierra rendering issues
        findAndRemoveAllResourcesFile();
        
        // Minor hack for running NetBeans on Windows 
        findAndRemoveWindowsSplashFile();
    }
    
    public void initSession() {
        log.info("Initializing Session");
        try {
            // Read local user preferences
            LocalPreferenceMgr prefs = LocalPreferenceMgr.getInstance();

            // Must init file services BEFORE calling AccessManager.loginSubject
            FileMgr.getFileMgr();
            
            // Try saved credentials
            String username = (String)prefs.getModelProperty(AccessManager.USER_NAME);
            String password = (String)prefs.getModelProperty(AccessManager.USER_PASSWORD);
            String runAsUser = (String)prefs.getModelProperty(AccessManager.RUN_AS_USER);
            String email = (String)prefs.getModelProperty(AccessManager.USER_EMAIL);

            if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                AccessManager.getAccessManager().loginSubject(username, password);
            }
            
            if (!AccessManager.getAccessManager().isLoggedIn() || email==null) {
                // If it didn't work for any reason, show the login dialog
                LoginDialog loginDialog = new LoginDialog();
                loginDialog.showDialog();
            }

            email = (String)prefs.getModelProperty(AccessManager.USER_EMAIL);
            
            if (!AccessManager.getAccessManager().isLoggedIn() || email==null) {
                log.warn("User closed login window without successfully logging in, exiting program.");
                LifecycleManager.getDefault().exit(0);
            }

            // Set run-as user if any
            try {
                AccessManager.getAccessManager().setRunAsUser(runAsUser);
            }
            catch (Exception e) {
                prefs.setModelProperty(AccessManager.RUN_AS_USER, "");
                ConsoleApp.handleException(e);
            }
            
            // Things that can be lazily initialized 
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    patternSearchDialog = new PatternSearchDialog();
                    fiberSearchDialog = new GiantFiberSearchDialog();
                    releaseNotesDialog = new ReleaseNotesDialog();
                    releaseNotesDialog.showIfFirstRunSinceUpdate();
                }
            });
        }
        catch (Throwable e) {
            ConsoleApp.handleException(e);
            LifecycleManager.getDefault().exit(0);
        }
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
                log.info("Cached all-resources file "+evilCachedResourcesFile+" exists.  Removing...");
                boolean deleteSuccess = evilCachedResourcesFile.delete();
                if (deleteSuccess) {
                    log.info("Successfully removed the all-resources.dat file");
                }
                else {
                    log.warn("Could not successfully removed the all-resources.dat file");
                }
            }
            else {
                log.debug("Did not find the cached all-resources.dat file ("+evilCachedResourcesFile+"). Continuing...");
            }
        }
        catch (Exception e) {
            log.error("Ignoring error trying to exorcise the all-resources.dat", e);
        }
    }
    
    /**
     * Method to work-around a problem with the NetBeans Windows integration
     * todo Formally submit a bug report and tell Geertjan
     */
    private void findAndRemoveWindowsSplashFile() {
        try {
            if (SystemInfo.isWindows) {
                File evilCachedSplashFile = Places.getCacheSubfile("splash.png");
                if (evilCachedSplashFile.exists()) {
                    log.info("Cached splash file "+evilCachedSplashFile+" exists.  Removing...");
                    boolean deleteSuccess = evilCachedSplashFile.delete();
                    if (deleteSuccess) {
                        log.info("Successfully removed the splash.png file");
                    }
                    else {
                        log.warn("Could not successfully removed the splash.png file");
                    }
                }
                else {
                    log.debug("Did not find the cached splash file ("+evilCachedSplashFile+").  Continuing...");
                }
            }
        }
        catch (Exception e) {
            log.error("Ignoring error trying to exorcise the splash file on Windows", e);
        }
    }

    private static JFrame mainFrame;
    public static JFrame getMainFrame() {
        if (mainFrame == null) {
            try {
                mainFrame = WindowLocator.getMainFrame();
            }
            catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
        }
        return mainFrame;
    }
    
    public String getApplicationName() {
        return appName;
    }

    public String getApplicationVersion() {
        return appVersion;
    }

    public static void handleException(Throwable throwable) {
        getConsoleApp().handle(throwable);
    }
    
    void handle(Throwable throwable) {
        FrameworkImplProvider.handleException(throwable);
    }

    public String getApplicationOutputDirectory() {
        return LocalPreferenceMgr.getInstance().getApplicationOutputDirectory();
    }
    
    public Object setModelProperty(Object key, Object value) {
        return LocalPreferenceMgr.getInstance().setModelProperty(key, value);
    }

    public Object getModelProperty(Object key) {
        return LocalPreferenceMgr.getInstance().getModelProperty(key);
    }

    public ImageCache getImageCache() {
        return imageCache;
    }
    
    public ReleaseNotesDialog getReleaseNotesDialog() {
        return releaseNotesDialog;
    }

    public PatternSearchDialog getPatternSearchDialog() {
        return patternSearchDialog;
    }

    public GiantFiberSearchDialog getGiantFiberSearchDialog() {
        return fiberSearchDialog;
    }
    
    @Subscribe
    public void systemWillExit(ApplicationClosing closingEvent) {
        log.info("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");
        findAndRemoveWindowsSplashFile();
    }
}
