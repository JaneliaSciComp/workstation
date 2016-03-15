/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

/**
 * Wrap-around support for activity logging, via session manager.
 * @author fosterl
 */
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ActivityLogging.class, path=ActivityLogging.LOOKUP_PATH)
public class SessionMgrActivityLogging implements ActivityLogging {
    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolThresholdEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolThresholdEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

}
