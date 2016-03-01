package org.janelia.it.workstation.gui.framework.session_mgr;

import java.awt.IllegalComponentStateException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.SystemError;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.external_listener.ExternalListener;
import org.janelia.it.workstation.gui.framework.keybind.KeyBindings;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;
import org.janelia.it.workstation.shared.util.RendererType2D;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.filecache.LocalFileCache;
import org.janelia.it.workstation.shared.util.filecache.WebDavClient;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.ws.ExternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel;

public final class SessionMgr implements ActivityLogging {

    private static final Logger log = LoggerFactory.getLogger(SessionMgr.class);

    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;

    private static final int MAX_PORT_TRIES = 20;
    private static final int PORT_INCREMENT = 1000;
    private static final int LOG_GRANULARITY = 100;
    
    public static String USER_EMAIL = "UserEmail";

    public static String DISPLAY_FREE_MEMORY_METER_PROPERTY = "SessionMgr.DisplayFreeMemoryProperty";
    public static String UNLOAD_IMAGES_PROPERTY = "SessionMgr.UnloadImagesProperty";
    public static String DISPLAY_SUB_EDITOR_PROPERTY = "SessionMgr.DisplaySubEditorProperty";
    public static String JACS_DATA_PATH_PROPERTY = "SessionMgr.JacsDataPathProperty";
    public static String JACS_INTERACTIVE_SERVER_PROPERTY = "SessionMgr.JacsInteractiveServerProperty";
    public static String JACS_PIPELINE_SERVER_PROPERTY = "SessionMgr.JacsPipelineServerProperty";
    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;
    public static String REMEMBER_PASSWORD = LoginProperties.REMEMBER_PASSWORD;
    public static String FILE_CACHE_DISABLED_PROPERTY = "console.localCache.disabled";
    public static String FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY = "console.localCache.gigabyteCapacity";
    public static String DOWNLOADS_DIR = "DownloadsDir";
    public static String DISPLAY_LOOK_AND_FEEL = "SessionMgr.JavaLookAndFeel";
    public static String DISPLAY_RENDERER_2D = "SessionMgr.Renderer2D";

    public static boolean isDarkLook = false;

    private static JFrame mainFrame;
    private static final ModelMgr modelManager = ModelMgr.getModelMgr();
    private static final SessionMgr sessionManager = new SessionMgr();
    private SessionModel sessionModel = SessionModel.getSessionModel();
    
    private ImageIcon browserImageIcon;
    private ExternalListener externalHttpListener;
//    private EmbeddedAxisServer axisServer;
//    private EmbeddedWebServer webServer;
    private File settingsFile;
    private String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private String prefsFile = prefsDir + ".JW_Settings";
    private Browser activeBrowser;
    private String appName, appVersion;
    private boolean isLoggedIn;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;
    private Long currentSessionId;
    private WebDavClient webDavClient;
    private LocalFileCache localFileCache;    
    private Map<CategoryString, Long> categoryInstanceCount = new HashMap<>();

    private SessionMgr() {
        log.info("Initializing Session Manager");
        findAndRemoveWindowsSplashFile();
        System.setProperty("winsys.stretching_view_tabs", "true");

        settingsFile = new File(prefsFile);
        try {
            boolean success = settingsFile.createNewFile();  //only creates if does not exist
            if (success) {
                log.info("Created a new settings file in "+settingsFile.getAbsolutePath());
            }
        }
        catch (IOException ioEx) {
            if (!new File(prefsDir).mkdirs()) {
                log.error("Could not create prefs dir at " + prefsDir);
            }
            try {
                boolean success = settingsFile.createNewFile();  //only creates if does not exist
                if (success) {
                    log.info("Created a new settings file in "+settingsFile.getAbsolutePath());
                }
            }
            catch (IOException e) {
                log.error("Cannot create settings file at: " + settingsFile, e);
            }
        }

        readSettingsFile();
        EJBFactory.initFromModelProperties(sessionModel);
        PathTranslator.initFromModelProperties(sessionModel);

        // -----------------------------------------------
        // initialize WebDAV and local cache components
        webDavClient = new WebDavClient(
                ConsoleProperties.getString("console.webDavClient.baseUrl",
                        WebDavClient.JACS_WEBDAV_BASE_URL),
                ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100),
                ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100));

        setFileCacheGigabyteCapacity((Integer) getModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY));
        setFileCacheDisabled(Boolean.parseBoolean(String.valueOf(
                getModelProperty(SessionMgr.FILE_CACHE_DISABLED_PROPERTY))));

        // -----------------------------------------------
        if (getModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY) == null) {
            setModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY, true);
        }

        if (getModelProperty(UNLOAD_IMAGES_PROPERTY) == null) {
            setModelProperty(UNLOAD_IMAGES_PROPERTY, false);
        }

        if (getModelProperty(DISPLAY_SUB_EDITOR_PROPERTY) == null) {
            setModelProperty(DISPLAY_SUB_EDITOR_PROPERTY, true);
        }

        if (getModelProperty(SessionMgr.DISPLAY_RENDERER_2D) == null) {
            setModelProperty(SessionMgr.DISPLAY_RENDERER_2D, RendererType2D.IMAGE_IO.toString());
        }

        log.info("Using 2d renderer: {}", getModelProperty(SessionMgr.DISPLAY_RENDERER_2D));

        // Look for user's model-property-designated look-and-feel.
        //  If it is found, and it is installed (not defunct/obsolete) use it.
        // If not, force user's setting to one that is installed (current one).
        //
        // Synthetica Licenses
        String[] li = {"Licensee=HHMI", "LicenseRegistrationNumber=122030", "Product=Synthetica", "LicenseType=Single Application License", "ExpireDate=--.--.----", "MaxVersion=2.20.999"};
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", "9A519ECE-5BB55629-B2E1233E-9E3E72DB-19992C5D");

        String[] li2 = {"Licensee=HHMI", "LicenseRegistrationNumber=142016", "Product=SyntheticaAddons", "LicenseType=Single Application License", "ExpireDate=--.--.----", "MaxVersion=1.10.999"};
        UIManager.put("SyntheticaAddons.license.info", li2);
        UIManager.put("SyntheticaAddons.license.key", "43BF31CE-59317732-9D0D5584-654D216F-7806C681");

        // Ensure the synthetical choices are all available.
        UIManager.installLookAndFeel("Synthetica AluOxide Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaAluOxideLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackEye Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackStar Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueIce Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueLight Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueLightLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueSteel Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel");
        UIManager.installLookAndFeel("Synthetica Classy Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaClassyLookAndFeel");
        UIManager.installLookAndFeel("Synthetica GreenDream Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel");
        UIManager.installLookAndFeel("Synthetica MauveMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica OrangeMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica SilverMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica Simple2D Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel");
        UIManager.installLookAndFeel("Synthetica SkyMetallic Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel");
        UIManager.installLookAndFeel("Synthetica WhiteVision Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel");
        LookAndFeelInfo[] installedInfos = UIManager.getInstalledLookAndFeels();

        String lafName = (String) getModelProperty(DISPLAY_LOOK_AND_FEEL);
        LookAndFeel currentLaf = UIManager.getLookAndFeel();
        LookAndFeelInfo currentLafInfo = null;
        if (lafName==null) lafName = "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel";
        try {
            boolean installed = false;
            for (LookAndFeelInfo lafInfo : installedInfos) {
                if (lafInfo.getClassName().equals(lafName)) {
                    installed = true;
                }
                if (lafInfo.getName().equals(currentLaf.getName())) {
                    currentLafInfo = lafInfo;
                }
            }
            if (installed) {
                setLookAndFeel(lafName);
            }
            else if (currentLafInfo != null) {
                setLookAndFeel(currentLafInfo.getName());
                setModelProperty(DISPLAY_LOOK_AND_FEEL, currentLafInfo.getClassName());
            }
            else {
                log.error("Could not set Look and Feel: {}",lafName);
            }
        }
        catch (Exception ex) {
            handleException(ex);
        }

        String tempLogin = (String) getModelProperty(USER_NAME);
        String tempPassword = (String) getModelProperty(USER_PASSWORD);
        if (tempLogin != null && tempPassword != null) {
            PropertyConfigurator.getProperties().setProperty(USER_NAME, tempLogin);
            PropertyConfigurator.getProperties().setProperty(USER_PASSWORD, tempPassword);
        }
        Integer tmpCache = (Integer) getModelProperty(FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
        if (null != tmpCache) {
            PropertyConfigurator.getProperties().setProperty(FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, tmpCache.toString());
        }

    } //Singleton enforcement

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

    private void readSettingsFile() {
        try {
            if (!settingsFile.canRead()) {
                JOptionPane.showMessageDialog(getMainFrame(), "Settings file cannot be opened.  " + "Settings were not read and recovered.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                boolean success = settingsFile.renameTo(new File(prefsFile + ".old"));
                if (success) {
                    log.info("Moved the unreadable settings file to "+settingsFile.getAbsolutePath());
                }
            }
            ObjectInputStream istream = new ObjectInputStream(new FileInputStream(settingsFile));
            switch (istream.readInt()) {
                case 1: {
                    try {
                        sessionModel.setModelProperties((TreeMap) istream.readObject());
                    }
                    catch (Exception ex) {
                        log.info("Error reading settings ",ex);
                        JOptionPane.showMessageDialog(getMainFrame(), "Settings were not recovered into the session.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                        File oldFile = new File(prefsFile + ".old");
                        boolean deleteSuccess = oldFile.delete();
                        if (!deleteSuccess) {
                            log.error("Could not delete the old settings: "+oldFile.getAbsolutePath());
                        }
                        boolean renameSuccess = settingsFile.renameTo(new File(prefsFile + ".old"));
                        if (!renameSuccess) {
                            log.error("Could not rename new settings file to old.");
                        }
                    }
                    finally {
                        istream.close();
                    }
                    break;
                }
                default: {
                }
            }
        }
        catch (EOFException eof) {
            log.info("No settings file",eof);
            // Do nothing, there are no preferences
        }
        catch (Exception ioEx) {
            log.info("Error reading settings file", ioEx);
        } //new settingsFile
    }

    static public SessionMgr getSessionMgr() {
        return sessionManager;
    }

    public SessionModel getSessionModel() {
        return sessionModel;
    }

    public Object setModelProperty(Object key, Object value) {
        return sessionModel.setModelProperty(key, value);
    }

    public int addExternalClient(String newClientName) {
        return sessionModel.addExternalClient(newClientName);
    }

    public List<ExternalClient> getExternalClientsByName(String clientName) {
        return sessionModel.getExternalClientsByName(clientName);
    }

    public ExternalClient getExternalClientByPort(int targetPort) {
        return sessionModel.getExternalClientByPort(targetPort);
    }

    public void removeExternalClientByPort(int targetPort) {
        sessionModel.removeExternalClientByPort(targetPort);
    }

    public void sendMessageToExternalClients(String operationName, Map<String, Object> parameters) {
        sessionModel.sendMessageToExternalClients(operationName, parameters);
    }

    public static KeyBindings getKeyBindings() {
        return SessionModel.getKeyBindings();
    }

    public Object getModelProperty(Object key) {
        return sessionModel.getModelProperty(key);
    }

    public void registerPreferenceInterface(Object interfaceKey, Class interfaceClass) throws Exception {
        PrefController.getPrefController().registerPreferenceInterface(interfaceKey, interfaceClass);
    }

    public void registerExceptionHandler(ExceptionHandler handler) {
        modelManager.registerExceptionHandler(handler);
    }

    public void setApplicationName(String name) {
        appName = name;
    }

    public String getApplicationName() {
        return appName;
    }

    public void setApplicationVersion(String version) {
        appVersion = version;
    }

    public String getApplicationVersion() {
        return appVersion;
    }

    public void setNewBrowserImageIcon(ImageIcon newImageIcon) {
        browserImageIcon = newImageIcon;
    }

    /**
     * @return the session client for issuing WebDAV requests.
     */
    public WebDavClient getWebDavClient() {
        return webDavClient;
    }

    /**
     * @return true if a local file cache is available for this session; otherwise false.
     */
    public boolean isFileCacheAvailable() {
        return (localFileCache != null);
    }

    /**
     * Enables or disables the local file cache and
     * saves the setting as a session preference.
     *
     * @param isDisabled if true, cache will be disabled;
     * otherwise cache will be enabled.
     */
    public void setFileCacheDisabled(boolean isDisabled) {

        setModelProperty(SessionMgr.FILE_CACHE_DISABLED_PROPERTY, isDisabled);

        if (isDisabled) {
            log.warn("disabling local cache");
            localFileCache = null;
        }
        else {
            try {
                final String localCacheRoot
                        = ConsoleProperties.getString("console.localCache.rootDirectory",
                                prefsDir);
                final long kilobyteCapacity = getFileCacheGigabyteCapacity() * 1024 * 1024;

                localFileCache = new LocalFileCache(new File(localCacheRoot),
                        kilobyteCapacity,
                        webDavClient,
                        null);
            }
            catch (Exception e) {
                localFileCache = null;
                log.error("disabling local cache after initialization failure", e);
            }
        }
    }

    /**
     * @return the session local file cache instance or null if a cache is not available.
     */
    public LocalFileCache getFileCache() {
        return localFileCache;
    }

    /**
     * @return the maximum number of gigabytes to store in the local file cache.
     */
    public int getFileCacheGigabyteCapacity() {
        return (Integer) getModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
    }

    /**
     * Sets the local file cache capacity and saves the setting as a session preference.
     *
     * @param gigabyteCapacity cache capacity in gigabytes.
     */
    public void setFileCacheGigabyteCapacity(Integer gigabyteCapacity) {

        if ((gigabyteCapacity == null)
                || (gigabyteCapacity < MIN_FILE_CACHE_GIGABYTE_CAPACITY)) {
            gigabyteCapacity = MIN_FILE_CACHE_GIGABYTE_CAPACITY;
        }
        else if (gigabyteCapacity > MAX_FILE_CACHE_GIGABYTE_CAPACITY) {
            gigabyteCapacity = MAX_FILE_CACHE_GIGABYTE_CAPACITY;
        }

        setModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY,
                gigabyteCapacity);

        if (isFileCacheAvailable()) {
            final long kilobyteCapacity = gigabyteCapacity * 1024 * 1024;
            if (kilobyteCapacity != localFileCache.getKilobyteCapacity()) {
                localFileCache.setKilobyteCapacity(kilobyteCapacity);
            }
        }
    }

    /**
     * @return the total size (in gigabytes) of all currently cached files.
     */
    public double getFileCacheGigabyteUsage() {
        double usage = 0.0;
        if (isFileCacheAvailable()) {
            final long kilobyteUsage = localFileCache.getNumberOfKilobytes();
            usage = kilobyteUsage / (1024.0 * 1024.0);
        }
        return usage;
    }

    /**
     * Removes all locally cached files.
     */
    public void clearFileCache() {
        if (isFileCacheAvailable()) {
            localFileCache.clear();
        }
    }

    /**
     * Send an event described by the information given as parameters, to the
     * logging apparatus. Apply the criteria of:
     * 1. allow-to-log if more time was taken, than the lower threshold, or
     * 2. allow-to-log if the count of attempts for category==granularity.
     * 
     * @param toolName the stakeholder tool, in this event.
     * @param category for namespacing.
     * @param action what happened.
     * @param timestamp when it happened.
     * @param elapsedMs how much time passed to carry this out?
     * @param thresholdMs beyond this time, force log issue.
     */
    @Override
    public void logToolEvent(final ToolString toolName, final CategoryString category, final ActionString action, final long timestamp, final double elapsedMs, final double thresholdMs) {
        String userLogin = null;

        try {
            userLogin = PropertyConfigurator.getProperties().getProperty(USER_NAME);
            final UserToolEvent event = new UserToolEvent(getCurrentSessionId(), userLogin.toString(), toolName.toString(), category.toString(), action.toString(), new Date(timestamp));
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
                    Long count = categoryInstanceCount.get(category);
                    if (count == null) {
                        count = new Long(0);
                    }
                    boolean shouldLog = false;
                    if (elapsedMs > thresholdMs) {
                        shouldLog = true;
                    } else if (count % LOG_GRANULARITY == 0) {
                        shouldLog = true;
                    }
                    categoryInstanceCount.put(category, ++count);

                    if (shouldLog) {
                        ModelMgr.getModelMgr().addEventToSession(event);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);

        } catch (Exception ex) {
            log.warn(
                    "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                    getCurrentSessionId(), userLogin, toolName, category, action, timestamp
            );
            ex.printStackTrace();
        }
    }

    /**
     * Send an event described by the information given as parameters, to the
     * logging apparatus. Apply the criteria of:
     * 1. allow-to-log if more time was taken, than the lower threshold, or
     * 
     * @param toolName the stakeholder tool, in this event.
     * @param category for namespacing.
     * @param action what happened.
     * @param timestamp when it happened.
     * @param elapsedMs how much time passed to carry this out?
     * @param thresholdMs beyond this time, force log issue.
     * @todo see about reusing code between this and non-threshold.
     */
    @Override
    public void logToolThresholdEvent(final ToolString toolName, final CategoryString category, final ActionString action, final long timestamp, final double elapsedMs, final double thresholdMs) {
        String userLogin = null;

        try {
            userLogin = PropertyConfigurator.getProperties().getProperty(USER_NAME);
            final UserToolEvent event = new UserToolEvent(getCurrentSessionId(), userLogin.toString(), toolName.toString(), category.toString(), action.toString(), new Date(timestamp));
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
                    boolean shouldLog = false;
                    if (elapsedMs > thresholdMs) {
                        shouldLog = true;
                    }

                    if (shouldLog) {
                        ModelMgr.getModelMgr().addEventToSession(event);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);

        } catch (Exception ex) {
            log.warn(
                    "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                    getCurrentSessionId(), userLogin, toolName, category, action, timestamp
            );
            ex.printStackTrace();
        }
    }

    /**
     * Log a tool event, always.  No criteria will be checked.
     * 
     * @see #logToolEvent(org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString, org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString, org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString, long) 
     */
    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action) {
        // Force logging, by setting elapsed > threshold.
        logToolEvent(toolName, category, action, new Date().getTime(), 1.0, 0.0);
    }

    /**
     * Log-tool-event override, which includes elapsed/threshold comparison
     * values.  If the elapsed time (expected milliseconds) exceeds the
     * threshold, definitely log.  Also, will check number-of-issues against
     * a granularity map.  Only issue the message at a preset
     * granularity.
     * 
     * @see #logToolEvent(org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString, org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString, org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString, long, double, double) 
     * @param elapsedMs
     * @param thresholdMs 
     */
    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        logToolEvent(toolName, category, action, new Date().getTime(), elapsedMs, thresholdMs);
    }
    
    public void handleException(Throwable throwable) {
        modelManager.handleException(throwable);
    }

    public Browser newBrowser() {
        Browser browser = new Browser(sessionModel.addBrowserModel());
        if (browserImageIcon != null) {
            browser.setBrowserImageIcon(browserImageIcon);
        }
        activeBrowser = browser;
        return browser;
    }

    public void systemExit() {
        systemExit(0);
    }

    public void systemExit(int errorlevel) {
        log.info("Exiting with code "+errorlevel);
        sessionModel.systemWillExit();
        writeSettings(); // Saves user preferences.
        sessionModel.removeAllBrowserModels();

        //logoutUser();
        log.info("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");

        modelManager.prepareForSystemExit();
        findAndRemoveWindowsSplashFile();
    }

    public void addSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.addSessionListener(sessionModelListener);
    }

    public void removeSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.removeSessionListener(sessionModelListener);
    }

    public void setLookAndFeel(String lookAndFeelClassName) {
        try {
            if (lookAndFeelClassName.contains("BlackEye")) {
                isDarkLook = true;
                try {
                    UIManager.setLookAndFeel(new SyntheticaBlackEyeLookAndFeel() {
                        @Override
                        protected void loadCustomXML() throws ParseException {
                            loadXMLConfig("/SyntheticaBlackEyeLookAndFeel.xml");
                        }
                    });
                }
                catch (IllegalComponentStateException ex) {
                    handleException(ex);
                }
            }
            else {
                UIManager.setLookAndFeel(lookAndFeelClassName);
            }

            // The main frame is not presented until after this time.
            //  No need to update its LaF.
            setModelProperty(DISPLAY_LOOK_AND_FEEL, lookAndFeelClassName);

            log.info("Configured Look and Feel: {}", lookAndFeelClassName);
        }
        catch (Exception ex) {
            handleException(ex);
        }
    }

    public boolean isUnloadImages() {
        Boolean unloadImagesBool = (Boolean) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.UNLOAD_IMAGES_PROPERTY);
        return unloadImagesBool != null && unloadImagesBool;
    }

    public boolean isDarkLook() {
        return isDarkLook;
    }

    /**
     * Use getBrowser, it's shorter and static.
     */
    public Browser getActiveBrowser() {
        return activeBrowser;
    }

    public static Browser getBrowser() {
        return getSessionMgr().getActiveBrowser();
    }

    /**
     * Call this if all you need is a parent frame. Browser will no longer
     * extend JFrame.
     *
     * @return the main framework window.
     */
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

    public void startExternalHttpListener(int port) {
        if (externalHttpListener == null) {
            externalHttpListener = new ExternalListener(port);
        }
    }
    
    private int axisServerPort;

    public int getAxisServerPort() {
        return axisServerPort;
    }

    public void setAxisServerPort(int axisServerPort) {
        this.axisServerPort = axisServerPort;
    }

    private int webServerPort;

    public int getWebServerPort() {
        return webServerPort;
    }

    public void setWebServerPort(int webServerPort) {
        this.webServerPort = webServerPort;
    }

//    public int startAxisServer(int startingPort) {
//        int port = startingPort;
//        try {
//            if (axisServer == null) {
//                axisServer = new EmbeddedAxisServer();
//            }
//            int tries = 0;
//            while (true) {
//                try {
//                    axisServer.start(port);
//                    log.info("Started web services on port " + port);
//                    sessionModel.setPortOffset(port);
//                    break;
//                } 
//                catch (Exception e) {
//                    if (e instanceof BindException || e.getCause() instanceof BindException) {
//                        log.info("Could not start web service on port: " + port);
//                        port += PORT_INCREMENT;
//                        tries++;
//                        if (tries >= MAX_PORT_TRIES) {
//                            log.error("Tried to start web service on " + MAX_PORT_TRIES + " ports, giving up.");
//                            return -1;
//                        }
//                    } 
//                    else {
//                        log.error("Could not start web service on port: " + port);
//                        throw e;
//                    }
//                }
//            }
//            return port;
//        } 
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//            return -1;
//        }
//    }

//    public EmbeddedAxisServer getAxisServer() {
//        return axisServer;
//    }

//    public int startWebServer(int startingPort) {
//        int port = startingPort;
//        try {
//            if (webServer == null) {
//                webServer = new EmbeddedWebServer();
//            }
//            int tries = 0;
//            while (true) {
//                try {
//                    webServer.start(port);
//                    log.info("Started web server on port " + port);
//                    break;
//                } 
//                catch (Exception e) {
//                    if (e instanceof BindException || e.getCause() instanceof BindException) {
//                        log.info("Could not start web server on port: " + port);
//                        port += PORT_INCREMENT;
//                        tries++;
//                        if (tries >= MAX_PORT_TRIES) {
//                            log.error("Tried to start web server on " + MAX_PORT_TRIES + " ports, giving up.");
//                            return -1;
//                        }
//                    } 
//                    else {
//                        log.error("Could not start web server on port: " + port);
//                        throw e;
//                    }
//                }
//            }
//            return port;
//        } 
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//            return -1;
//        }
//    }
    
    public void saveUserSettings() {
        writeSettings();
    }

    private void writeSettings() {
        try {
            boolean success = settingsFile.delete();
            if (!success) {
                log.error("Unable to delete old settings file.");
            }
            ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(settingsFile));
            ostream.writeInt(1);  //stream format
            ostream.writeObject(sessionModel.getModelProperties());
            ostream.flush();
            ostream.close();
            log.info("Saving user settings to " + settingsFile.getAbsolutePath());
        }
        catch (IOException ioEx) {
            handleException(ioEx);
        }
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }

    class MyBrowserListener extends WindowAdapter {

        public void windowClosed(WindowEvent e) {
            log.info("Window is closing...");
            e.getWindow().removeWindowListener(this);
            SessionMgr.getSessionMgr().saveUserSettings();
        }

        public void windowActivated(WindowEvent e) {
            // NO-FRAME activeBrowser = (Browser) e.getWindow();
        }
    }

    /**
     * If local caching is enabled, this method will synchronously cache
     * the requested system file (as needed) and return the cached file.
     * If local caching is disabled, null is returned.
     *
     * @param standardPath the standard system path for the file.
     *
     * @param forceRefresh indicates if any existing cached file
     * should be forcibly refreshed before
     * being returned. In most cases, this
     * should be set to false.
     *
     * @return an accessible file for the specified path or
     * null if caching is disabled or the file cannot be cached.
     */
    public static File getCachedFile(String standardPath,
            boolean forceRefresh) {

        final SessionMgr mgr = SessionMgr.getSessionMgr();

        File file = null;
        if (mgr.isFileCacheAvailable()) {
            final LocalFileCache cache = mgr.getFileCache();
            final WebDavClient client = mgr.getWebDavClient();
            try {
                final URL url = client.getWebDavUrl(standardPath);
                file = cache.getFile(url, forceRefresh);
            }
            catch (Exception e) {
                log.error("Failed to retrieve " + standardPath + " from local cache", e);
            }
        }
        else {
            log.warn("Local file cache is not available");
        }

        return file;
    }

    /**
     * Get the URL for a standard path. It may be a local URL, if the file has been cached, or a remote
     * URL on the WebDAV server. It might even be a mounted location, if WebDAV is disabled.
     *
     * @param standardPath a standard system path
     * @return an accessible URL for the specified path
     */
    public static URL getURL(String standardPath) {
        try {
            SessionMgr sessionMgr = getSessionMgr();
            WebDavClient client = sessionMgr.getWebDavClient();
            URL remoteFileUrl = client.getWebDavUrl(standardPath);
            LocalFileCache cache = sessionMgr.getFileCache();
            return sessionMgr.isFileCacheAvailable() ? cache.getEffectiveUrl(remoteFileUrl) : remoteFileUrl;
        }
        catch (MalformedURLException e) {
            SessionMgr.getSessionMgr().handleException(e);
            return null;
        }
    }

    /**
     * This is a hack to inject the run-as subject key from an already-authenticated user in the NG world. 
     * It ties the legacy SessionMgr to the new AccessManager.
     */
    public static void setSubjectKey(final String subjectKey) {
        try {
            Subject subject = ModelMgr.getModelMgr().getSubjectWithPreferences(subjectKey);
            getSessionMgr().setSubject(subject);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private void setSubject(Subject subject) {
        loggedInSubject = authenticatedSubject = subject;
        if (loggedInSubject!=null) {
            isLoggedIn = true;
        }
        resetSession();
        log.info("Completed legacy track init with user "+subject.getKey());
    }
    
    public static String getSubjectKey() {
        return getSessionMgr().getSubject().getKey();
    }

    public static List<String> getSubjectKeys() {
        List<String> subjectKeys = new ArrayList<>();
        Subject subject = SessionMgr.getSessionMgr().getSubject();
        if (subject != null) {
            subjectKeys.add(subject.getKey());
            if (subject instanceof User) {
                for (SubjectRelationship relation : ((User) subject).getGroupRelationships()) {
                    subjectKeys.add(relation.getGroup().getKey());
                }
            }
        }
        return subjectKeys;
    }

    public Subject getSubject() {
        return loggedInSubject;
    }

    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
    }


    public static String getUsername() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getEmail();
    }

//    public static boolean authenticatedSubjectIsInGroup(String groupName) {
//        Subject subject = SessionMgr.getSessionMgr().getAuthenticatedSubject();
//        return subject instanceof User && isUserInGroup((User) subject, groupName);
//    }
//
//    public static boolean currentUserIsInGroup(String groupName) {
//        Subject subject = SessionMgr.getSessionMgr().getSubject();
//        return subject instanceof User && isUserInGroup((User) subject, groupName);
//    }
//
//    private static boolean isUserInGroup(User targetUser, String targetGroup) {
//        if (null == targetUser) {
//            return false;
//        }
//        for (SubjectRelationship relation : targetUser.getGroupRelationships()) {
//            if (relation.getGroup().getName().equals(targetGroup)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public boolean loginSubject(String username, String password) {
//        try {
//            boolean relogin = false;
//
//            if (isLoggedIn()) {
//                logoutUser();
//                log.info("RELOGIN");
//                relogin = true;
//            }
//
//            findAndRemoveWindowsSplashFile();
//            // Login and start the session
//            authenticatedSubject = FacadeManager.getFacadeManager().getComputeFacade().loginSubject(username, password);
//            if (null != authenticatedSubject) {
//                isLoggedIn = true;
//                loggedInSubject = authenticatedSubject;
//                log.info("Authenticated as {}", authenticatedSubject.getKey());
//
//                FacadeManager.getFacadeManager().getComputeFacade().beginSession();
//                if (relogin) {
//                    resetSession();
//                }
//            }
//
//            return isLoggedIn;
//        }
//        catch (Exception e) {
//            isLoggedIn = false;
//            log.error("Error logging in", e);
//            throw new FatalCommError(ConsoleProperties.getInstance().getProperty("interactive.server.url"),
//                    "Cannot authenticate login. The server may be down. Please try again later.");
//        }
//    }

//    public boolean setRunAsUser(String runAsUser) {
//
//        if (!SessionMgr.authenticatedSubjectIsInGroup(Group.ADMIN_GROUP_NAME) && !StringUtils.isEmpty(runAsUser)) {
//            throw new IllegalStateException("Non-admin user cannot run as another user");
//        }
//
//        try {
//            if (!StringUtils.isEmpty(runAsUser)) {
//                Subject runAsSubject = ModelMgr.getModelMgr().getSubjectWithPreferences(runAsUser);
//                if (runAsSubject==null) {
//                    return false;
//                }
//                loggedInSubject = runAsSubject;
//            }
//            else {
//                loggedInSubject = authenticatedSubject;
//            }
//
//            if (!authenticatedSubject.getId().equals(loggedInSubject.getId())) {
//                log.info("Authenticated as {} (Running as {})", authenticatedSubject.getKey(), loggedInSubject.getId());
//            }
//
//            resetSession();
//            return true;
//        }
//        catch (Exception e) {
//            loggedInSubject = authenticatedSubject;
//            handleException(e);
//            return false;
//        }
//    }

    private void resetSession() {
        final Browser browser = SessionMgr.getBrowser();
        if (browser != null) {
            log.debug("Refreshing all views");
            browser.resetView();
        }
        log.debug("Clearing entity model");
        ModelMgr.getModelMgr().reset();
        FacadeManager.addProtocolToUseList(FacadeManager.getEJBProtocolString());
    }

//    public void logoutUser() {
//        try {
//            if (loggedInSubject != null) {
//                log.info("Logged out with: {}", loggedInSubject.getKey());
//            }
//            isLoggedIn = false;
//            loggedInSubject = null;
//            authenticatedSubject = null;
//        }
//        catch (Exception e) {
//            log.error("Error logging out", e);
//        }
//    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public Long getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(Long currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
}
