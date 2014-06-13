package org.janelia.it.workstation.gui.framework.session_mgr;

import de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.SystemError;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.external_listener.ExternalListener;
import org.janelia.it.workstation.gui.framework.keybind.KeyBindings;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;
import org.janelia.it.workstation.shared.util.RendererType2D;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.util.filecache.LocalFileCache;
import org.janelia.it.workstation.shared.util.filecache.WebDavClient;
import org.janelia.it.workstation.web.EmbeddedWebServer;
import org.janelia.it.workstation.ws.EmbeddedAxisServer;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.ws.ExternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import javax.swing.UIManager.LookAndFeelInfo;

public class SessionMgr {

    private static final Logger log = LoggerFactory.getLogger(SessionMgr.class);

    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;

    public static String DISPLAY_FREE_MEMORY_METER_PROPERTY = "SessionMgr.DisplayFreeMemoryProperty";
    public static String UNLOAD_IMAGES_PROPERTY = "SessionMgr.UnloadImagesProperty";
    public static String DISPLAY_SUB_EDITOR_PROPERTY = "SessionMgr.DisplaySubEditorProperty";
    public static String JACS_DATA_PATH_PROPERTY = "SessionMgr.JacsDataPathProperty";
    public static String JACS_INTERACTIVE_SERVER_PROPERTY = "SessionMgr.JacsInteractiveServerProperty";
    public static String JACS_PIPELINE_SERVER_PROPERTY = "SessionMgr.JacsPipelineServerProperty";
    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;
    public static String USER_EMAIL = "UserEmail";
    public static String FILE_CACHE_DISABLED_PROPERTY = "console.localCache.disabled";
    public static String FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY = "console.localCache.gigabyteCapacity";
    public static String RUN_AS_USER = "RunAs";
    public static String DOWNLOADS_DIR = "DownloadsDir";
    public static String DISPLAY_LOOK_AND_FEEL = "SessionMgr.JavaLookAndFeel";
    public static String DISPLAY_RENDERER_2D = "SessionMgr.Renderer2D";

    public static boolean isDarkLook = false;
    // TODO: This is a quick hack to get the data viewer to work in the new NetBeans eco-system. This needs to be replaced with group:admin controls. 
    public static boolean rootAccess = false;

    private static JFrame mainFrame;
    private static ModelMgr modelManager = ModelMgr.getModelMgr();
    private static SessionMgr sessionManager = new SessionMgr();
    private SessionModel sessionModel = SessionModel.getSessionModel();
    private float browserSize = .8f;
    private String browserTitle;
    private ImageIcon browserImageIcon;
    private Component splashPanel;
    private ExternalListener externalHttpListener;
    private EmbeddedAxisServer axisServer;
    private EmbeddedWebServer webServer;
    private File settingsFile;
    private String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private String prefsFile = prefsDir + ".JW_Settings";
    private Map<BrowserModel, Browser> browserModelsToBrowser = new HashMap<BrowserModel, Browser>();
    private WindowListener myBrowserWindowListener = new MyBrowserListener();
    private Browser activeBrowser;
    private String appName, appVersion;
    private boolean isLoggedIn;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;
    private Long currentSessionId;
    private WebDavClient webDavClient;
    private LocalFileCache localFileCache;

    private SessionMgr() {
        log.info("Initializing Session Manager");

        System.setProperty("winsys.stretching_view_tabs", "true");

        settingsFile = new File(prefsFile);
        try {
            // @todo Remove this Dec 2013  :-)
            //if you get this far, check to migrate over the old preferences
            File oldDir = new File(System.getProperty("user.home") + "/.FlyWorkstationSuite/Console/");
            if (oldDir.exists()) {
                new File(System.getProperty("user.home") + "/.FlyWorkstationSuite/").renameTo(new File(System.getProperty("user.home") + "/.JaneliaWorkstationSuite/"));
                new File(prefsDir + "/.FW_Settings").renameTo(settingsFile);
                log.info("Renamed settings directory and files.");
            }
            else {
                settingsFile.createNewFile();  //only creates if does not exist
            }
        }
        catch (IOException ioEx) {
            if (!new File(prefsDir).mkdirs()) {
                log.error("Could not create prefs dir at " + prefsDir);
            }
            try {
                settingsFile.createNewFile();  //only creates if does not exist
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
        if (lafName != null) {
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
            }
            catch (Exception ex) {
                handleException(ex);
            }
        }
        else {
            setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel");
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

    private void readSettingsFile() {
        JFrame mainFrame = new JFrame();
        JOptionPane optionPane = new JOptionPane();
        try {
            mainFrame.setIconImage(Utils.getClasspathImage("flyscope.jpg").getImage());
            mainFrame.getContentPane().add(optionPane);
            if (!settingsFile.canRead()) {
                optionPane.showMessageDialog(mainFrame, "Settings file cannot be opened.  " + "Settings were not read and recovered.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                settingsFile.renameTo(new File(prefsFile + ".old"));

            }
            ObjectInputStream istream = new ObjectInputStream(new FileInputStream(settingsFile));
            switch (istream.readInt()) {
                case 1: {
                    try {

                        sessionModel.setModelProperties((TreeMap) istream.readObject());
                        istream.close();
                    }
                    catch (Exception ex) {
                        istream.close();
                        optionPane.showMessageDialog(mainFrame, "Settings were not recovered into the session.", "ERROR!", JOptionPane.ERROR_MESSAGE);
                        File oldFile = new File(prefsFile + ".old");
                        oldFile.delete();
                        settingsFile.renameTo(new File(prefsFile + ".old"));
                    }
                    break;
                }
                default: {
                }
            }
        }
        catch (EOFException eof) {
            // Do nothing, there are no preferences
        }
        catch (Exception ioEx) {
            ioEx.printStackTrace();
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

    public List<ExternalClient> getExternalClients() {
        return sessionModel.getExternalClients();
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

    public void removeModelProperty(Object key) {
        sessionModel.removeModelProperty(key);
    }

    public Iterator getModelPropertyKeys() {
        return sessionModel.getModelPropertyKeys();
    }

    public String getNewBrowserTitle() {
        return browserTitle;
    }

    public void registerPreferenceInterface(Object interfaceKey, Class interfaceClass) throws Exception {
        PrefController.getPrefController().registerPreferenceInterface(interfaceKey, interfaceClass);
    }

    public void removePreferenceInterface(Object interfaceKey) throws Exception {
        PrefController.getPrefController().deregisterPreferenceInterface(interfaceKey);
    }

    public void registerExceptionHandler(ExceptionHandler handler) {
        modelManager.registerExceptionHandler(handler);
    }

    public void setNewBrowserSize(float screenPercent) {
        browserSize = screenPercent;
    }

    public void setNewBrowserTitle(String title) {
        browserTitle = title;
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

    public void handleException(Throwable throwable) {
        modelManager.handleException(throwable);
    }

    public Browser newBrowser() {
        Browser browser = new Browser(browserSize, sessionModel.addBrowserModel());
        if (browserImageIcon != null) {
            browser.setBrowserImageIcon(browserImageIcon);
        }
        browserModelsToBrowser.put(browser.getBrowserModel(), browser);
        activeBrowser = browser;
        return browser;
    }

    public void removeBrowser(Browser browser) {
        browserModelsToBrowser.remove(browser.getBrowserModel());
        sessionModel.removeBrowserModel(browser.getBrowserModel());
    }

    public void useFreeMemoryWatcher(boolean use) {
        this.setModelProperty("FreeMemoryViewer", use);
//      freeMemoryWatcher=use;
    }
    /*
     public boolean isUsingMemoryWatcher() {
     return freeMemoryWatcher;
     }
     */

    public void systemExit() {
        systemExit(0);
    }

    public void systemExit(int errorlevel) {
        sessionModel.systemWillExit();
        writeSettings(); // Saves user preferences.
        sessionModel.removeAllBrowserModels();

        logoutUser();
        log.info("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");

        modelManager.prepareForSystemExit();
        // System-exit is now handled by NetBeans framework.
        //  System.exit(errorlevel);
    }

    public void addSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.addSessionListener(sessionModelListener);
    }

    public void removeSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.removeSessionListener(sessionModelListener);
    }

    public void setSplashPanel(Component panel) {
        splashPanel = panel;
    }

    public Component getSplashPanel() {
        return splashPanel;
    }

    public int getNumberOfOpenBrowsers() {
        return sessionModel.getNumberOfBrowserModels();
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
            else if (lookAndFeelClassName.toLowerCase().contains("jtattoo")) {
                // setup the look and feel properties
                Properties props = new Properties();

                //props.put("logoString", "my company");
                //props.put("licenseKey", "INSERT YOUR LICENSE KEY HERE");
//                String controlColor = "218 254 230";
//                String buttonColor = "218 230 254"; 
//                String foreGround = "180 240 197";
//                String backGround = "0 0 0";
//                props.put("selectionBackgroundColor", backGround);
//                props.put("menuSelectionBackgroundColor", backGround);
//
//                props.put("controlColor", controlColor);
//                props.put("controlColorLight", controlColor);
//                props.put("controlColorDark", backGround);
//
//                props.put("buttonColor", buttonColor);
//                props.put("buttonColorLight", "255 255 255");
//                props.put("buttonColorDark", "244 242 232");
//
//                props.put("rolloverColor", controlColor);
//                props.put("rolloverColorLight", controlColor);
//                props.put("rolloverColorDark", backGround);
//
//                props.put("windowTitleForegroundColor", foreGround);
//                props.put("windowTitleBackgroundColor", backGround);
//                props.put("windowTitleColorLight", controlColor);
//                props.put("windowTitleColorDark", backGround);
//                props.put("windowBorderColor", controlColor);
//                com.jtattoo.plaf.smart.SmartLookAndFeel.setCurrentTheme(props);
                UIManager.setLookAndFeel(lookAndFeelClassName);
            }
            else {
                UIManager.setLookAndFeel(lookAndFeelClassName);
            }

            // The main frame is not presented until after this time.
            //  No need to update its LaF.
            setModelProperty(DISPLAY_LOOK_AND_FEEL, lookAndFeelClassName);
        }
        catch (Exception ex) {
            handleException(ex);
        }
    }

    public boolean isUnloadImages() {
        Boolean unloadImagesBool = (Boolean) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.UNLOAD_IMAGES_PROPERTY);
        if (unloadImagesBool != null && unloadImagesBool) {
            return true;
        }
        return false;
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
                Runnable runnable = new Runnable() {
                    public void run() {
                        mainFrame = WindowLocator.getMainFrame();
                    }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                    runnable.run();
                }
                else {
                    SwingUtilities.invokeAndWait(runnable);
                }
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

    public void stopExternalHttpListener() {
        if (externalHttpListener != null) {
            externalHttpListener.stop();
            externalHttpListener = null;
        }
    }

    public void startAxisServer(String url) {
        try {
            if (axisServer == null) {
                axisServer = new EmbeddedAxisServer(url);
            }
            axisServer.start();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public void stopAxisServer() {
        if (axisServer != null) {
            axisServer.stop();
            axisServer = null;
        }
    }

    public EmbeddedAxisServer getAxisServer() {
        return axisServer;
    }

    public void startWebServer(int port) {
        try {
            if (webServer == null) {
                webServer = new EmbeddedWebServer(port);
            }
            webServer.start();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public void stopWebServer() {
        if (webServer != null) {
            try {
                webServer.stop();
                webServer = null;
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
    }

    public EmbeddedWebServer getWebServer() {
        return webServer;
    }

    public Browser getBrowserFor(BrowserModel model) {
        return (Browser) browserModelsToBrowser.get(model);
    }

    public void saveUserSettings() {
        writeSettings();
    }

    private void writeSettings() {
        try {
            settingsFile.delete();
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

    public boolean loginSubject() {
        try {
            boolean relogin = false;

            if (isLoggedIn()) {
                logoutUser();
                log.info("RELOGIN");
                relogin = true;
            }

            // Login and start the session
            Subject tmpSubjectSubject = FacadeManager.getFacadeManager().getComputeFacade().loginSubject();
            authenticatedSubject = tmpSubjectSubject;
            if (null != authenticatedSubject) {
                isLoggedIn = true;

                String runAsUser = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER);
                loggedInSubject = StringUtils.isEmpty(runAsUser) ? authenticatedSubject : ModelMgr.getModelMgr().getSubject(runAsUser);

                if (loggedInSubject == null) {
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Cannot run as non-existent subject " + runAsUser, "Error", JOptionPane.ERROR_MESSAGE);
                    loggedInSubject = authenticatedSubject;
                }

                if (!authenticatedSubject.getId().equals(loggedInSubject.getId())) {
                    log.info("Authenticated as {} (Running as {})", authenticatedSubject.getKey(), loggedInSubject.getId());
                }
                else {
                    log.info("Authenticated as {}", authenticatedSubject.getKey());
                }

                FacadeManager.getFacadeManager().getComputeFacade().beginSession();

                if (relogin) {
                    log.info("Clearing entity model");
                    ModelMgr.getModelMgr().reset();
                    if (SessionMgr.getBrowser() != null) {
                        log.info("Refreshing all views");
                        SessionMgr.getBrowser().getEntityOutline().refresh();
                        SessionMgr.getBrowser().getOntologyOutline().refresh();
                        SessionMgr.getBrowser().getViewerManager().clearAllViewers();
                    }
                }
            }

            return isLoggedIn;
        }
        catch (Exception e) {
            log.error("loginUser: exception caught", e);
            isLoggedIn = false;
            log.error("Error logging in", e);
            throw new SystemError("Cannot authenticate login. The server may be down. Please try again later.");
        }
    }

    public void logoutUser() {
        try {
            if (loggedInSubject != null) {
                FacadeManager.getFacadeManager().getComputeFacade().endSession();
                log.info("Logged out with: {}", loggedInSubject.getKey());
            }
            isLoggedIn = false;
            loggedInSubject = null;
            authenticatedSubject = null;
        }
        catch (Exception e) {
            log.error("Error logging out", e);
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }

    public void setSubject(Subject subject) {
        this.loggedInSubject = subject;
    }

    public Subject getSubject() {
        return loggedInSubject;
    }

    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
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

    public static boolean authenticatedSubjectIsInGroup(String groupName) {
        Subject subject = SessionMgr.getSessionMgr().getAuthenticatedSubject();
        if (subject instanceof User) {
            return isUserInGroup((User) subject, groupName);
        }
        else {
            return false;
        }
    }

    public static boolean currentUserIsInGroup(String groupName) {
        Subject subject = SessionMgr.getSessionMgr().getSubject();
        if (subject instanceof User) {
            return isUserInGroup((User) subject, groupName);
        }
        else {
            return false;
        }
    }

    private static boolean isUserInGroup(User targetUser, String targetGroup) {
        if (null == targetUser) {
            return false;
        }
        for (SubjectRelationship relation : targetUser.getGroupRelationships()) {
            if (relation.getGroup().getName().equals(targetGroup)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getSubjectKeys() {
        List<String> subjectKeys = new ArrayList<String>();
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

    public static String getSubjectKey() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            if (rootAccess) {
                return null;
            }
            throw new SystemError("Not logged in");
        }
        return subject.getKey();
    }

    public static String getUsername() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            if (rootAccess) {
                return null;
            }
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            if (rootAccess) {
                return null;
            }
            throw new SystemError("Not logged in");
        }
        return subject.getEmail();
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
            log.error("Local file cache is not available");
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

    public Long getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(Long currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
}
