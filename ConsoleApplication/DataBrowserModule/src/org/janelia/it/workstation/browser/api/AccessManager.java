package org.janelia.it.workstation.browser.api;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.exceptions.AuthenticationException;
import org.janelia.it.workstation.browser.api.exceptions.ServiceException;
import org.janelia.it.workstation.browser.api.exceptions.SystemError;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.LoginEvent;
import org.janelia.it.workstation.browser.events.lifecycle.SessionEndEvent;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;
import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog;
import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog.ErrorType;
import org.janelia.it.workstation.browser.util.SimpleJwtParser;
import org.janelia.model.domain.enums.SubjectRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.util.SubjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the access credentials and current privileges. Takes care of logging in users during start-up, and 
 * state changes while the application is running. 
 * 
 * There are two user concepts here: 
 * 1) the authenticated subject: the real user that provided their username/password and successfully authenticated
 * 2) the actual subject: the subject being used for all access in the application. Usually this is the same as the 
 *    authenticated subject, but if that subject is an admin, they can "Run As" another user, or even as a group.
 *    
 * This manager is also a state machine which encodes AuthState transitions. In particular:
 * 
 * Starting -> Logged in (if the user information is stored, and can be used to automatically log the user in at start up)
 * Starting -> Logged out (if the user cannot be logged in at start up for any reason)
 * Logged out -> Logged in (if the user authenticates manually after start up)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class AccessManager {
    
    private static final Logger log = LoggerFactory.getLogger(AccessManager.class);

    public static String RUN_AS_USER = "RunAs";
    public static String USER_NAME = "console.serverLogin";
    public static String USER_PASSWORD = "console.serverPassword";
    public static String REMEMBER_PASSWORD = "console.rememberPassword";

    private enum AuthState {
        Starting,
        LoggedIn,
        LoggedOut
    }
    
    // Start up state
    private AuthState currState = AuthState.Starting;
    private boolean hadLoginIssue;
    private ErrorType loginIssue;
    
    // Running state
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();
    private String username;
    private String password;
    private String token;
    private Date tokenExpirationDate; 
    private Date tokenCreationDate; 
    private Subject authenticatedSubject; 
    private Subject actualSubject;
    private static String bypassSubjectKey;
    private boolean isAdmin;
    private Set<String> readerSet;
    private Set<String> writerSet;


    // Singleton
    private static AccessManager accessManager;
    static public AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = new AccessManager();
        }
        return accessManager;
    }
    
    private AccessManager() {
        log.info("Initializing Access Manager");
        moveToStartingState();
    }
    
    private void moveToStartingState() {
        log.info("Moving to starting state");
        this.currState = AuthState.Starting;
        this.token = null;
        this.authenticatedSubject = null;
        this.actualSubject = null;
    }
    
    private void moveToLoggedInState(Subject authenticatedSubject) {
        log.info("Moving to logged in state");
        this.currState = AuthState.LoggedIn;
        this.authenticatedSubject = authenticatedSubject;
        this.isAdmin = AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
        
        ActivityLogHelper.logUserInfo(authenticatedSubject);
        Events.getInstance().postOnEventBus(new LoginEvent(authenticatedSubject));
        
        // Is there a run-as user? 
        boolean runningAs = false;
        LocalPreferenceMgr prefs = LocalPreferenceMgr.getInstance();
        String runAsUser = (String)prefs.getModelProperty(AccessManager.RUN_AS_USER);
        if (runAsUser!=null) {
            // If so, override the actual subject.
            try {
                if (setRunAsUser(runAsUser)) {
                    runningAs = true;
                }
            }
            catch (Exception e) {
                prefs.setModelProperty(AccessManager.RUN_AS_USER, "");
                ConsoleApp.handleException(e);
            }
        }
        
        if (!runningAs) {
            // Session was not started with a run-as user
            setActualSubject(authenticatedSubject);
        }
    }
    
    private void moveToLoggedOutState() {
        log.info("Moving to logged out state");
        this.currState = AuthState.LoggedOut;
        this.token = null;
        this.authenticatedSubject = null;
        setActualSubject(null);
    }
    
    /**
     * Attempt to log in using saved credentials during application start up. 
     * If this fails, a second (interactive) try can be made once the application 
     * is showing, by calling resolveLoginIssue. 
     */
    public void loginUsingSavedCredentials() {

        // Assume we'll have a login issue unless proven otherwise
        hadLoginIssue = true;
        loginIssue = null;
        
        // Get saved credentials
        LocalPreferenceMgr prefs = LocalPreferenceMgr.getInstance();
        String username = (String)prefs.getModelProperty(AccessManager.USER_NAME);
        String password = (String)prefs.getModelProperty(AccessManager.USER_PASSWORD);

        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            try {
                if (loginUser(username, password)) {
                    hadLoginIssue = false;
                }
                else {
                    moveToLoggedOutState();
                }
            }
            catch (AuthenticationException e) {
                log.warn("Authentication problem during auto-login", e);
                moveToLoggedOutState();
                loginIssue = ErrorType.AuthError;
            }
            catch (ServiceException e) {
                FrameworkImplProvider.handleExceptionQuietly("Problem encountered during auto-login", e);
                moveToLoggedOutState();
                loginIssue = ErrorType.NetworkError;
            }
            catch (Throwable t) {
                FrameworkImplProvider.handleExceptionQuietly("Problem encountered during auto-login", t);
                moveToLoggedOutState();
                loginIssue = ErrorType.OtherError;
            }
        }
    }
    
    /**
     * Should be called when the application is first displayed. If there were login
     * issues at startup, this will attempt to resolve them by showing the login 
     * dialog with an appropriate error message. 
     */
    public void resolveLoginIssue() {
        if (hadLoginIssue) {
            SwingUtilities.invokeLater(() -> {
                LoginDialog.getInstance().showDialog(loginIssue);
            });
        }
    }

    /**
     * Can be called to change the current "actual subject". 
     * @param runAsUser user or group (key or name)
     * @return true if the user was successfully made actual
     */
    public boolean setRunAsUser(String runAsUser) {
        
        if (!isAdmin() && !StringUtils.isEmpty(runAsUser)) {
            log.error("Non-admin user cannot run as another user");
            setActualSubject(authenticatedSubject);
            return false;
        }
        
        try {
            if (!StringUtils.isEmpty(runAsUser)) {
                String fullUserKey = runAsUser;
                if (!runAsUser.startsWith("user:") && !runAsUser.startsWith("group:")) {
                    fullUserKey = "user:" + fullUserKey;
                }
                Subject runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(fullUserKey);
                if (runAsSubject==null) {
                    // try group before failing
                    fullUserKey = "group:" + runAsUser;
                    runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(fullUserKey);
                    if (runAsSubject==null) {
                        return false;
                    }
                }
                setActualSubject(runAsSubject);
            }
            else {
                setActualSubject(authenticatedSubject);
            }

            log.info("Running as {}", getActualSubject().getKey());
            return true;
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
            setActualSubject(authenticatedSubject);
            return false;
        }
    }
    
    /**
     * This method can be called when the user requests a login dialog to be shown.
     */
    public void userRequestedLoginDialog() {
        SwingUtilities.invokeLater(() -> {
            LoginDialog.getInstance().showDialog();
        });
    }
    
    public boolean loginUser(String username, String password) {

        // Keep these in memory for token refreshes, because user may not want to persist the password
        this.username = username;
        this.password = password;
    
        // Authenticate
        Subject authenticatedSubject = authenticateSubject();
        
        if (authenticatedSubject != null) {
            moveToLoggedInState(authenticatedSubject);
            return true;
        }
        else {
            return false;
        }
    }
        
    private Subject authenticateSubject() {
        
        // First get auth token
        renewToken();
        
        // We're now authenticated. Get or create the Workstation user object.
        Subject authenticatedSubject;
        try {
            authenticatedSubject = DomainMgr.getDomainMgr().getModel().getOrCreateUser(username);
        }
        catch (Exception e) {
            throw new ServiceException("Error getting or creating user "+username, e);
        }
        return authenticatedSubject;
    }
    
    /**
     * Checks to see if the system is in a logged in state. 
     * @return true if a user is authenticated and logged in
     */
    public boolean isLoggedIn() {
        return currState==AuthState.LoggedIn;
    }

    /**
     * Checks to see if the current user is in the admin group.
     * @return true if current user is admin
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    public Set<String> getActualReaderSet() {
        return readerSet;
    }
    public Set<String> getActualWriterSet() {
        return writerSet;
    }
    
    /**
     * Returns the current authentication token.
     * @return JWS token 
     */
    public String getToken() {
        renewTokenIfNeeded();
        log.trace("Returning token: {}", token);
        return token;
    }

    private void renewToken() {

        tokenRefreshLock.lock();
        
        try {
            log.debug("Attempting to obtain new auth token for {}", username);
            this.token = DomainMgr.getDomainMgr().getAuthClient().obtainToken(username, password);
            this.tokenCreationDate = new Date();
            
            try {
                this.tokenExpirationDate = null;
                SimpleJwtParser parser = new SimpleJwtParser(token);
                this.tokenExpirationDate = new Date(Long.parseLong(parser.getExp()) * 1000);
            }
            catch (Exception e) {
                FrameworkImplProvider.handleException(e);
            }
            
            log.info("Now using token {}", token);
            log.info("Token will expire {}", tokenExpirationDate);
            
        } 
        finally {
            tokenRefreshLock.unlock();
        }
    }

    private boolean renewTokenIfNeeded() {

        tokenRefreshLock.lock();
        
        try {
            if (tokenMustBeRenewed()) {
                renewToken();
                return true;
            }
            else {
                return false;
            }
        } 
        finally {
            tokenRefreshLock.unlock();
        }
        
    }

    private boolean tokenMustBeRenewed() {
        
        log.trace("Checking if token must be renewed");
        if (token==null || tokenCreationDate==null) return true;
        Date now = new Date();
        
        if (tokenExpirationDate != null && now.after(tokenExpirationDate)) {
            // Token has already expired
            log.trace("Token is expired");
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns the current authenticated subject (user)
     * @return user
     */
    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
    }
    
    /**
     * Returns the actual user or group for the current session.
     * @return user or group
     */
    public Subject getActualSubject() {
        return actualSubject;
    }

    private void setActualSubject(Subject subject) {
        
        if (actualSubject!=null) {
            if (subject!=null && actualSubject.getKey().equals(subject.getKey())) {
                // Correct subject is already set, nothing needs to happen 
                return;
            }
            
            // End the current session
            Events.getInstance().postOnEventBus(new SessionEndEvent(actualSubject));
        }
        
        // Start a new session
        this.actualSubject = subject;
        if (actualSubject!=null) {
            this.readerSet = SubjectUtils.getReaderSet(actualSubject);
            this.writerSet = SubjectUtils.getWriterSet(actualSubject);
            Events.getInstance().postOnEventBus(new SessionStartEvent(actualSubject));
        }
        else {
            this.readerSet = new HashSet<>();
            this.writerSet = new HashSet<>();
        }
    }
    
    public static Subject getSubjectByNameOrKey(String key) {
        try {
            return DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(key);
        } 
        catch (Exception e) {
            log.error("Error getting subject with key: " + key, e);
        }
        return null;
    }

    public static boolean authenticatedSubjectIsInGroup(SubjectRole role) {
        Subject subject = AccessManager.getAccessManager().getAuthenticatedSubject();
        if (subject==null) return false;
        if (subject instanceof User) {
            User user = (User)subject;
            return user.hasGroupRead(role.getRole());
        }
        return false;
    }

    public static boolean actualSubjectIsInGroup(SubjectRole role) {
        Subject subject = getAccessManager().getActualSubject();
        if (subject==null) return false;
        if (subject instanceof User) {
            User user = (User)subject;
            return user.hasGroupRead(role.getRole());
        }
        return false;
    }
    
    public static Set<String> getReaderSet() {
        return getAccessManager().getActualReaderSet();
    }
    
    public static Set<String> getWriterSet() {
        return getAccessManager().getActualWriterSet();
    }

    /**
     * Bypass for testing without logging in an actual user.
     * @param subjectKey
     * @return
     */
    public static void setSubjectKey(String subjectKey) {
        bypassSubjectKey = subjectKey;
    }

    public static boolean loggedIn() {
        return getAccessManager().isLoggedIn();
    }
    
    public static String getSubjectKey() {
        if (bypassSubjectKey!=null) {
            return bypassSubjectKey;
        }
        Subject subject = getAccessManager().getActualSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getKey();
    }
    
    public static String getSubjectName() {
        Subject subject = getAccessManager().getActualSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getAccessManager().getActualSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        if (subject instanceof User) {
            User user = (User)subject;
            String email = user.getEmail();
            if (StringUtils.isBlank(email)) {
                email = user.getName()+"@janelia.hhmi.org";
            }
            return email;
        }
        return null;
    }
}
