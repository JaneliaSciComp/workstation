package org.janelia.workstation.core.api.services;

import org.janelia.workstation.integration.api.ActivityLogging;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.workstation.core.api.SessionMgr;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ActivityLogging.class, path=ActivityLogging.LOOKUP_PATH)
public class BrowserActivityLoggingService implements ActivityLogging {
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
