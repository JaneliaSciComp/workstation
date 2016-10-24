package org.janelia.it.workstation.browser.api.services;

import org.janelia.it.jacs.integration.framework.system.ActivityLogging;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ActivityLogging.class, path=ActivityLogging.LOOKUP_PATH)
public class BrowserActivityLoggingService implements ActivityLogging {
    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        AccessManager.getAccessManager().logToolEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action) {
        AccessManager.getAccessManager().logToolEvent(toolName, category, action);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        AccessManager.getAccessManager().logToolEvent(toolName, category, action, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolThresholdEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        AccessManager.getAccessManager().logToolThresholdEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

}
