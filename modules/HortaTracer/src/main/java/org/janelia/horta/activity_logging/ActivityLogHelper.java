package org.janelia.horta.activity_logging;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.integration.api.ActivityLogging;

import org.janelia.workstation.integration.activity_logging.ActionString;

import org.janelia.workstation.integration.activity_logging.CategoryString;

import org.janelia.workstation.integration.activity_logging.ToolString;

/**
 * Centralized logging: any hooks kept in Horta should be here.
 * NOTE: much of this code may be / has been abandoned in coming weeks,
 * because it is more tractable to do some of this logging from the
 * Neuron Model Adapter.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    public static final ToolString HORTA_TOOL_STRING = new ToolString("Horta");
    private static final String BOTH_COORDS_FMT = "%d:%d:%5.3f,%5.3f,%5.3f:%5.3f,%5.3f,%5.3f";
    
    private static final ActivityLogHelper instance = new ActivityLogHelper();

    // These category strings are used similarly.  Lining them up spatially
    // makes it easier to see that they are all different.
    private static final CategoryString HORTA_LAUNCH_CATEGORY_STRING              = new CategoryString("launchHorta");
    private static final CategoryString HORTA_LOAD_BRICK_CATEGORY_STRING          = new CategoryString("loadBrick");
    private static final CategoryString HORTA_ADD_ANCHOR_CATEGORY_STRING          = new CategoryString("addAnchor:xyzsw");
    private static final CategoryString HORTA_MERGE_NEURITES_CATEGORY_STRING      = new CategoryString("mergeNeurites:xyzsw");
    private static final CategoryString HORTA_MOVE_NEURITE_CATEGORY_STRING        = new CategoryString("moveNeurite:xyzsw");
    private static final CategoryString HORTA_SPLIT_NEURITE_CATEGORY_STRING       = new CategoryString("splitNeurite:xyzsw");
    private static final CategoryString HORTA_SPLIT_ANNO_CATEGORY_STRING          = new CategoryString("splitAnnotation:xyzsw");
    private static final CategoryString HORTA_DELETE_LINK_CATEGORY_STRING         = new CategoryString("deleteLink:xyzsw");
    private static final CategoryString HORTA_DELETE_SUBTREE_CATEGORY_STRING      = new CategoryString("deleteSubTree:xyzsw");
    private static final CategoryString HORTA_REROOT_NEURITE_CATEGORY_STRING      = new CategoryString("rerootNeurite");

    private ActivityLogging activityLogging;
    
    public static ActivityLogHelper getInstance() {
        return instance;
    }
    
    private ActivityLogHelper() {
        activityLogging = FrameworkAccess.getActivityLogging();
    }
}
