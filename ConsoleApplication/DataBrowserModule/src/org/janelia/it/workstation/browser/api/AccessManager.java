package org.janelia.it.workstation.browser.api;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.exceptions.FatalCommError;
import org.janelia.it.workstation.browser.api.exceptions.SystemError;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.LoginEvent;
import org.janelia.it.workstation.browser.events.lifecycle.SessionEndEvent;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;
import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog;
import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog.ErrorType;
import org.janelia.model.domain.enums.SubjectRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.openide.util.RequestProcessor;
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
 * Logged in -> Expiring (after some time, this state is automatically entered so that the token can be refreshed)
 * Expiring -> Logged in (if the token is successfully refreshed)
 * Expiring -> Logged out (if the token cannot be refreshed after TOKEN_REFRESH_MAX_FAILURES tries)
 * 
 */
public final class AccessManager {
    
    private static final Logger log = LoggerFactory.getLogger(AccessManager.class);
    private static final RequestProcessor RP = new RequestProcessor(AccessManager.class);
    
    // Refresh the token after it's an hour old. Tokens probably last much longer than this, but let's be safe.
    private static final int TOKEN_BEGIN_REFRESH_AFTER_SECS = 60 * 60;
    
    // If a token refresh fails, wait a minute and then try again.
    private static final int TOKEN_FAILURE_WAIT_SECS = 60;
    
    // Try for an hour, and if we still can't get a token, fail hard.
    private static final int TOKEN_REFRESH_MAX_FAILURES = 60; 
    
    public static String RUN_AS_USER = "RunAs";
    public static String USER_NAME = "console.serverLogin";
    public static String USER_PASSWORD = "console.serverPassword";
    public static String REMEMBER_PASSWORD = "console.rememberPassword";

    private enum AuthState {
        Starting,
        LoggedIn,
        LoggedOut,
        Expiring
    }
    
    // Start up state
    private AuthState currState = AuthState.Starting;
    private boolean hadLoginIssue;
    private ErrorType loginIssue;
    
    // Running state
    private int tokenRefreshFailures = 0;
    private String token;
    private Subject authenticatedSubject; 
    private Subject actualSubject; 
    private static String bypassSubjectKey;

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
        
        ActivityLogHelper.logUserInfo(authenticatedSubject);
        Events.getInstance().postOnEventBus(new LoginEvent(authenticatedSubject));
        
        // Is there a run-as user? 
        boolean sessionStarted = false;
        LocalPreferenceMgr prefs = LocalPreferenceMgr.getInstance();
        String runAsUser = (String)prefs.getModelProperty(AccessManager.RUN_AS_USER);
        if (runAsUser!=null) {
            // If so, override the actual subject.
            try {
                if (setRunAsUser(runAsUser)) {
                    sessionStarted = true;
                }
            }
            catch (Exception e) {
                prefs.setModelProperty(AccessManager.RUN_AS_USER, "");
                ConsoleApp.handleException(e);
            }
        }
        
        if (!sessionStarted) {
            // Session was not started with a run-as user
            Events.getInstance().postOnEventBus(new SessionStartEvent(actualSubject));
        }
    }
    
    private void moveToExpiringState() {
        log.info("Moving to expiring state");
        this.currState = AuthState.Expiring;
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
            catch (FatalCommError e) {
                moveToLoggedOutState();
                loginIssue = ErrorType.NetworkError;
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
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.showDialog(this::loginUser, loginIssue);
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
                Subject runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByKey(fullUserKey);
                if (runAsSubject==null) {
                    // try group before failing
                    fullUserKey = "group:" + runAsUser;
                    runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByKey(fullUserKey);
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
        LoginDialog loginDialog = new LoginDialog();
        loginDialog.showDialog(this::loginUser);
    }
    
    private boolean loginUser(String username, String password) {
        try {
            if (isLoggedIn()) {
                // Log out current subject first
                moveToLoggedOutState();
            }

            // Authenticate
            Subject authenticatedSubject = authenticateSubject(username, password);
            
            if (authenticatedSubject != null) {
                moveToLoggedInState(authenticatedSubject);
                return true;
            }
            else {
                moveToLoggedOutState();
                return false;
            }
        }
        catch (Exception e) {
            currState = AuthState.LoggedOut;
            log.error("Error logging in", e);
            throw new FatalCommError(ConsoleApp.getConsoleApp().getRemoteRestUrl(),
                    "Cannot authenticate login. The server may be down. Please try again later.");
        }
    }
        
    private Subject authenticateSubject(final String username, final String password) throws Exception {
        // First get auth token
        if (!obtainToken(username, password, false)) {
            return null;
        }
        
        // make RESTful call to authenticate user
        Subject authenticatedSubject = DomainMgr.getDomainMgr().getModel().loginSubject(username, password);

        // Legacy JFS/Webdav needs basic auth
        if (authenticatedSubject != null) {
            log.info("Authenticated as {}", authenticatedSubject.getKey());
            
            log.debug("Setting default authenticator");
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
            FileMgr.getFileMgr().getWebDavClient().setCredentialsUsingAuthenticator();
        }
        
        return authenticatedSubject;
    }
    
    private synchronized boolean obtainToken(String username, String password, boolean retryErrors) {

        log.info("Attempting to obtain new auth token for {}", username);
        String newToken = DomainMgr.getDomainMgr().getAuthClient().obtainToken(username, password);
        
        if (newToken==null) {
            if (retryErrors) {
                if (++tokenRefreshFailures < TOKEN_REFRESH_MAX_FAILURES) {
                    // Retry in a minute
                    log.info("Will retry auth token in a minute...");
                    scheduleTokenUpdate(username, password, TOKEN_FAILURE_WAIT_SECS);
                }
                else {
                    log.info("Reached max token retries ({}). Moving to logged out state, and showing login dialog.", TOKEN_REFRESH_MAX_FAILURES);
                    // Expired token cannot be refreshed
                    moveToLoggedOutState();
                    // Show login dialog to allow user to update password
                    LoginDialog loginDialog = new LoginDialog();
                    loginDialog.showDialog(this::loginUser, ErrorType.TokenExpiredError);
                }
            }
            else {
                log.error("Got null auth token");
            }
            return false;
        }
        else {
            this.tokenRefreshFailures = 0;
            this.token = newToken;
            log.info("Now using token: {}", token);        
            // Schedule the next refresh
            scheduleTokenUpdate(username, password, TOKEN_BEGIN_REFRESH_AFTER_SECS);
            return true;
        }
    }
    
    private synchronized void scheduleTokenUpdate(String username, String password, int secs) {
        RP.post(() -> {
            moveToExpiringState();
            obtainToken(username, password, true);
        }, secs*1000);
    }

    /**
     * Checks to see if the system is in a logged in state. 
     * @return true if a user is authenticated and logged in
     */
    public boolean isLoggedIn() {
        return currState==AuthState.LoggedIn || currState==AuthState.Expiring;
    }

    /**
     * Checks to see if the current user is in the admin group.
     * @return true if current user is admin
     */
    public boolean isAdmin() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }

    /**
     * Returns the current authentication token.
     * @return JWS token 
     */
    public String getToken() {
        return token;
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
            Events.getInstance().postOnEventBus(new SessionStartEvent(actualSubject));
        }
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
        Subject subject = getAccessManager().getActualSubject();
        Set<String> set = new HashSet<>();
        if (subject==null) return set;
        set.add(subject.getKey());
        if (subject instanceof User) {
            User user = (User)subject;
            set.addAll(user.getReadGroups());
        }
        return set;
    }
    
    public static Set<String> getWriterSet() {
        Subject subject = getAccessManager().getActualSubject();
        Set<String> set = new HashSet<>();
        if (subject==null) return set;
        set.add(subject.getKey());
        if (subject instanceof User) {
            User user = (User)subject;
            set.addAll(user.getWriteGroups());
        }
        return set;
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

    public static String getUsername() {
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
