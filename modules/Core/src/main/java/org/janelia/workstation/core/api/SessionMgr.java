package org.janelia.workstation.core.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.user_data.UserToolEvent;

import org.janelia.workstation.integration.activity_logging.ActionString;

import org.janelia.workstation.integration.activity_logging.CategoryString;

import org.janelia.workstation.integration.activity_logging.ToolString;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionEndEvent;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.util.SingleThreadedTaskQueue;
import org.janelia.model.security.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton manager for the user session.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionMgr {

    private static final Logger log = LoggerFactory.getLogger(SessionMgr.class);

    private static final int LOG_GRANULARITY = 100;
    private final Map<CategoryString, Long> categoryInstanceCount = new HashMap<>();
    
    private String username;
    
    // Singleton
    private static SessionMgr sessionMgr;
    static public SessionMgr getSessionMgr() {
        if (sessionMgr == null) {
            sessionMgr = new SessionMgr();
            Events.getInstance().registerOnEventBus(sessionMgr);
        }
        return sessionMgr;
    }

    private Long currentSessionId;
    
    private SessionMgr() {
        log.info("Initializing Session Manager");
    }

    @Subscribe
    public void beginSession(SessionStartEvent event) {
        Subject loggedInSubject = event.getSubject();
        String userName = loggedInSubject==null?null:loggedInSubject.getName();
        this.username = userName;
//        try {
//            UserToolEvent loginEvent = EJBFactory.getRemoteComputeBean().beginSession(
//                    userName,
//                    ConsoleProperties.getString("client.Title"),
//                    ConsoleProperties.getString("client.versionNumber"));
//            if (null!=loginEvent && null!=loginEvent.getSessionId()) {
//                this.currentSessionId = loginEvent.getSessionId();
//            }
//            ActivityLogHelper.logSessionBegin();
//        }
//        catch (Exception e) {
//            FrameworkAccess.handleExceptionQuietly(e);
//        }
    }

    @Subscribe
    public void endSession(SessionEndEvent event) {
        Subject subject = event.getSubject();
        String userName = subject==null?null:subject.getName();
        this.username = null;
//        try {
//            if (currentSessionId!=null) {
//                EJBFactory.getRemoteComputeBean().endSession(
//                        userName,
//                    ConsoleProperties.getString("client.Title"),
//                    currentSessionId);
//                this.currentSessionId = null;
//            }
//            ActivityLogHelper.logSessionEnd();
//        }
//        catch (Exception e) {
//            FrameworkAccess.handleExceptionQuietly(e);
//        }
        this.currentSessionId = null;
    }
    
    private void addEventToSession(UserToolEvent event) {
//        EJBFactory.getRemoteComputeBean().addEventToSessionAsync(event);
    }

    private void addEventsToSession(UserToolEvent[] events) {
//        EJBFactory.getRemoteComputeBean().addEventsToSessionAsync(events);
    }

    private String getLogEventUserLogin() {
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
                    try {
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
                    } 
                    catch (Exception ex) {
                        log.warn(
                                "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                                currentSessionId, userLogin, toolName, category, action, timestamp, ex);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);
    
        } 
        catch (Exception ex) {
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
                    try {
                        boolean shouldLog = false;
                        if (elapsedMs > thresholdMs) {
                            shouldLog = true;
                        }
    
                        if (shouldLog) {
                            addEventToSession(event);
                        }
                    } 
                    catch (Exception ex) {
                        log.warn(
                                "Failed to log tool event for session: {}, user: {}, tool: {}, category: {}, action: {}, timestamp: {}.",
                                currentSessionId, userLogin, toolName, category, action, timestamp, ex);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);

        } 
        catch (Exception ex) {
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
                    try {
                        addEventsToSession(events);
                    }
                    catch (Exception ex) {
                        log.warn(
                                "Failed to batch-log tool events for session: {}, user: {}, tool: {}, category: {}, action-prefix: {}, timestamp: {}.",
                                currentSessionId, userLogin, toolName, category, batchPrefix, new Date().getTime(), ex);
                    }
                    return null;
                }
            };
            SingleThreadedTaskQueue.submit(callable);
        }
        catch (Exception ex) {
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
     * @param elapsedMs
     * @param thresholdMs 
     */
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        logToolEvent(toolName, category, action, new Date().getTime(), elapsedMs, thresholdMs);
    }
}
