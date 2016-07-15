/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.browser.activity_logging;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.shared.util.SystemInfo;

/**
 * Helper for any and all activity logging, to round up this cross-cutting
 * concern in one tight area.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    private static final char ACTION_PART_DELIMITER = '/';
    private static final ToolString TOOL_STRING = new ToolString("DomainBrowserCore");
    
    private static final CategoryString SESS_CTG = new CategoryString("Session");
    private static final ActionString LOGIN_ACTION = new ActionString("Login");
    private static final ActionString LOGOUT_ACTION = new ActionString("Logout");

    private static final CategoryString USER_INFO_CTG = new CategoryString("UserInfo");

    private static final ActivityLogging ACTIVITY_LOGGING = FrameworkImplProvider.getSessionSupport();
    
    public void logSessionBegin() {
        ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SESS_CTG, LOGIN_ACTION);
    }
    
    public void logSessionEnd() {
        ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SESS_CTG, LOGOUT_ACTION);        
    }
    
    public void logUserInfo(Subject authenticatedSubject) {
        StringBuilder actionBuilder = new StringBuilder();
        String javaInfo = SystemInfo.getJavaInfo();
        String runtimeJavaInfo = SystemInfo.getRuntimeJavaInfo();
        String osInfo = SystemInfo.getOSInfo();
        int ramAllocatedInfo = -1; 
        try {
            ramAllocatedInfo = SystemInfo.getMemoryAllocation();
        } catch (Exception ex) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ActivityLogHelper.class.getSimpleName());
            logger.info("Failed to get the allocated ram value.");
            ex.printStackTrace();
        }
        Long ramTotalInfo = SystemInfo.getTotalSystemMemory();
        
        // user/totalRAM/allocatedRAM/JavaInfo/RuntimeJavaInfo/OSInfo
        actionBuilder.append(authenticatedSubject.getName())
                     .append(ACTION_PART_DELIMITER)
                     .append(ramTotalInfo)
                     .append(ACTION_PART_DELIMITER)
                     .append(ramAllocatedInfo)
                     .append(ACTION_PART_DELIMITER)
                     .append(javaInfo)
                     .append(ACTION_PART_DELIMITER)
                     .append(runtimeJavaInfo)
                     .append(ACTION_PART_DELIMITER)
                     .append(osInfo);
        ActionString userInfoAction = new ActionString(actionBuilder.toString());
        
        ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, USER_INFO_CTG, userInfoAction);
    }
}
