package org.janelia.workstation.core.api;

import org.janelia.workstation.integration.util.FrameworkAccess;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.api.exceptions.AuthenticationException;
import org.janelia.workstation.core.api.exceptions.ServiceException;
import org.janelia.workstation.core.api.exceptions.SystemError;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.LoginEvent;
import org.janelia.workstation.core.events.lifecycle.SessionEndEvent;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.model.LoginErrorType;
import org.janelia.workstation.core.util.SimpleJwtParser;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.model.domain.enums.SubjectRole;
import org.janelia.model.security.AppAuthorization;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.util.SubjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final Logger LOG = LoggerFactory.getLogger(AccessManager.class);

    public static final String RUN_AS_USER = "RunAs";
    public static final String USER_NAME = "console.serverLogin";
    public static final String USER_PASSWORD = "console.serverPassword";
    public static final String REMEMBER_PASSWORD = "console.rememberPassword";

    // TODO: technician groups should be modeled explicitly in the Subject model via a Group attribute
    public static final String FLYLIGHT_GROUP = "group:flylighttechnical";
    public static final String PTR_GROUP = "group:projtechres";
    private boolean localConnection;

    private enum AuthState {
        Starting,
        LoggedIn,
        LoggedOut
    }
    
    // Start up state
    private AuthState currState = AuthState.Starting;
    private boolean hadLoginIssue;
    private LoginErrorType loginIssue;
    
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
    private Set<String> adminSet;

    // Singleton
    private static AccessManager accessManager;
    static public AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = new AccessManager();
        }
        return accessManager;
    }
    
    private AccessManager() {
        LOG.info("Initializing Access Manager");
        moveToStartingState();
    }
    
    private void moveToStartingState() {
        LOG.info("Moving to starting state");
        this.currState = AuthState.Starting;
        this.token = null;
        this.authenticatedSubject = null;
        this.actualSubject = null;
        localConnection = false;
    }
    
    private void moveToLoggedInState(Subject authenticatedSubject) {
        LOG.info("Moving to logged in state");
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
                FrameworkAccess.handleException(e);
            }
        }
        
        if (!runningAs) {
            // Session was not started with a run-as user
            setActualSubject(authenticatedSubject);
        }
    }
    
    private void moveToLoggedOutState() {
        LOG.info("Moving to logged out state");
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
                } else {
                    moveToLoggedOutState();
                }
            } catch (AuthenticationException e) {
                LOG.warn("Authentication problem during auto-login", e);
                moveToLoggedOutState();
                loginIssue = LoginErrorType.AuthError;
            } catch (ServiceException e) {
                FrameworkAccess.handleExceptionQuietly("Problem encountered during auto-login", e);
                moveToLoggedOutState();
                loginIssue = LoginErrorType.NetworkError;
            } catch (Throwable t) {
                FrameworkAccess.handleExceptionQuietly("Problem encountered during auto-login", t);
                moveToLoggedOutState();
                loginIssue = LoginErrorType.OtherError;
            }
        }
    }

    public boolean hadLoginIssue() {
        return hadLoginIssue;
    }

    public LoginErrorType getLoginIssue() {
        return loginIssue;
    }

    /**
     * Can be called to change the current "actual subject". 
     * @param runAsUser user or group (key or name)
     * @return true if the user was successfully made actual
     */
    public boolean setRunAsUser(String runAsUser) {
        if (!isAdmin() && !StringUtils.isEmpty(runAsUser)) {
            LOG.error("Non-admin user cannot run as another user");
            setActualSubject(authenticatedSubject);
            return false;
        }
        try {
            if (!StringUtils.isEmpty(runAsUser)) {
                Subject runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(runAsUser);
                if (runAsSubject == null) {
                    return false;
                }
                setActualSubject(runAsSubject);
            } else {
                setActualSubject(authenticatedSubject);
            }
            LOG.info("Running as {}", getActualSubject().getKey());
            return true;
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            setActualSubject(authenticatedSubject);
            return false;
        }
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

    public void setLocal(boolean local) {
        localConnection = local;
    }
        
    private Subject authenticateSubject() {
        // First get auth token
        if (!localConnection)
            renewToken();
        // We're now authenticated. Get or create the Workstation user object.
        try {
            return DomainMgr.getDomainMgr().getModel().getUser(username);
        }
        catch (Exception e) {
            throw new ServiceException("Error getting or creating user "+username, e);
        }
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

    /**
     * Checks to see if the current user is a writer in one of the technican groups.
     * @return true if writer is a technician
     */
    public boolean isTechnician() {
        return writerSet.contains(FLYLIGHT_GROUP) || writerSet.contains(PTR_GROUP);
    }

    public Set<String> getActualReaderSet() {
        return readerSet;
    }
    public Set<String> getActualWriterSet() {
        return writerSet;
    }
    public Set<String> getActualAdminSet() {
        return adminSet;
    }

    /**
     * Can the current user create data sets on behalf of others?
     * @return
     */
    public boolean isDataSetCreator() {
        return writerSet.contains("group:admin")
                || writerSet.contains("group:flylighttechnical")
                || writerSet.contains("group:projtechres");
    }

    /**
     * Returns the current authentication token.
     * @return JWS token 
     */
    public String getToken() {
        renewTokenIfNeeded();
        LOG.trace("Returning token: {}", token);
        return token;
    }

    private void renewToken() {
        tokenRefreshLock.lock();
        try {
            LOG.debug("Attempting to obtain new auth token for {}", username);
            this.token = DomainMgr.getDomainMgr().getAuthClient().obtainToken(username, password);
            this.tokenCreationDate = new Date();

            try {
                this.tokenExpirationDate = null;
                SimpleJwtParser parser = new SimpleJwtParser(token);
                this.tokenExpirationDate = new Date(Long.parseLong(parser.getExp()) * 1000);
            } catch (Exception e) {
                FrameworkAccess.handleException(e);
            }

            LOG.info("Now using token {} with expiration date: {}", token, tokenExpirationDate);
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    private boolean renewTokenIfNeeded() {
        tokenRefreshLock.lock();
        try {
            if (tokenMustBeRenewed()) {
                renewToken();
                return true;
            } else {
                return false;
            }
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    private boolean tokenMustBeRenewed() {
        LOG.trace("Checking if token must be renewed");
        if (token==null || tokenCreationDate==null) return true;
        Date now = new Date();
        
        if (tokenExpirationDate != null && now.after(tokenExpirationDate)) {
            // Token has already expired
            LOG.trace("Token is expired");
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
        if (actualSubject != null) {
            this.readerSet = SubjectUtils.getReaderSet(actualSubject);
            this.writerSet = SubjectUtils.getWriterSet(actualSubject);
            this.adminSet = SubjectUtils.getAdminSet(actualSubject);
            Events.getInstance().postOnEventBus(new SessionStartEvent(actualSubject));
        }
        else {
            this.readerSet = new HashSet<>();
            this.writerSet = new HashSet<>();
            this.adminSet = new HashSet<>();
        }
    }

    public AppAuthorization getAppAuthorization() {
        return new AppAuthorization(getAuthenticatedSubject(),
                getToken(),
                getActualSubject());
    }

    public static Subject getSubjectByNameOrKey(String key) {
        try {
            return DomainMgr.getDomainMgr().getModel().getSubjectByNameOrKey(key);
        } catch (Exception e) {
            LOG.error("Error getting subject with key: " + key, e);
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

    public static Set<String> getAdminSet() {
        return getAccessManager().getActualAdminSet();
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
