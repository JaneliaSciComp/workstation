package org.janelia.gltools.activity_logging;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.integration.api.ActivityLogging;

import org.janelia.workstation.integration.activity_logging.ActionString;

import org.janelia.workstation.integration.activity_logging.CategoryString;

import org.janelia.workstation.integration.activity_logging.ToolString;

public class ActivityLogHelper {

    public static final ToolString HORTA_TOOL_STRING = new ToolString("GLViewerTools");
    private static final CategoryString GLVIEWER_LOAD_BRICK = new CategoryString("loadBrick");
    
    private static final ActivityLogHelper instance = new ActivityLogHelper();

    public static ActivityLogHelper getInstance() {
        return instance;
    }

    private ActivityLogging activityLogging = FrameworkAccess.getActivityLogging();

    private ActivityLogHelper() {
    }

    public void logBrickLoadToRendered(long logId, String filename, boolean useHttp, final double elapsedMs) {
        final ActionString actionString = new ActionString(logId+":"+filename + ":http="+useHttp+":elapsed_ms=" + elapsedMs);
        activityLogging.logToolEvent(HORTA_TOOL_STRING, GLVIEWER_LOAD_BRICK, actionString);
    }
}
