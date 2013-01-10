package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.SystemError;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.external_listener.ExternalListener;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.ws.EmbeddedAxisServer;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
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
    public static String CACHE_SIZE_PROPERTY = "SessionMgr.CacheSize";
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
    private String loggedInSubjectName;
    private User loggedInUser;
    private User authenticatedUser;
    
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
        Integer tmpCache = (Integer) getModelProperty(CACHE_SIZE_PROPERTY);
        if (null!=tmpCache) {
            PropertyConfigurator.getProperties().setProperty(CACHE_SIZE_PROPERTY, tmpCache.toString());
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
        browser.setVisible(true);
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
        log.info("Logged out");
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
    
    public Browser getActiveBrowser() {
        return activeBrowser;
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

    public User getUser() {
        try {
            return loggedInUser;
        }
        catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    public boolean loginUser() {
        try {
        	if (isLoggedIn()) {
        		logoutUser();
        		ModelMgr.getModelMgr().invalidateCache();
        	}
            authenticatedUser =  ModelMgr.getModelMgr().loginUser();
            if (null!=authenticatedUser) { isLoggedIn = true; }
            loggedInSubjectName = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
            loggedInUser = ModelMgr.getModelMgr().getUser();
            return isLoggedIn;
        }
        catch (Exception e) {
        	isLoggedIn = false;
        	loggedInSubjectName = null;
            throw new SystemError("Cannot authenticate login. The server may be down. Please try again later.");
        }
    }
    
    public void logoutUser() {
    	try {
    		ModelMgr.getModelMgr().logoutUser(loggedInSubjectName);
    		isLoggedIn = false;
        	loggedInSubjectName = null;
        	loggedInUser = null;
            authenticatedUser = null;
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getApplicationOutputDirectory() {
        return prefsDir;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
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

    public static boolean authenticatedUserIsInGroup(String groupName) {
        User user = SessionMgr.getSessionMgr().getAuthenticatedUser();
        return isUserInGroup(user, groupName);
    }

    public static boolean currentUserIsInGroup(String groupName) {
    	User user = SessionMgr.getSessionMgr().getUser();
        return isUserInGroup(user, groupName);
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
    	User user = SessionMgr.getSessionMgr().getUser();
    	subjectKeys.add(user.getKey());
    	for(SubjectRelationship relation : user.getGroupRelationships()) {
    		subjectKeys.add(relation.getGroup().getKey());
    	}
    	return subjectKeys;
	}
    
    public static String getSubjectKey() {
        try {
            String username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
            if (null!=SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER) &&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER))) {
                username = "user:" + SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER);
            }
            if (!username.contains(":")) {
            	username = "user:"+username;
            }
            return username;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static String getUsername() {
        try {
            String username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
            if (null!=SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER) &&
                    !"".equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER))) {
                username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.RUN_AS_USER);
            }
            return username;
        }
        catch (Exception e) {
            return null;
        }
    }

    public static String getUserEmail() {
        if (null!=SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL)) {
            String userEmail = SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL).toString();
            if (null != userEmail && userEmail.contains("@")){
                return userEmail;
            }
        }
        return null;
    }

    public static Integer getCacheSize() {
        if (null!=SessionMgr.getSessionMgr().getModelProperty(SessionMgr.CACHE_SIZE_PROPERTY)) {
            Integer cacheSize = (Integer)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.CACHE_SIZE_PROPERTY);
            if (null != cacheSize){
                return cacheSize;
            }
        }
        return ConsoleProperties.getInt(SessionMgr.CACHE_SIZE_PROPERTY);
    }

    public static Browser getBrowser() {
    	return getSessionMgr().getActiveBrowser();
    }
}
