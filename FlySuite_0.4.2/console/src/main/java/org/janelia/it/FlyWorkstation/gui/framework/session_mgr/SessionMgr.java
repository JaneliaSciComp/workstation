package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.external_listener.ExternalListener;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.ws.EmbeddedAxisServer;
import org.janelia.it.jacs.model.user_data.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.*;
import java.util.List;


public class SessionMgr {

    public static String DISPLAY_FREE_MEMORY_METER_PROPERTY = "SessionMgr.DisplayFreeMemoryProperty";
    public static String DISPLAY_SUB_EDITOR_PROPERTY = "SessionMgr.DisplaySubEditorProperty";

    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;

    public static String DISPLAY_LOOK_AND_FEEL = "SessionMgr.DisplayLookAndFeel";

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

    private SessionMgr() {
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
                System.err.println("Cannot create settings file!! " + ioEx1.getMessage());
            }
        }

        readSettingsFile();
        if (getModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY) == null)
            setModelProperty(DISPLAY_FREE_MEMORY_METER_PROPERTY, true);
//        if (getModelProperty(DISPLAY_SUB_EDITOR_PROPERTY) == null) setModelProperty(DISPLAY_SUB_EDITOR_PROPERTY, true);
//      if (getModelProperty(PROPERTY_CREATION_RULES)!=null) {
//        Set rules= (Set)getModelProperty(PROPERTY_CREATION_RULES);
//          for (Object rule : rules) {
//              PropertyMgr.getPropertyMgr().addPropertyCreationRule((PropertyCreationRule) rule);
//          }
//      }
        if (getModelProperty(DISPLAY_LOOK_AND_FEEL) != null) {
            try {
                setLookAndFeel((String) getModelProperty(DISPLAY_LOOK_AND_FEEL));
            }
            catch (Exception ex) {
                handleException(ex);
            }
        }
        else {
            setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        String tempLogin = (String) getModelProperty(USER_NAME);
        String tempPassword = (String) getModelProperty(USER_PASSWORD);
        if (tempLogin != null && tempPassword != null) {
            PropertyConfigurator.getProperties().setProperty(USER_NAME, tempLogin);
            PropertyConfigurator.getProperties().setProperty(USER_PASSWORD, tempPassword);
        }
        sessionCreationTime = new Date();
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
            SessionMgr.getSessionMgr().handleException(ioEx);
        } //new settingsFile
    }

    static public SessionMgr getSessionMgr() {
        return sessionManager;
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
     * Makes whole model read-only
     */
    public void makeReadOnly() {
        modelManager.makeReadOnly();
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
        System.err.println("Memory in use at exit: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000f + " MB");
        System.err.flush();
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
            UIManager.setLookAndFeel(lookAndFeelClassName);
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

    public void startAxisServer(int port) {
    	try {
	        if (axisServer == null) axisServer = new EmbeddedAxisServer(port);
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
        ModelMgr.getModelMgr().removeAllOntologies();
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
    private void writeSettings() {
        try {
            System.out.println("Saving user settings.");
            settingsFile.delete();
            ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(settingsFile));
            ostream.writeInt(1);  //stream format
            ostream.writeObject(sessionModel.getModelProperties());
            ostream.flush();
            ostream.close();
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
            return ModelMgr.getModelMgr().getUser();
        }
        catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    class MyBrowserListener extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
            e.getWindow().removeWindowListener(this);
        }

        public void windowActivated(WindowEvent e) {
            activeBrowser = (Browser) e.getWindow();
        }
    }

    public static String getUsername() {
        return (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
    }

}
