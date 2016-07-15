package org.janelia.it.workstation.gui.browser.api;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.api.stub.data.FatalCommError;
import org.janelia.it.workstation.api.stub.data.SystemError;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.lifecycle.LoginEvent;
import org.janelia.it.workstation.gui.browser.events.lifecycle.RunAsEvent;
import org.janelia.it.workstation.gui.framework.session_mgr.LoginProperties;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the data access credentials and current privileges.
 */
public final class AccessManager {
    
    private static final Logger log = LoggerFactory.getLogger(AccessManager.class);

    public static String RUN_AS_USER = "RunAs";
    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;

    private static final AccessManager accessManager = new AccessManager();
    private ActivityLogHelper activityLogHelper = new ActivityLogHelper();
    
    private boolean isLoggedIn;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;

    private AccessManager() {
        log.info("Initializing Access Manager");
        String tempLogin = (String) SessionMgr.getSessionMgr().getModelProperty(USER_NAME);
        String tempPassword = (String) SessionMgr.getSessionMgr().getModelProperty(USER_PASSWORD);
        if (tempLogin != null && tempPassword != null) {
            PropertyConfigurator.getProperties().setProperty(USER_NAME, tempLogin);
            PropertyConfigurator.getProperties().setProperty(USER_PASSWORD, tempPassword);
        }
    }

    static public AccessManager getAccessManager() {
        return accessManager;
    }

    public boolean loginSubject(String username, String password) {
        try {
            if (isLoggedIn()) {
                logoutUser();
            }

            // Login and start the session
            authenticatedSubject = authenticateSubject(username, password);
            if (null != authenticatedSubject) {
                isLoggedIn = true;                
                setSubject(authenticatedSubject);
                log.info("Authenticated as {}", authenticatedSubject.getKey());
                Events.getInstance().postOnEventBus(new LoginEvent(authenticatedSubject));
                beginSession();
            }

            return isLoggedIn;
        }
        catch (Exception e) {
            isLoggedIn = false;
            log.error("Error logging in", e);
            throw new FatalCommError(ConsoleProperties.getInstance().getProperty("interactive.server.url"),
                    "Cannot authenticate login. The server may be down. Please try again later.");
        }
    }

    private Subject authenticateSubject(final String username, final String password) {
        // make RESTful call to authenticate user

        try {
            Subject authenticatedSubject = DomainMgr.getDomainMgr().getModel().loginSubject(username, password);

            if (authenticatedSubject!=null) {
                log.debug("Setting default authenticator");
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });
                SessionMgr.getSessionMgr().getWebDavClient().setCredentialsUsingAuthenticator();
            }
            return authenticatedSubject;
        } 
        catch (Exception e) {
            log.error("Problem getting the subject using key " + username, e);
        }
        return null;
    }

    private void beginSession() {
        // TO DO: add to eventBus server logging
        UserToolEvent loginEvent = EJBFactory.getRemoteComputeBean().beginSession(
                SessionMgr.getSessionMgr().getSubject().getName(),
                ConsoleProperties.getString("console.Title"),
                ConsoleProperties.getString("console.versionNumber"));
        if (null!=loginEvent && null!=loginEvent.getSessionId()) {
            SessionMgr.getSessionMgr().setCurrentSessionId(loginEvent.getSessionId());
        }
        activityLogHelper.logUserInfo(authenticatedSubject);
    }

    private void endSession() {
        // TO DO: add to eventBus server logging
        activityLogHelper.logSessionEnd();
    }

    public boolean setRunAsUser(String runAsUser) {
        
        if (!AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin) && !StringUtils.isEmpty(runAsUser)) {
            throw new IllegalStateException("Non-admin user cannot run as another user");
        }
        
        try {
            if (!StringUtils.isEmpty(runAsUser)) {
                // set subject from RESTful service
                String fullUserKey = runAsUser;
                if (!runAsUser.startsWith("user") || runAsUser.startsWith("group")) {
                    fullUserKey = "user:" + fullUserKey;
                }
                Subject runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByKey(fullUserKey);
                if (runAsSubject==null) {
                    // try group before failing
                    fullUserKey = "group:" + runAsUser;
                    runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByKey(fullUserKey);
                    if (runAsSubject==null)
                        return false;
                }
                setSubject(runAsSubject);
            }
            else {
                setSubject(authenticatedSubject);
            }

            Events.getInstance().postOnEventBus(new RunAsEvent(authenticatedSubject));
            log.info("Running as {}", getSubject().getKey());
                
            return true;
        }
        catch (Exception e) {
            setSubject(authenticatedSubject);
            SessionMgr.getSessionMgr().handleException(e);
            return false;
        }
    }
    
    public void logoutUser() {
        try {
            if (getSubject() != null) {
                endSession();
                log.info("Logged out with: {}", getSubject().getKey());
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

    private void setSubject(Subject subject) {
        this.loggedInSubject = subject;
        // TODO: This is a temporary hack to inject this information back into the old modules. It should go away eventually.
        SessionMgr.getSessionMgr().setSubjectKey(
                authenticatedSubject==null?null:authenticatedSubject.getKey(),
                loggedInSubject==null?null:loggedInSubject.getKey());
    }

    public Subject getSubject() {
        return loggedInSubject;
    }

    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
    }

    public static Subject getSubjectByKey(String key) {
        try {
            return DomainMgr.getDomainMgr().getModel().getSubjectByKey(key);
        } 
        catch (Exception e) {
            log.error("Error getting subject with key: " + key, e);
        }
        return null;
    }

    public static boolean authenticatedSubjectIsInGroup(SubjectRole role) {
        Subject subject = AccessManager.getAccessManager().getAuthenticatedSubject();
        return subject.getGroups().contains(role.getRole());
    }

    public static boolean currentUserIsInGroup(String groupName) {
        Subject subject = AccessManager.getAccessManager().getSubject();
        return subject.getGroups().contains(groupName);
    }

    public static List<String> getSubjectKeys() {
        List<String> subjectKeys = new ArrayList<>();
        Subject subject = AccessManager.getAccessManager().getSubject();
        if (subject != null) {
            subjectKeys.add(subject.getKey());
            subjectKeys.addAll(subject.getGroups());
        }
        return subjectKeys;
    }

    public static String getSubjectKey() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getKey();
    }

    public static String getUsername() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getEmail();
    }
}
