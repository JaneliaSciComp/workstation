package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.SystemError;
import org.janelia.it.FlyWorkstation.gui.dataview.DataviewApp;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.external_listener.ExternalListener;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.shared.filestore.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.util.filecache.LocalFileCache;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClient;
import org.janelia.it.FlyWorkstation.web.EmbeddedWebServer;
import org.janelia.it.FlyWorkstation.ws.EmbeddedAxisServer;
import org.janelia.it.FlyWorkstation.ws.ExternalClient;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.StringUtils;
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


public class SessionMgr {

	private static final Logger log = LoggerFactory.getLogger(SessionMgr.class);
	
    public static String DISPLAY_FREE_MEMORY_METER_PROPERTY = "SessionMgr.DisplayFreeMemoryProperty";
    public static String DISPLAY_SUB_EDITOR_PROPERTY = "SessionMgr.DisplaySubEditorProperty";
    public static String JACS_DATA_PATH_PROPERTY = "SessionMgr.JacsDataPathProperty";
    public static String JACS_INTERACTIVE_SERVER_PROPERTY = "SessionMgr.JacsInteractiveServerProperty";
    public static String JACS_PIPELINE_SERVER_PROPERTY = "SessionMgr.JacsPipelineServerProperty";
    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;
    public static String USER_EMAIL = "UserEmail";
    public static String FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY = "console.localCache.gigabyteCapacity";
    public static String RUN_AS_USER = "RunAs";

    public static String DISPLAY_LOOK_AND_FEEL = "SessionMgr.JavaLookAndFeel";

    public static boolean isDarkLook = false;
    
    //  private static String PROPERTY_CREATION_RULES="SessionMgr.PropertyCreationRules";
    private static ModelMgr modelManager = ModelMgr.getModelMgr();
    private static SessionMgr sessionManager = new SessionMgr();
    private SessionModel sessionModel = SessionModel.getSessionModel();
    private float browserSize = .8f;
    private String browserTitle;
    private ImageIcon browserImageIcon;
    private Component splashPanel;
    //private String releaseVersion="$date$";
    private ExternalListener externalHttpListener;
    private EmbeddedAxisServer axisServer;
    private EmbeddedWebServer webServer;
    private File settingsFile;
    private String fileSep = File.separator;
    private String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private String prefsFile = prefsDir + ".FW_Settings";
    private Map browserModelsToBrowser = new HashMap();
    private String backupFileName = null;
    private WindowListener myBrowserWindowListener = new MyBrowserListener();
    private Browser activeBrowser;
    private String appName, appVersion;
    private Date sessionCreationTime;
    private boolean isLoggedIn;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;
    private WebDavClient webDavClient;
    private LocalFileCache localFileCache;
    
    private SessionMgr() {
    	
    	log.info("Initializing Session Manager");
    	
        settingsFile = new File(prefsFile);
        try {
            settingsFile.createNewFile();  //only creates if does not exist
        }
        catch (IOException ioEx) {
            try {
                new File(prefsDir).mkdirs();
                settingsFile.createNewFile();  //only creates if does not exist
            }
            catch (IOException ioEx1) {
            	log.error("Cannot create settings file!! " + ioEx1.getMessage());
            }
        }

        readSettingsFile();
        EJBFactory.initFromModelProperties(sessionModel);
        PathTranslator.initFromModelProperties(sessionModel);
                
        // -----------------------------------------------
        // initialize WebDAV and local cache components

        webDavClient = new WebDavClient(
                ConsoleProperties.getString("console.webDavClient.baseUrl",
                                            "http://jacs.int.janelia.org/WebDAV"),
                ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100),
                ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100));

        Integer configuredGigabyteCapacity = (Integer)
                getModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
        if (configuredGigabyteCapacity == null) {
            configuredGigabyteCapacity = ConsoleProperties.getInt(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, 0);
            setModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, configuredGigabyteCapacity);
        }

        final String localCacheRoot = ConsoleProperties.getString("console.localCache.rootDirectory", prefsDir);
        final int minimumCacheGigabyteCapacity = 1;
        if (configuredGigabyteCapacity >= minimumCacheGigabyteCapacity) {
            try {
                final long kilobyteCapacity = configuredGigabyteCapacity * 1024 * 1024;
                localFileCache = new LocalFileCache(new File(localCacheRoot),
                                                    kilobyteCapacity,
                                                    webDavClient);
            } catch (Exception e) {
                localFileCache = null;
                log.error("disabling local cache after initialization failure", e);
            }
        } else {
            log.warn("disabling local cache since configured size of {} GB is less than minimum size of {} GB",
                     configuredGigabyteCapacity, minimumCacheGigabyteCapacity);
        }

        // -----------------------------------------------
        if (getModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY) == null) {
            setModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY, true);
        }
        
        if (getModelProperty(DISPLAY_SUB_EDITOR_PROPERTY) == null) { 
        	setModelProperty(DISPLAY_SUB_EDITOR_PROPERTY, true); 
        }
        
//      if (getModelProperty(PROPERTY_CREATION_RULES)!=null) {
//        Set rules= (Set)getModelProperty(PROPERTY_CREATION_RULES);
//          for (Object rule : rules) {
//              PropertyMgr.getPropertyMgr().addPropertyCreationRule((PropertyCreationRule) rule);
//          }
//      }

        String[] li = {"Licensee=HHMI", "LicenseRegistrationNumber=122030", "Product=Synthetica", "LicenseType=Single Application License", "ExpireDate=--.--.----", "MaxVersion=2.999.999"};
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", "1839F3DB-00416A48-64C9E2C5-F9E25A71-A885FFC0");
        
        UIManager.installLookAndFeel("Synthetica AluOxide Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaAluOxideLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackEye Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackMoon Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlackStar Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel");
        UIManager.installLookAndFeel("Synthetica BlueIce Look and Feel", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel");
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
        
        String lafName = (String) getModelProperty(DISPLAY_LOOK_AND_FEEL);
        if (lafName != null) {
            try {
            	setLookAndFeel(lafName);
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
        if (null!=tmpCache) {
            PropertyConfigurator.getProperties().setProperty(FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, tmpCache.toString());
        }
        sessionCreationTime = new Date();
        // TODO: Bundle FIJI with Fly Workstation
    } //Singleton enforcement

	private String getSafeModelProperty(String targetKey) {
        Object testValue = getModelProperty(targetKey);
        if (null==testValue || !(testValue instanceof String)) {
            return "";
        }
        else {
            return (String)testValue;
        }
    }

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
            SessionMgr.getSessionMgr().handleException(ioEx);
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

    public List<ExternalClient> getExternalClientsByName(String clientName){
        return sessionModel.getExternalClientsByName(clientName);
    }

    public ExternalClient getExternalClientByPort(int targetPort) {
        return sessionModel.getExternalClientByPort(targetPort);
    }

    public void removeExternalClientByPort(int targetPort){
        sessionModel.removeExternalClientByPort(targetPort);
    }

    public void sendMessageToExternalClients(String operationName, Map<String,Object> parameters) {
    	sessionModel.sendMessageToExternalClients(operationName, parameters);
    }
    
    public static KeyBindings getKeyBindings() {
        return SessionModel.getKeyBindings();
    }

    public Object getModelProperty(Object key) {
        return sessionModel.getModelProperty(key);
    }

    public void removeModelProperty(Object key){
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

    public void setNewBrowserMenuBar(Class menuBarClass) {
        Browser.setMenuBarClass(menuBarClass);
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
    public boolean isLocalFileCacheAvailable() {
        return (localFileCache != null);
    }

    /**
     * @return the session local file cache instance or null if a cache is not available.
     */
    public LocalFileCache getLocalFileCache() {
        return localFileCache;
    }

    /**
     * Register an editor for a model type
     *
     */
//  public void registerEditorForType(Class type, Class editor, String editorName, boolean defaultEditorForType) throws Exception {
//    Browser.registerEditorForType(type,editor,editorName,defaultEditorForType);
//  }


    /**
     * Register the editor for this type, but only under the specified protocol
     */
//  public void registerEditorForType(Class type, Class editor, String editorName, String protocol, boolean defaultEditorForType) throws Exception {
//    if (FacadeManager.isProtocolRegistered(protocol))  Browser.registerEditorForType(type,editor,editorName,defaultEditorForType);
//  }
//
//  public void registerSubEditorForMainEditor(Class mainEditor, Class subEditor, String protocol) throws Exception {
//    if (FacadeManager.isProtocolRegistered(protocol))  registerSubEditorForMainEditor(mainEditor, subEditor);
//  }
//
//  public void registerSubEditorForMainEditor(Class mainEditor, Class subEditor) throws Exception {
//    Browser.registerSubEditorForMainEditor(mainEditor,subEditor);
//  }
    public void handleException(Throwable throwable) {
        modelManager.handleException(throwable);
    }

    public Browser newBrowser() {
        Browser browser = new Browser(browserSize, sessionModel.addBrowserModel());
        browser.addWindowListener(myBrowserWindowListener);
        if (browserTitle != null) {
            browser.setTitle(browserTitle);
        }
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

    public void cloneBrowser(Browser browser) {
        Browser newBrowser = (Browser) browser.clone();
        newBrowser.addWindowListener(myBrowserWindowListener);
        sessionModel.addBrowserModel(newBrowser.getBrowserModel());
        newBrowser.setVisible(true);
        browserModelsToBrowser.put(newBrowser.getBrowserModel(), newBrowser);
    }


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
        System.exit(errorlevel);
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

    /**
     * This method will be used to write out the Session Log in
     * time in the annotationLog file. Note the SessionLog in time
     * will be same if multiple browsers are open.
     */
    public Date getSessionCreationTime() {
        return sessionCreationTime;
    }


    public int getNumberOfOpenBrowsers() {
        return sessionModel.getNumberOfBrowserModels();
    }

    public void setLookAndFeel(String lookAndFeelClassName) {
        try {
        	if (lookAndFeelClassName.contains("BlackEye")) {
        		isDarkLook = true;
            	UIManager.setLookAndFeel(new de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel() {
					@Override
					protected void loadCustomXML() throws ParseException {
						loadXMLConfig("/SyntheticaBlackEyeLookAndFeel.xml");
					}
            	});	
        	}
        	else {
        		UIManager.setLookAndFeel(lookAndFeelClassName);	
        	}
            
            Set browserModels = browserModelsToBrowser.keySet();
            Object obj;
            for (Object browserModel : browserModels) {
                obj = browserModelsToBrowser.get(browserModel);
                if (obj != null) {
                    SwingUtilities.updateComponentTreeUI((JFrame) obj);
                    ((JFrame) obj).repaint();
                }
            }
            setModelProperty(DISPLAY_LOOK_AND_FEEL, lookAndFeelClassName);
        }
        catch (Exception ex) {
            handleException(ex);
        }
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
    
    public void startExternalHttpListener(int port) {
        if (externalHttpListener == null) externalHttpListener = new ExternalListener(port);
    }

    public void stopExternalHttpListener() {
        if (externalHttpListener != null) {
            externalHttpListener.stop();
            externalHttpListener = null;
        }
    }

    public void startAxisServer(String url) {
    	try {
	        if (axisServer == null) axisServer = new EmbeddedAxisServer(url);
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
            if (webServer == null) webServer = new EmbeddedWebServer(port);
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

	public void resetSession() {
        Set keys = browserModelsToBrowser.keySet();
        List browserList = new ArrayList(keys.size());
        for (Object key : keys) {
            browserList.add(key);
        }
        Browser[] browsers = (Browser[]) browserList.toArray(new Browser[0]);
        for (Browser browser : browsers) {
            browser.closeAllViews();
            browser.getBrowserModel().reset();
        }
        FacadeManager.resetFacadeManager();
    }

    public Browser getBrowserFor(BrowserModel model) {
        return (Browser) browserModelsToBrowser.get(model);
    }

    //  public void addPropertyCreationRule(PropertyCreationRule rule) {
//    PropertyMgr.getPropertyMgr().addPropertyCreationRule(rule);
//    Set rules=(Set)getModelProperty(PROPERTY_CREATION_RULES);
//    if (rules==null) rules = new HashSet();
//    rules.add(rule);
//    setModelProperty(PROPERTY_CREATION_RULES,rules);
//  }
//
//  public Set getPropertyCreationRules() {
//    return PropertyMgr.getPropertyMgr().getPropertyCreationRules();
//  }
//
//  public void removePropertyCreationRule(String ruleName){
//    PropertyMgr.getPropertyMgr().removePropertyCreationRule(ruleName);
//    Set rules=(Set)getModelProperty(PROPERTY_CREATION_RULES);
//    PropertyCreationRule rule;
//    for (Iterator it=rules.iterator();it.hasNext();){
//      rule=(PropertyCreationRule)it.next();
//      if (rule.getName().equals(ruleName)) it.remove();
//    }
//  }
//
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
        	log.info("Saving user settings to "+settingsFile.getAbsolutePath());
        }
        catch (IOException ioEx) {
            handleException(ioEx);
        }
    }


    public void setBackupFileName(String userChosenLocation) {
        backupFileName = userChosenLocation;
    }
    
    public boolean loginSubject() {
        try {
            boolean relogin = false;
            
        	if (isLoggedIn()) {
        		logoutUser();
        		log.info("RELOGIN");    
        		relogin = true;
        	}
        	
            authenticatedSubject =  ModelMgr.getModelMgr().loginSubject();
            if (null!=authenticatedSubject) { 
                isLoggedIn = true; 
                
                String runAsUser = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER);
                loggedInSubject = StringUtils.isEmpty(runAsUser) ? authenticatedSubject : ModelMgr.getModelMgr().getSubject(runAsUser);
                
                if (loggedInSubject==null) {
                    JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Cannot run as non-existent subject "+runAsUser, "Error", JOptionPane.ERROR_MESSAGE);
                    loggedInSubject = authenticatedSubject;
                }

                if (!authenticatedSubject.getId().equals(loggedInSubject.getId())) {
                    log.info("Authenticated as {} (Running as {})",authenticatedSubject.getKey(),loggedInSubject.getId());
                }
                else {
                    log.info("Authenticated as {}",authenticatedSubject.getKey());    
                }
                
                if (relogin) {
                    log.info("Clearing all caches");    
                    ModelMgr.getModelMgr().invalidateCache();
                    log.info("Refreshing all views");
                    SessionMgr.getBrowser().getEntityOutline().refresh();
                    SessionMgr.getBrowser().getViewerManager().clearAllViewers();
                }
            }

            return isLoggedIn;
        }
        catch (Exception e) {
            log.error("loginUser: exception caught", e);
        	isLoggedIn = false;
        	log.error("Error logging in",e);
            throw new SystemError("Cannot authenticate login. The server may be down. Please try again later.");
        }
    }
    
    public void logoutUser() {
    	try {
    		ModelMgr.getModelMgr().logoutSubject();
    		log.info("Logged out with: {}",loggedInSubject.getKey());
    		isLoggedIn = false;
        	loggedInSubject = null;
            authenticatedSubject = null;
    	}
    	catch (Exception e) {
    	    log.error("Error logging out",e);
    	}
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }


    public Subject getSubject() {
        return loggedInSubject;
    }

    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
    }

    class MyBrowserListener extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
        	e.getWindow().removeWindowListener(this);
            SessionMgr.getSessionMgr().saveUserSettings();
        }

        public void windowActivated(WindowEvent e) {
            activeBrowser = (Browser) e.getWindow();
        }
    }

    public static boolean authenticatedSubjectIsInGroup(String groupName) {
        Subject subject = SessionMgr.getSessionMgr().getAuthenticatedSubject();
        if (subject instanceof User) {
            return isUserInGroup((User)subject, groupName);
        }
        else {
            return false;
        }
    }

    public static boolean currentUserIsInGroup(String groupName) {
    	Subject subject = SessionMgr.getSessionMgr().getSubject();
    	if (subject instanceof User) {
    	    return isUserInGroup((User)subject, groupName);
    	}
    	else {
    	    return false;
    	}
    }

	private static boolean isUserInGroup(User targetUser, String targetGroup) {
        if (null==targetUser) return false;
        for(SubjectRelationship relation : targetUser.getGroupRelationships()) {
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
            	for(SubjectRelationship relation : ((User)subject).getGroupRelationships()) {
            		subjectKeys.add(relation.getGroup().getKey());
            	}
        	}
    	}
    	return subjectKeys;
	}
    
    public static String getSubjectKey() {
        Subject subject = getSessionMgr().getSubject();
        if (subject==null) {
            if (DataviewApp.getMainFrame()!=null) return null;
            throw new SystemError("Not logged in");
        }
        return subject.getKey();
    }
    
    public static String getUsername() {
        Subject subject = getSessionMgr().getSubject();
        if (subject==null) {
            if (DataviewApp.getMainFrame()!=null) return null;
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getSessionMgr().getSubject();
        if (subject==null) {
            if (DataviewApp.getMainFrame()!=null) return null;
            throw new SystemError("Not logged in");
        }
        return subject.getEmail();
    }

    /**
     * @return the maximum number of gigabytes to store in the local file cache.
     */
    public static int getFileCacheGigabyteCapacity() {
        final SessionMgr mgr = SessionMgr.getSessionMgr();
        return (Integer) mgr.getModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
    }

    /**
     * Sets the local file cache capacity and saves the setting as a session preference.
     *
     * @param  gigabyteCapacity  cache capacity in gigabytes.
     */
    public static void setFileCacheGigabyteCapacity(int gigabyteCapacity) {
        final SessionMgr mgr = SessionMgr.getSessionMgr();
        mgr.setModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, gigabyteCapacity);
        if (mgr.isLocalFileCacheAvailable()) {
            LocalFileCache cache = mgr.getLocalFileCache();
            final long kilobyteCapacity = gigabyteCapacity * 1024 * 1024;
            if (kilobyteCapacity != cache.getKilobyteCapacity()) {
                cache.setKilobyteCapacity(kilobyteCapacity);
            }
        }
    }

    /**
     * Removes all locally cached files.
     */
//    public static void clearFileCache() {
//        final SessionMgr mgr = SessionMgr.getSessionMgr();
//        LocalFileCache cache = mgr.getLocalFileCache();
//        cache.clear();
//    }

    /**
     * If local caching is enabled, this method will cache the requested
     * system file (as needed) and return the cached file.
     * If local caching is disabled, this method will simply convert
     * the specified path for the current platform assuming that
     * the file is to be loaded via remote mount.
     *
     * @param  standardPath  the standard system path for the file.
     *
     * @param  forceRefresh  indicates if any existing cached file
     *                       should be forcibly refreshed before
     *                       being returned.  In most cases, this
     *                       should be set to false.
     *
     * @return an accessible file for the specified path.
     */
    public static File getCachedFile(String standardPath,
                               boolean forceRefresh) {

        final SessionMgr mgr = SessionMgr.getSessionMgr();

        File file = null;
        if (mgr.isLocalFileCacheAvailable()) {
            final LocalFileCache cache = mgr.getLocalFileCache();
            final WebDavClient client = mgr.getWebDavClient();
            try {
                final URL url = client.getWebDavUrl(standardPath);
                file = cache.getFile(url, forceRefresh);
            } catch (Exception e) {
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
     * @param standardPath a standard system path
     * @return an accessible URL for the specified path
     * @throws Exception
     */
    public static URL getURL(String standardPath) {
        try {
            SessionMgr sessionMgr = getSessionMgr();
            WebDavClient client = sessionMgr.getWebDavClient();
            URL remoteFileUrl = client.getWebDavUrl(standardPath);
            LocalFileCache cache = sessionMgr.getLocalFileCache();
            return sessionMgr.isLocalFileCacheAvailable() ? cache.getEffectiveUrl(remoteFileUrl) : remoteFileUrl;
        }
        catch (MalformedURLException e) {
            SessionMgr.getSessionMgr().handleException(e);
            return null;
        }
    }
}
