package org.janelia.it.workstation.gui.browser;

import java.io.File;
import java.security.ProtectionDomain;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.FileMgr;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.gui.browser.gui.dialogs.GiantFiberSearchDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.LoginDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.MaskSearchDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.PatternSearchDialog;
import org.janelia.it.workstation.gui.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.browser.util.LocalPreferences;
import org.janelia.it.workstation.gui.browser.util.SystemInfo;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.tools.ToolMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.gui.util.server_status.ServerStatusReportManager;
import org.openide.LifecycleManager;
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
    private static final ConsoleApp consoleApp = new ConsoleApp();
    public static synchronized ConsoleApp getConsoleApp() {
        return consoleApp;
    }
    
    private UserNotificationExceptionHandler exceptionHandler = new UserNotificationExceptionHandler();

    private LocalPreferences prefs;
    
    private static PatternSearchDialog patternSearchDialog;
    private static GiantFiberSearchDialog fiberSearchDialog;
    private static MaskSearchDialog maskSearchDialog;

    private String appName;
    private String appVersion;
    
    public ConsoleApp() {

        // Minor hack
        findAndRemoveWindowsSplashFile();
        
        // Load properties
        ConsoleProperties.load();
        this.appName = ConsoleProperties.getString("console.Title");
        this.appVersion = ConsoleProperties.getString("console.versionNumber");

        log.debug("Java version: " + System.getProperty("java.version"));
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        log.debug("Code Source: "+pd.getCodeSource().getLocation());
        
        // System properties
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("winsys.stretching_view_tabs", "true");
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        
        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");

        // Load local preferences
        this.prefs = new LocalPreferences();
        
        // Init singletons
        FileMgr fileMgr = FileMgr.getFileMgr();
        StateMgr stateMgr = StateMgr.getStateMgr();
        ToolMgr toolMgr = ToolMgr.getToolMgr();
        
        try {
            ServerStatusReportManager.getReportManager().startCheckingForReport();

            // Assuming that the user has entered the login/password information, now validate
            String username = (String)SessionMgr.getSessionMgr().getModelProperty(AccessManager.USER_NAME);
            String password = (String)SessionMgr.getSessionMgr().getModelProperty(AccessManager.USER_PASSWORD);
            String runAsUser = (String) SessionMgr.getSessionMgr().getModelProperty(AccessManager.RUN_AS_USER);
            String email = (String)SessionMgr.getSessionMgr().getModelProperty(AccessManager.USER_EMAIL);

            if (username!=null) {
                AccessManager.getAccessManager().loginSubject(username, password);
            }
            
            if (!AccessManager.getAccessManager().isLoggedIn() || email==null) {
                LoginDialog loginDialog = new LoginDialog();
                loginDialog.showDialog();
            }

            email = (String)SessionMgr.getSessionMgr().getModelProperty(AccessManager.USER_EMAIL);
            
            if (!AccessManager.getAccessManager().isLoggedIn() || email==null) {
                log.warn("User closed login window without successfully logging in, exiting program.");
                LifecycleManager.getDefault().exit(0);
            }

            try {
                AccessManager.getAccessManager().setRunAsUser(runAsUser);
            }
            catch (Exception e) {
                setModelProperty(AccessManager.RUN_AS_USER, "");
                SessionMgr.getSessionMgr().handleException(e);
            }
                        
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    patternSearchDialog = new PatternSearchDialog();
                    fiberSearchDialog = new GiantFiberSearchDialog();
                    maskSearchDialog = new MaskSearchDialog();
                }
            });
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
            LifecycleManager.getDefault().exit(0);
        }
    }

    /**
     * Method to work-around a problem with the NetBeans Windows integration
     * todo Formally submit a bug report and tell Geertjan
     */
    private void findAndRemoveWindowsSplashFile() {
        try {
            if (SystemInfo.isWindows) {
                String evilCachedSplashFile = System.getProperty("netbeans.user")+File.separator+"var"+File.separator+"cache"+File.separator+"splash.png";
                File tmpEvilCachedSplashFile = new File(evilCachedSplashFile);
                if (tmpEvilCachedSplashFile.exists()) {
                    log.info("Cached splash file "+evilCachedSplashFile+" exists.  Removing...");
                    boolean deleteSuccess = tmpEvilCachedSplashFile.delete();
                    if (deleteSuccess) {
                        log.info("Successfully removed the splash.png file");
                    }
                    else {
                        log.info("Could not successfully removed the splash.png file");
                    }
                }
                else {
                    log.info("Did not find the cached splash file ("+evilCachedSplashFile+").  Continuing...");
                }
            }
        }
        catch (Exception e) {
            log.error("Error trying to exorcise the splash file on Windows.  Ignoring...");
        }
    }

    private static JFrame mainFrame;
    public static JFrame getMainFrame() {
        if (mainFrame == null) {
            try {
                mainFrame = WindowLocator.getMainFrame();
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
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
        exceptionHandler.handleException(throwable);
    }

    public String getApplicationOutputDirectory() {
        return prefs.getApplicationOutputDirectory();
    }
    
    public Object setModelProperty(Object key, Object value) {
        return prefs.setModelProperty(key, value);
    }

    public Object getModelProperty(Object key) {
        return prefs.getModelProperty(key);
    }

    @Subscribe
    public void systemWillExit(ApplicationClosing closingEvent) {
        log.info("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");
        findAndRemoveWindowsSplashFile();
        prefs.writeSettings();
    }

    public static PatternSearchDialog getPatternSearchDialog() {
        return patternSearchDialog;
    }

    public static GiantFiberSearchDialog getGiantFiberSearchDialog() {
        return fiberSearchDialog;
    }
    
    public static MaskSearchDialog getMaskSearchDialog() {
        return maskSearchDialog;
    }
}
