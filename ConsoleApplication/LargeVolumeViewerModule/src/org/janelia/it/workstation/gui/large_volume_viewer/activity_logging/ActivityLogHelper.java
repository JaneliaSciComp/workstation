package org.janelia.it.workstation.gui.large_volume_viewer.activity_logging;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import static org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID;

/**
 * Keep all the logging code in one place, to declutter.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    // sample:workspace:micronX,micronY,micronZ:voxX,voxY,voxZ:time
    public static final String BOTH_COORDS_FMT = "%d:%d:%5.3f,%5.3f,%5.3f:%5.3f,%5.3f,%5.3f";
    // workspace:X,Y,Z:time
    public static final String SIMPLE_COORDS_FMT = "%d:%5.3f,%5.3f,%5.3f";

    private static final ActivityLogHelper instance = new ActivityLogHelper();

    // These category strings are used similarly.  Lining them up spatially
    // makes it easier to see that they are all different.
    // xyzsw = x-y-z coords, sample, workspace
    private static final ToolString EXTERNAL_LVV_LOGSTAMP_ID                    = new ToolString("Horta");
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
    // endAOmu: end of annotation operation; coords as microns
    // endAOvx: end of annotation operation; coords as voxels.
    // xyzw=x-y-z coords, workspace
    private static final CategoryString END_OP_VOXEL_CATEGORY_STRING            = new CategoryString("endAOvx:xyzw");
    // Time-of-writing: nothing is producing micron coords alone.
    private static final CategoryString END_OP_MICRON_CATEGORY_STRING           = new CategoryString("endAOmu:xyzw");
    private static final CategoryString LVV_LOAD_WORKSPACE_CATEGORY_STRING      = new CategoryString("loadWorkspace");
    private static final CategoryString LVV_SET_PREFERENCE_CATEGORY_STRING      = new CategoryString("setPreference");
    private static final CategoryString LVV_SELECT_NEURON_CATEGORY_STRING       = new CategoryString("selectNeuron");
    private static final CategoryString LVV_CREATE_NEURON_CATEGORY_STRING       = new CategoryString("createNeuron");
    private static final CategoryString LVV_RENAME_NEURON_CATEGORY_STRING       = new CategoryString("renameNeuron");
    private static final CategoryString LVV_DELETE_NEURON_CATEGORY_STRING       = new CategoryString("deleteNeuron");
    private static final CategoryString LVV_CREATE_WORKSPACE_CATEGORY_STRING    = new CategoryString("createWorkspace");
    private static final CategoryString LVV_ADD_ANCHORED_PATH_CATEGORY_STRING   = new CategoryString("addAnchoredPath");
    private static final CategoryString LVV_REMOVE_ANCHORED_PATH_CATEGORY_STRING = new CategoryString("removeAnchoredPath");
    private static final CategoryString LVV_SET_NOTE_CATEGORY_STRING            = new CategoryString("setNote");
    private static final CategoryString LVV_REMOVE_NOTE_CATEGORY_STRING         = new CategoryString("removeNote");
    private static final CategoryString LVV_SET_STYLE_CATEGORY_STRING           = new CategoryString("setStyle");
    private static final CategoryString LVV_SHOW_WORKSPACE_INFO_CATEGORY_STRING = new CategoryString("showWorkspaceInfo");
    private static final CategoryString LVV_EXPORT_SWC_CATEGORY_STRING          = new CategoryString("exportSWCFile");
    private static final CategoryString LVV_IMPORT_SWC_CATEGORY_STRING          = new CategoryString("importSWCFile");

    private static final int LONG_TIME_LOAD_LOG_THRESHOLD = 5 * 1000;

    private Map<Long,TileFormat> sampleToTileFormat = new HashMap<>();

    public static ActivityLogHelper getInstance() {
        return instance;
    }

    private ActivityLogHelper() {}

    public void setTileFormat(TileFormat tileFormat, Long sampleId) {
        assert tileFormat != null : "Null tile format";
        assert sampleId != null : "Null sample id";
        sampleToTileFormat.put(sampleId, tileFormat);
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

    public void logExternallyAddAnchor(Long sampleId, Long workspaceId, TmGeoAnnotation source, float[] micronXYZ) {
        //  Change Vec3 to double[] if inconvenient.
        logExternalGeometricEvent(
                sampleId,
                workspaceId,
                source.getX(), source.getY(), source.getZ(),
                micronXYZ[0], micronXYZ[1], micronXYZ[2],
                LVV_ADD_ANCHOR_CATEGORY_STRING);
    }

    public void logExternallyAddAnchor(Long sampleId, Long workspaceId, Vec3 source, float[] micronXYZ) {
        //  Change Vec3 to double[] if inconvenient.
        logExternalGeometricEvent(
                sampleId,
                workspaceId,
                source.getX(), source.getY(), source.getZ(),
                micronXYZ[0], micronXYZ[1], micronXYZ[2],
                LVV_ADD_ANCHOR_CATEGORY_STRING);
    }

    public void logRerootNeurite(Long sampleID, Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_REROOT_NEURITE_CATEGORY_STRING,
                new ActionString(sampleID + ":" + workspaceID + ":" + neuronID)
        );
    }

    public void logMergedNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_MERGE_NEURITES_CATEGORY_STRING);
    }

    public void logMovedNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_MOVE_NEURITE_CATEGORY_STRING);
    }

    public void logMovedNeurite(Long sampleID, long workspaceID, Vec3 location) {
        this.logGeometricEvent(sampleID, workspaceID, location, LVV_MOVE_NEURITE_CATEGORY_STRING);
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

    public void logExternallyDeleteLink(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        double muX = 0;
        double muY = 0;
        double muZ = 0;
        TileFormat tileFormat = sampleToTileFormat.get(sampleID);
        if (tileFormat != null) {
            TileFormat.MicrometerXyz mxyz = tileFormat.micrometerXyzForVoxelXyz(
                    new TileFormat.VoxelXyz(source.getX().intValue(), source.getY().intValue(), source.getZ().intValue()),
                    CoordinateAxis.Z);
            muX = mxyz.getX();
            muY = mxyz.getY();
            muZ = mxyz.getZ();
        }
        this.logExternalGeometricEvent(
                sampleID, workspaceID,
                source.getX(), source.getY(), source.getZ(),
                (float)muX, (float)muY, (float)muZ,
                LVV_DELETE_LINK_CATEGORY_STRING
        );
    }

    public void logExternallyMergeNeurite(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        double muX = 0;
        double muY = 0;
        double muZ = 0;
        TileFormat tileFormat = sampleToTileFormat.get(sampleID);
        if (tileFormat != null) {
            TileFormat.MicrometerXyz mxyz = tileFormat.micrometerXyzForVoxelXyz(
                    new TileFormat.VoxelXyz(source.getX().intValue(), source.getY().intValue(), source.getZ().intValue()),
                    CoordinateAxis.Z);
            muX = mxyz.getX();
            muY = mxyz.getY();
            muZ = mxyz.getZ();
        }
        this.logExternalGeometricEvent(
                sampleID, workspaceID,
                source.getX(), source.getY(), source.getZ(),
                (float)muX, (float)muY, (float)muZ,
                LVV_MERGE_NEURITES_CATEGORY_STRING
        );
    }

    public void logDeleteSubTree(Long sampleID, Long workspaceID, TmGeoAnnotation source) {
        this.logGeometricEvent(sampleID, workspaceID, source, LVV_DELETE_SUBTREE_CATEGORY_STRING);
    }

    /**
     * Capture when something has been completed.  The location should be
     * available from the annotation. Protect caller from runtime errors.
     *
     * @param workspaceID which workspace was it?
     * @param annotation annotation, bearing the location.
     */
    public void logEndOfOperation(Long workspaceID, TmGeoAnnotation annotation) {
        if (annotation == null  ||  workspaceID == null) {
            return;
        }
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                END_OP_VOXEL_CATEGORY_STRING,
                new ActionString(
                        formatSingleLocationAction(
                                annotation.getX(), annotation.getY(), annotation.getZ(),
                                workspaceID
                        )
                )
        );
    }

    /**
     * Capture when something has been completed.  The location should be
     * available from the annotation. Shield caller from errors.
     *
     * @param workspaceID which workspace was it?
     * @param location location.
     */
    public void logEndOfOperation(Long workspaceID, Vec3 location) {
        if (workspaceID == null  ||  location == null) {
            return;
        }
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                END_OP_VOXEL_CATEGORY_STRING,
                new ActionString(
                        formatSingleLocationAction(
                                location.getX(), location.getY(), location.getZ(),
                                workspaceID
                        )
                )
        );
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

    public void logLoadWorkspace(Long workspaceID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_LOAD_WORKSPACE_CATEGORY_STRING,
                new ActionString(workspaceID.toString())
        );
    }

    public void logSetPreference(Long workspaceID, String key) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_SET_PREFERENCE_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + key)
        );
    }

    public void logSelectNeuron(Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_SELECT_NEURON_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + neuronID.toString())
        );
    }

    public void logCreateNeuron(Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_CREATE_NEURON_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + neuronID.toString())
        );
    }

    public void logRenameNeuron(Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_RENAME_NEURON_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + neuronID.toString())
        );
    }

    public void logDeleteNeuron(Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_DELETE_NEURON_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + neuronID.toString())
        );
    }

    public void logCreateWorkspace(Long workspaceID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_CREATE_WORKSPACE_CATEGORY_STRING,
                new ActionString(workspaceID.toString())
        );
    }

    public void logAddAnchoredPath(Long workspaceID, Long pathID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_ADD_ANCHORED_PATH_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + pathID.toString())
        );
    }

    public void logRemoveAnchoredPath(Long workspaceID, Long pathID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_REMOVE_ANCHORED_PATH_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + pathID.toString())
        );
    }

    public void logSetNote(Long workspaceID, Long annotationID, String note) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_SET_NOTE_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + annotationID.toString() + ":" + note)
        );
    }

    public void logRemoveNote(Long workspaceID, Long annotationID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_REMOVE_NOTE_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + annotationID.toString())
        );
    }

    public void logSetStyle(Long workspaceID, Long neuronID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_SET_STYLE_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + neuronID.toString())
        );
    }

    public void logShowWorkspaceInfo(Long workspaceID) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_SHOW_WORKSPACE_INFO_CATEGORY_STRING,
                new ActionString(workspaceID.toString())
        );
    }

    public void logExportSWCFile(Long workspaceID, String filename) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_EXPORT_SWC_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + filename)
        );
    }

    public void logImportSWCFile(Long workspaceID, String filename) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LVV_IMPORT_SWC_CATEGORY_STRING,
                new ActionString(workspaceID.toString() + ":" + filename)
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

    private void logExternalGeometricEvent(Long sampleID, Long workspaceID, Double x, Double y, Double z, float muX, float muY, float muZ, CategoryString category) {
        String action = formatGeoAction(x, y, z, muX, muY, muZ, sampleID, workspaceID);
        SessionMgr.getSessionMgr().logToolEvent(
                EXTERNAL_LVV_LOGSTAMP_ID,   // For now: only Horta makes requests.
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

    private void logGeometricEvent(Long sampleID, Long workspaceID, Vec3 location, CategoryString category) {
        logGeometricEvent(
                sampleID, workspaceID,
                location.getX(), location.getY(), location.getZ(),
                category);
    }

    private String formatGeoAction(Double x, Double y, Double z, Long sampleID, Long workspaceID) {
        TileFormat.MicrometerXyz mxyz;
        String action;
        double muX = 0;
        double muY = 0;
        double muZ = 0;
        TileFormat tileFormat = sampleToTileFormat.get(sampleID);
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

    private String formatGeoAction(Double x, Double y, Double z, float muX, float muY, float muZ, Long sampleID, Long workspaceID) {
        String action;
        action = String.format(
                BOTH_COORDS_FMT,
                sampleID, workspaceID,
                muX, muY, muZ,
                x, y, z
        );
        return action;
    }

    private String formatSingleLocationAction(Double x, Double y, Double z, Long workspaceID) {
        String action = String.format(
                SIMPLE_COORDS_FMT,
                workspaceID,
                x, y, z
        );
        return action;
    }

}
