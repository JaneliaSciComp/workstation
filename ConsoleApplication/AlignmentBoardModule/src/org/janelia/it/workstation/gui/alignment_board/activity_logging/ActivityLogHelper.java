/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board.activity_logging;

import java.util.Collection;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;

/**
 * Helps capture events generated at the Alignment Board.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    private static final ToolString TOOL_STRING = new ToolString("AlignmentBoard");

    private static final CategoryString OPEN_CATEGORY = new CategoryString("openAB");
    private static final CategoryString SUBVOL_SELECT_CATEGORY = new CategoryString("subvolumeSelect");
    private static final CategoryString TOGGLE_INTENSITY_CATEGORY = new CategoryString("toggleSignal");
    private static final ActivityLogging ACTIVITY_LOGGING = FrameworkImplProvider.getSessionSupport();

    public void logOpen(String boardName) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, OPEN_CATEGORY, new ActionString(boardName));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public void logSubVolSelect(String boardName, Collection<float[]> acceptedCords) {
        try {
            StringBuilder acceptedCoordsValue = new StringBuilder();
            for (float[] f: acceptedCords) {
                if (acceptedCoordsValue.length() > 0) {
                    acceptedCoordsValue.append(";");
                }
                for (float c: f) {
                    acceptedCoordsValue.append(c);
                    acceptedCoordsValue.append(" ");
                }
            }
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, SUBVOL_SELECT_CATEGORY, new ActionString((boardName == null ? "" : boardName)  + ":" + acceptedCoordsValue.toString()));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
    public void logToggleIntensity(String boardName, boolean value) {
        try {
            ACTIVITY_LOGGING.logToolEvent(TOOL_STRING, TOGGLE_INTENSITY_CATEGORY, new ActionString((boardName == null ? "" : boardName) + ":" + value));
        } catch (Throwable th) {
            // Eat all exceptions.  Avoid harming caller.
            th.printStackTrace();
        }
    }
    
}
