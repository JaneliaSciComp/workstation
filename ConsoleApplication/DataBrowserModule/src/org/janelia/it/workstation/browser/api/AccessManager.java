package org.janelia.it.workstation.browser.api;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.exceptions.FatalCommError;
import org.janelia.it.workstation.browser.api.exceptions.SystemError;
import org.janelia.it.workstation.browser.api.facade.impl.ejb.EJBFactory;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.LoginEvent;
import org.janelia.it.workstation.browser.events.lifecycle.RunAsEvent;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.PropertyConfigurator;
import org.janelia.it.workstation.browser.util.SingleThreadedTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the data access credentials and current privileges.
 */
public final class AccessManager {
    
    private static final Logger log = LoggerFactory.getLogger(AccessManager.class);

    private static final int LOG_GRANULARITY = 100;

    public static String RUN_AS_USER = "RunAs";
    public static String USER_EMAIL = "UserEmail";
    public static String USER_NAME = "console.serverLogin";
    public static String USER_PASSWORD = "console.serverPassword";
    public static String REMEMBER_PASSWORD = "console.rememberPassword";

    private static AccessManager accessManager;
    private static String bypassSubjectKey;

    private boolean isLoggedIn;
    private Long currentSessionId;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;

    private final Map<CategoryString, Long> categoryInstanceCount = new HashMap<>();
    
    private AccessManager() {
        log.info("Initializing Access Manager");
        String tempLogin = (String) LocalPreferenceMgr.getInstance().getModelProperty(USER_NAME);
        String tempPassword = (String) LocalPreferenceMgr.getInstance().getModelProperty(USER_PASSWORD);
        if (tempLogin != null && tempPassword != null) {
            PropertyConfigurator.getProperties().setProperty(USER_NAME, tempLogin);
            PropertyConfigurator.getProperties().setProperty(USER_PASSWORD, tempPassword);
        }
    }

    static public AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = new AccessManager();
        }
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
                
                String email = ((User)authenticatedSubject).getEmail();
                if (email==null) {
                    // Take a guess
                    email = authenticatedSubject.getName()+"@janelia.hhmi.org";
                }
                ConsoleApp.getConsoleApp().setModelProperty(AccessManager.USER_EMAIL, email);
                
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

    private Subject authenticateSubject(final String username, final String password) throws Exception {
        // make RESTful call to authenticate user
        Subject authenticatedSubject = DomainMgr.getDomainMgr().getModel().loginSubject(username, password);
        if (authenticatedSubject!=null) {
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

    private void beginSession() {
        String userName = getSubject()==null?null:getSubject().getName();
        UserToolEvent loginEvent = EJBFactory.getRemoteComputeBean().beginSession(
                userName,
                ConsoleProperties.getString("console.Title"),
                ConsoleProperties.getString("console.versionNumber"));
        if (null!=loginEvent && null!=loginEvent.getSessionId()) {
            this.currentSessionId = loginEvent.getSessionId();
        }
        ActivityLogHelper.logSessionBegin();
        ActivityLogHelper.logUserInfo(authenticatedSubject);
    }

    private void endSession() {
        String userName = getSubject()==null?null:getSubject().getName();
        EJBFactory.getRemoteComputeBean().endSession(
                userName,
            ConsoleProperties.getString("console.Title"),
            currentSessionId);
        this.currentSessionId = null;
        ActivityLogHelper.logSessionEnd();
    }
    
    private void addEventToSession(UserToolEvent event) {
        EJBFactory.getRemoteComputeBean().addEventToSessionAsync(event);
    }

    private void addEventsToSession(UserToolEvent[] events) {
        EJBFactory.getRemoteComputeBean().addEventsToSessionAsync(events);
    }

    private String getLogEventUserLogin() {
        String username = PropertyConfigurator.getProperties().getProperty(USER_NAME);
        if (username!=null) return username;
        return UserToolEvent.DEFAULT_USER_LOGIN;
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
    public void logToolEvent(final ToolString toolName, final CategoryString category, final ActionString action, final long timestamp, final double elapsedMs, final double thresholdMs) {
        String userLogin = getLogEventUserLogin();
        try {
            final UserToolEvent event = new UserToolEvent(currentSessionId, userLogin, toolName.toString(), category.toString(), action.toString(), new Date(timestamp));
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
                        addEventToSession(event);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);

        } catch (Exception ex) {
            log.warn(
                    "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                    currentSessionId, userLogin, toolName, category, action, timestamp, ex);
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
    public void logToolThresholdEvent(final ToolString toolName, final CategoryString category, final ActionString action, final long timestamp, final double elapsedMs, final double thresholdMs) {
        String userLogin = getLogEventUserLogin();
        try {
            final UserToolEvent event = new UserToolEvent(currentSessionId, userLogin, toolName.toString(), category.toString(), action.toString(), new Date(timestamp));
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
                    boolean shouldLog = false;
                    if (elapsedMs > thresholdMs) {
                        shouldLog = true;
                    }

                    if (shouldLog) {
                        addEventToSession(event);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);

        } catch (Exception ex) {
            log.warn(
                    "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                    currentSessionId, userLogin, toolName, category, action, timestamp, ex);
        }
    }

    /**
     * Log a tool event, always.  No criteria will be checked. 
     */
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action) {
        // Force logging, by setting elapsed > threshold.
        logToolEvent(toolName, category, action, new Date().getTime(), 1.0, 0.0);
    }

    /**
     * Log a whole list of tool events, in one server-pump.
     * 
     * @param toolName tool, like LVV or Console
     * @param category type of event
     * @param batchPrefix distinguish action/optional, may be null.
     * @param actions explicit action information.
     */
    public void logBatchToolEvent(ToolString toolName, CategoryString category, String batchPrefix, List<String> actions) {
        String userLogin = getLogEventUserLogin();
        try {
            final UserToolEvent[] events = new UserToolEvent[actions.size()];
            int evtNum = 0;
            for (String action: actions) {                
                Date eventDate = null;
                int pos = action.lastIndexOf(":");
                if (pos > -1  &&  pos < action.length()) {
                    eventDate = new Date(Long.parseLong(action.substring(pos + 1)));
                    action = action.substring(0, pos); // Trim away redundant info.
                }
                else {
                    eventDate = new Date();
                }
                if (batchPrefix != null)
                    action = batchPrefix + ":" + action;
                UserToolEvent event = new UserToolEvent(currentSessionId, userLogin, toolName.toString(), category.toString(), action, eventDate);
                events[evtNum++] = event;
            }
            
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
                    addEventsToSession(events);
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);
        } catch (Exception ex) {
            log.warn(
                    "Failed to batch-log tool events for session: {}, user: {}, tool: {}, category: {}, action-prefix: {}, timestamp: {}.",
                    currentSessionId, userLogin, toolName, category, batchPrefix, new Date().getTime(), ex);
        }
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
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        logToolEvent(toolName, category, action, new Date().getTime(), elapsedMs, thresholdMs);
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
            ConsoleApp.handleException(e);
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
        if (subject==null) return false;
        if (subject instanceof User) {
            User user = (User)subject;
            return user.hasGroupRead(role.getRole());
        }
        return false;
    }

    public static boolean currentUserIsInGroup(SubjectRole role) {
        Subject subject = AccessManager.getAccessManager().getSubject();
        if (subject==null) return false;
        if (subject instanceof User) {
            User user = (User)subject;
            return user.hasGroupRead(role.getRole());
        }
        return false;
    }

    public static Set<String> getReaderSet() {
        Subject subject = AccessManager.getAccessManager().getSubject();
        Set<String> set = new HashSet<>();
        set.add(subject.getKey());
        if (subject instanceof User) {
            User user = (User)subject;
            set.addAll(user.getReadGroups());
        }
        return set;
    }
    
    public static Set<String> getWriterSet() {
        Subject subject = AccessManager.getAccessManager().getSubject();
        Set<String> set = new HashSet<>();
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

    public static String getSubjectKey() {
        if (bypassSubjectKey!=null) {
            return bypassSubjectKey;
        }
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
        if (subject instanceof User) {
            User user = (User)subject;
            return user.getEmail();
        }
        return null;
    }
}
