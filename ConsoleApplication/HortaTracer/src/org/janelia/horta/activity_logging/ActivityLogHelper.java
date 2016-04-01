package org.janelia.horta.activity_logging;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.session_mgr.ActivityLogging;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;

/**
 * Centralized logging: any hooks kept in Horta should be here.
 * NOTE: much of this code may be / has been abandoned in coming weeks,
 * because it is more tractable to do some of this logging from the
 * Neuron Model Adapter.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    public static final ToolString HORTA_TOOL_STRING = new ToolString("HORTA");
    private static final String BOTH_COORDS_FMT = "%d:%d:%5.3f,%5.3f,%5.3f:%5.3f,%5.3f,%5.3f";
    
    private static final ActivityLogHelper instance = new ActivityLogHelper();

    // These category strings are used similarly.  Lining them up spatially
    // makes it easier to see that they are all different.
    private static final CategoryString HORTA_LAUNCH_CATEGORY_STRING              = new CategoryString("launchHorta");
    private static final CategoryString HORTA_ADD_ANCHOR_CATEGORY_STRING          = new CategoryString("addAnchor:xyzsw");
    private static final CategoryString HORTA_MERGE_NEURITES_CATEGORY_STRING      = new CategoryString("mergeNeurites:xyzsw");
    private static final CategoryString HORTA_MOVE_NEURITE_CATEGORY_STRING        = new CategoryString("moveNeurite:xyzsw");
    private static final CategoryString HORTA_SPLIT_NEURITE_CATEGORY_STRING       = new CategoryString("splitNeurite:xyzsw");
    private static final CategoryString HORTA_SPLIT_ANNO_CATEGORY_STRING          = new CategoryString("splitAnnotation:xyzsw");
    private static final CategoryString HORTA_DELETE_LINK_CATEGORY_STRING         = new CategoryString("deleteLink:xyzsw");
    private static final CategoryString HORTA_DELETE_SUBTREE_CATEGORY_STRING      = new CategoryString("deleteSubTree:xyzsw");
    private static final CategoryString HORTA_REROOT_NEURITE_CATEGORY_STRING      = new CategoryString("rerootNeurite");
    
    private ActivityLogging activityLogging;
    private SampleLocation sampleLocation;
    
    public static ActivityLogHelper getInstance() {
        return instance;
    }
    
    private ActivityLogHelper() {
        activityLogging = FrameworkImplProvider.getSessionSupport();
    }
    
    public void logHortaLaunch(SampleLocation sampleLocation) {
        this.sampleLocation = sampleLocation;
        FrameworkImplProvider.getSessionSupport().logToolEvent(HORTA_TOOL_STRING, HORTA_LAUNCH_CATEGORY_STRING, new ActionString(sampleLocation.getSampleUrl().toString()));        
    }

    public void logAddAnchor(float[] location) {
        logGeometricEvent(
                new Double(location[0]), new Double(location[1]), new Double(location[2]), 
                HORTA_ADD_ANCHOR_CATEGORY_STRING);
        
    }
    public void logAddAnchor(double[] location) {        
        logGeometricEvent(
                location[0], location[1], location[2], 
                HORTA_ADD_ANCHOR_CATEGORY_STRING);
    }
    
    public void logRerootNeurite(Long sampleID, Long workspaceID, Long neuronID) {
        activityLogging.logToolEvent(
                HORTA_TOOL_STRING, 
                HORTA_REROOT_NEURITE_CATEGORY_STRING, 
                new ActionString(sampleID + ":" + workspaceID + ":" + neuronID)
        );
    }
    
    public void logMergedNeurite(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_MERGE_NEURITES_CATEGORY_STRING);
    }
    
    public void logMovedNeurite(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_MOVE_NEURITE_CATEGORY_STRING);
    }
    
    public void logSplitNeurite(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_SPLIT_NEURITE_CATEGORY_STRING);
    }

    public void logSplitAnnotation(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_SPLIT_ANNO_CATEGORY_STRING);
    }
    
    public void logDeleteLink(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_DELETE_LINK_CATEGORY_STRING);
    }
    
    public void logDeleteSubTree(NeuronVertex source) {
        this.logGeometricEvent(source, HORTA_DELETE_SUBTREE_CATEGORY_STRING);        
    }
    
    /**
     * Method to ensure consistent formatting/non-redundant code.
     *
     * @param sampleID sample upon which the workspace is predicated.
     * @param workspaceID where user is working.
     * @param source bears location.
     * @param category discriminator for calling methods.
     */
    private void logGeometricEvent(NeuronVertex source, CategoryString category) {
        float[] location = source.getLocation();
        logGeometricEvent(
                new Double(location[0]), new Double(location[1]), new Double(location[2]),
                category);
    }

    private void logGeometricEvent(Double x, Double y, Double z, CategoryString category) {
        String action = formatGeoAction(x, y, z);
        activityLogging.logToolEvent(
                HORTA_TOOL_STRING,
                category,
                new ActionString(action)
        );
    }
    
    private Long getSampleId() {
        return sampleLocation != null ? sampleLocation.getSampleId() : null;
    }
    
    private Long getWorkspaceId() {
        return sampleLocation != null ? sampleLocation.getWorkspaceId() : null;
    }

    /** Horta coordinates are all in micrometers. */
    private String formatGeoAction(Double muX, Double muY, Double muZ) {
        double x = 0;
        double y = 0;
        double z = 0;
        String action = String.format(
                BOTH_COORDS_FMT,
                getSampleId(), getWorkspaceId(),
                muX, muY, muZ,
                x, y, z
        );
        return action;
    }

}
