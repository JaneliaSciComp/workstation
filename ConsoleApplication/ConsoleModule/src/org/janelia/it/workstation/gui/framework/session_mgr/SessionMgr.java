package org.janelia.it.workstation.gui.framework.session_mgr;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.SystemError;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionMgr {

    private static final Logger log = LoggerFactory.getLogger(SessionMgr.class);

    @Deprecated
    public static String USER_EMAIL = "UserEmail";
    @Deprecated
    public static String JACS_DATA_PATH_PROPERTY = "SessionMgr.JacsDataPathProperty";
    @Deprecated
    public static String JACS_INTERACTIVE_SERVER_PROPERTY = "SessionMgr.JacsInteractiveServerProperty";
    @Deprecated
    public static String JACS_PIPELINE_SERVER_PROPERTY = "SessionMgr.JacsPipelineServerProperty";

    // Delete these after deleting settings panels

    @Deprecated
    public static String DISPLAY_FREE_MEMORY_METER_PROPERTY = "SessionMgr.DisplayFreeMemoryProperty";

    private static JFrame mainFrame;
    private static final ModelMgr modelManager = ModelMgr.getModelMgr();
    private static final SessionMgr sessionManager = new SessionMgr();
    private SessionModel sessionModel = SessionModel.getSessionModel();
    
    private ImageIcon browserImageIcon;
    private Browser activeBrowser;

    private SessionMgr() {

    } //Singleton enforcement


    static public SessionMgr getSessionMgr() {
        return sessionManager;
    }

    public SessionModel getSessionModel() {
        return sessionModel;
    }

    public Object setModelProperty(Object key, Object value) {
        return sessionModel.setModelProperty(key, value);
    }

    public Object getModelProperty(Object key) {
        return sessionModel.getModelProperty(key);
    }

    public void registerExceptionHandler(ExceptionHandler handler) {
        modelManager.registerExceptionHandler(handler);
    }

    public void setNewBrowserImageIcon(ImageIcon newImageIcon) {
        browserImageIcon = newImageIcon;
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

    public void systemWillExit() {
        sessionModel.systemWillExit();
        sessionModel.removeAllBrowserModels();
        modelManager.prepareForSystemExit();
    }
    
    public void systemExit(int errorlevel) {
        log.info("Exiting with code "+errorlevel);
        systemWillExit();
     // System-exit is now handled by NetBeans framework.
//        System.exit(errorlevel);
    }

    public void addSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.addSessionListener(sessionModelListener);
    }

    public void removeSessionModelListener(SessionModelListener sessionModelListener) {
        sessionModel.removeSessionListener(sessionModelListener);
    }

    public boolean isUnloadImages() {
        throw new UnsupportedOperationException();
    }

    public boolean isDarkLook() {
        return true;
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

    public void saveUserSettings() {
        throw new UnsupportedOperationException();
    }

    public String getApplicationOutputDirectory() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Get the URL for a standard path. It may be a local URL, if the file has been cached, or a remote
     * URL on the WebDAV server. It might even be a mounted location, if WebDAV is disabled.
     *
     * @param standardPath a standard system path
     * @return an accessible URL for the specified path
     */
    public static URL getURL(String standardPath) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a hack to inject the run-as subject key from an already-authenticated user in the NG world. 
     * It ties the legacy SessionMgr to the new AccessManager.
     */
    @Deprecated
    public void setSubjectKey(final String authSubjectKey, final String subjectKey) {
//        try {
//            authenticatedSubject = ModelMgr.getModelMgr().getSubjectWithPreferences(authSubjectKey);
//            if (subjectKey!=null) {
//                loggedInSubject = ModelMgr.getModelMgr().getSubjectWithPreferences(subjectKey);
//                isLoggedIn = true;
//            }
//            resetSession();
//            log.info("Completed legacy track init with authed user "+authSubjectKey+" and run as user "+subjectKey);
//        }
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//        }
    }
    
    @Deprecated
    public static String getSubjectKey() {
        return getSessionMgr().getSubject().getKey();
    }

    @Deprecated
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

    @Deprecated
    public Subject getSubject() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static String getUsername() {
        Subject subject = getSessionMgr().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

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
}
