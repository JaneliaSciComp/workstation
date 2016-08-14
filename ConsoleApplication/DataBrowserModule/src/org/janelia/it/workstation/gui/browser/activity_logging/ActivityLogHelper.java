package org.janelia.it.workstation.gui.browser.activity_logging;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.impl.rest.RESTClientManager;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for any and all activity logging, to round up this cross-cutting
 * concern in one tight area.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ActivityLogHelper {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private static final String ACTION_PART_DELIMITER = "\t";
    private static final ToolString TOOL_STRING = new ToolString("DomainBrowserCore");
    private static final CategoryString SESS_CTG = new CategoryString("Session");
    private static final CategoryString USER_INFO_CTG = new CategoryString("UserInfo");
    private static final ActionString LOGIN_ACTION = new ActionString("Login");
    private static final ActionString LOGOUT_ACTION = new ActionString("Logout");
    private static final CategoryString USER_ACTION_CTG = new CategoryString("UserAction");

    private static final ActivityLogging activityLogging = FrameworkImplProvider.getSessionSupport();
    
    public static void logSessionBegin() {
        activityLogging.logToolEvent(TOOL_STRING, SESS_CTG, LOGIN_ACTION);
    }

    public static void logSessionEnd() {
        activityLogging.logToolEvent(TOOL_STRING, SESS_CTG, LOGOUT_ACTION);
    }

    public static void logUserInfo(Subject authenticatedSubject) {
        String javaInfo = SystemInfo.getJavaInfo();
        String runtimeJavaInfo = SystemInfo.getRuntimeJavaInfo();
        String osInfo = SystemInfo.getOSInfo();
        int ramAllocatedInfo = -1; 
        try {
            ramAllocatedInfo = SystemInfo.getMemoryAllocation();
        } catch (Exception ex) {
            log.info("Failed to get the allocated ram value",ex);
        }
        Long ramTotalInfo = SystemInfo.getTotalSystemMemory();
        // user/totalRAM/allocatedRAM/JavaInfo/RuntimeJavaInfo/OSInfo
        activityLogging.logToolEvent(TOOL_STRING, USER_INFO_CTG, buildAction(authenticatedSubject.getName(), ramTotalInfo, ramAllocatedInfo, javaInfo, runtimeJavaInfo, osInfo));
    }

    public static void logUserAction(String action) {
        logElapsed(action, null, null);
    }

    public static void logUserAction(String action, Object parameter) {
        logElapsed(action, parameter, null);
    }

    public static void logElapsed(String action, StopWatch watch) {
        logElapsed(action, null, watch);
    }

    public static void logElapsed(String action, Object parameter, StopWatch watch) {
        String subjectName = AccessManager.getAccessManager().getSubject().getName();
        ActionString actionString;
        if (watch==null) {
            actionString = buildAction(subjectName, action, parameter);
        }
        else {
            actionString = buildAction(subjectName, action, parameter, "elapsed:"+watch.getElapsedTime());
        }
        activityLogging.logToolEvent(TOOL_STRING, USER_ACTION_CTG, actionString);
    }

    private static ActionString buildAction(Object... params) {
        StringBuilder sb = new StringBuilder();
        for(Object param : params) {
            if (param == null) continue;
            if (sb.length()>0) sb.append(ACTION_PART_DELIMITER);
            if (param instanceof DomainObject) {
                DomainObject domainObject = (DomainObject)param;
                sb.append(domainObject.getType()).append("#").append(domainObject.getId());
            }
            else if (param instanceof HasIdentifier) {
                HasIdentifier hasIdentifier = (HasIdentifier)param;
                sb.append(hasIdentifier.getClass().getSimpleName()).append("#").append(hasIdentifier.getId());
            }
            else {
                sb.append(param);
            }
        }
        return new ActionString(sb.toString());
    }

}
