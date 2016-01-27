package org.janelia.it.workstation.gui.large_volume_viewer.activity_logging;

import java.util.Date;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import static org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID;

/**
 * Keep all the logging code in one place, to declutter.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    public static final String BOTH_COORDS_FMT = "%d:%d:%7.3f,%7.3f,%7.3f:%7.3f,%7.3f,%7.3f";

    // These category strings are used similarly.  Lining them up spatially
    // makes it easier to see that they are all different.
    private static final CategoryString LIX_CATEGORY_STRING                     = new CategoryString("loadTileIndexToRam:elapsed");
    private static final CategoryString LONG_TILE_LOAD_CATEGORY_STRING          = new CategoryString("longRunningTileIndexLoad");
    private static final CategoryString LVV_SESSION_CATEGORY_STRING             = new CategoryString("openFolder");
    private static final CategoryString LVV_ADD_ANCHOR_CATEGORY_STRING          = new CategoryString("addAnchor:xyzsw");
    private static final CategoryString LVV_MERGE_NEURITES_CATEGORY_STRING      = new CategoryString("mergeNeurites:xyzsw");
    private static final CategoryString LVV_MOVE_NEURITE_CATEGORY_STRING        = new CategoryString("moveNeurite:xyzsw");
    private static final CategoryString LVV_SPLIT_NEURITE_CATEGORY_STRING       = new CategoryString("splitNeurite:xyzsw");
    private static final CategoryString LVV_SPLIT_ANNO_CATEGORY_STRING          = new CategoryString("splitAnnotation:xyzsw");
    private static final CategoryString LVV_DELETE_LINK_CATEGORY_STRING         = new CategoryString("deleteLink:xyzsw");
    private static final CategoryString LVV_DELETE_SUBTREE_CATEGORY_STRING      = new CategoryString("deleteSubTree:xyzsw");
    private static final CategoryString LVV_REROOT_NEURITE_CATEGORY_STRING      = new CategoryString("rerootNeurite");
    private static final CategoryString LVV_3D_LAUNCH_CATEGORY_STRING           = new CategoryString("launch3dBrickView");
    private static final CategoryString LVV_NAVIGATE_LANDMARK_CATEGORY_STRING   = new CategoryString("navigateInLandmarkView");
    
    private static final int LONG_TIME_LOAD_LOG_THRESHOLD = 5 * 1000;
    
    private TileFormat tileFormat;
    
    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;
    }

    public void logTileLoad(int relativeSlice, TileIndex tileIndex, final double elapsedMs, long folderOpenTimestamp) {
        final ActionString actionString = new ActionString(
                folderOpenTimestamp + ":" + relativeSlice + ":" + tileIndex.toString() + ":elapsed_ms=" + elapsedMs
        );
        // Use the by-category granularity for these.
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LIX_CATEGORY_STRING,
                actionString,
                elapsedMs,
                Double.MAX_VALUE
        );
        // Use the elapsed cutoff for this parallel category.
        SessionMgr.getSessionMgr().logToolThresholdEvent(
                LVV_LOGSTAMP_ID,
                LONG_TILE_LOAD_CATEGORY_STRING,
                actionString,
                new Date().getTime(),
                elapsedMs,
                LONG_TIME_LOAD_LOG_THRESHOLD
        );
    }

    public void logFolderOpen(String remoteBasePath, long folderOpenTimestamp) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_SESSION_CATEGORY_STRING, 
                new ActionString(remoteBasePath + ":" + folderOpenTimestamp)
        );
    }
    
    public void logAddAnchor(Long sampleId, Long workspaceId, Vec3 location) {        
        //  Change Vec3 to double[] if inconvenient.
        logGeometricEvent(
                sampleId,
                workspaceId,
                location.getX(), location.getY(), location.getZ(), 
                LVV_ADD_ANCHOR_CATEGORY_STRING);
    }
    
    public void logRerootNeurite(Long sampleID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_REROOT_NEURITE_CATEGORY_STRING, 
                new ActionString(sampleID + ":" + neuronID)
        );
    }
    
    public void logMergedNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_MERGE_NEURITES_CATEGORY_STRING);
    }
    
    public void logMovedNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_MOVE_NEURITE_CATEGORY_STRING);
    }
    
    public void logSplitNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_SPLIT_NEURITE_CATEGORY_STRING);
    }

    public void logSplitAnnotation(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_SPLIT_ANNO_CATEGORY_STRING);
    }
    
    public void logDeleteLink(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_DELETE_LINK_CATEGORY_STRING);
    }
    
    public void logDeleteSubTree(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_DELETE_SUBTREE_CATEGORY_STRING);        
    }
    
    public void logSnapshotLaunch(String labelText, Long workspaceId) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_3D_LAUNCH_CATEGORY_STRING,
                new ActionString(labelText + " workspaceId=" + workspaceId)
        );
    }
    
    public void logLandmarkViewPick(AnnotationModel annotationModel, Long annotationId) {
        String action = "Unknown";
        if (annotationModel != null
                && annotationModel.getCurrentWorkspace() != null
                && annotationModel.getCurrentWorkspace().getId() != null) {
            action = "Sample/Annotation:" + annotationModel.getCurrentWorkspace().getSampleID() + ":" + annotationId;
        }
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_NAVIGATE_LANDMARK_CATEGORY_STRING,
                new ActionString(action)
        );
    }
    
    private void logGeometricEvent(Long sampleID, Long workspaceID, Double x, Double y, Double z, CategoryString category) {
        String action = formatGeoAction(x, y, z, sampleID, workspaceID);
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                category,
                new ActionString(action)
        );
    }

    private void logGeometricEvent(Long sampleID, Long workspaceID, TmGeoAnnotation anno, CategoryString category) {
        logGeometricEvent(
                sampleID, workspaceID,
                anno.getX(), anno.getY(), anno.getZ(),
                category);
    }

    private String formatGeoAction(Double x, Double y, Double z, Long sampleID, Long workspaceID) {
        TileFormat.MicrometerXyz mxyz;
        String action;
        double muX = 0;
        double muY = 0;
        double muZ = 0;
        if (tileFormat != null) {
            mxyz = tileFormat.micrometerXyzForVoxelXyz(
                    new TileFormat.VoxelXyz(x.intValue(), y.intValue(), z.intValue()),
                    CoordinateAxis.Z);
            muX = mxyz.getX();
            muY = mxyz.getY();
            muZ = mxyz.getZ();
        }
        action = String.format(
                BOTH_COORDS_FMT,
                sampleID, workspaceID,
                muX, muY, muZ,
                x, y, z
        );
        return action;
    }

}
