package org.janelia.workstation.controller.model;

import Jama.Matrix;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.model.util.MatrixUtilities;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.SpatialIndexManager;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgrFactory;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.controller.tileimagery.TileLoader;
import org.janelia.workstation.controller.tileimagery.TileServer;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;

/**
 * This class is singleton to manage references to all the different model
 * data that TM viewers might need to access.  This includes stuff like Samples, Workspaces, Meshes,
 * Scripts, ViewState, SelectionState.  Having it in one place gives us an easy way to manage performance,
 * provide concurrent-safe data structures, load/unload data, and add integrations with plugins and 3rd party tools.
 *
 * There are assumptions to simplify things like 1 Sample/Workspace open,
 * @author schauderd
 */
public class TmModelManager {

    private static final Logger log = LoggerFactory.getLogger(TmModelManager.class);

    private TmSample currentSample;
    private TmWorkspace currentWorkspace;
    private TmNeuronTagMap currentTagMap;

    private TmViewState currentView;
    private TmSelectionState currentSelections;
    private TmReviewState currentReviews;
    private TmHistory neuronHistory;

    private Jama.Matrix voxToMicronMatrix;
    private Jama.Matrix micronToVoxMatrix;
    private NeuronModel neuronModel;
    private final TiledMicroscopeDomainMgr tmDomainMgr;
    private TileLoader tileLoader;
    private TileServer tileServer;
    private TileFormat tileFormat;
    private Vec3 voxelCenter;
    private BoundingBox3d sampleBoundingBox;
    private SpatialIndexManager spatialIndexManager;
    private boolean isLocal;

    private static final TmModelManager instance = new TmModelManager();

    public static TmModelManager getInstance() {
        return instance;
    }
    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    private TmModelManager() {
        this.tmDomainMgr = TiledMicroscopeDomainMgrFactory.getDomainMgr();
        neuronModel = NeuronModel.getInstance();
        currentView = new TmViewState();
        spatialIndexManager = new SpatialIndexManager();
        neuronHistory = new TmHistory();
        initModel();
         }

    private void initModel() {
        String connectionType = FrameworkAccess.getLocalPreferenceValue(ConnectionMgr.class, ConnectionMgr.CONNECTION_STRING_PREF, null);
        if (connectionType!=null && connectionType.equals("local")) {
            isLocal =true;
        } else {
            isLocal = false;
        }
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public TmSample getCurrentSample() {
        return currentSample;
    }

    public SpatialIndexManager getSpatialIndexManager() {
        return spatialIndexManager;
    }

    public void setSampleMatrices(Matrix micronToVoxMatrix, Matrix voxToMicronMatrix) throws Exception {
        if (currentSample==null) {
            throw new IllegalStateException("Sample is not loaded");
        }
        currentSample.setMicronToVoxMatrix(MatrixUtilities.serializeMatrix(micronToVoxMatrix, "micronToVoxMatrix"));
        currentSample.setVoxToMicronMatrix(MatrixUtilities.serializeMatrix(voxToMicronMatrix, "voxToMicronMatrix"));
        tmDomainMgr.save(currentSample);
    }

    public Jama.Matrix getVoxToMicronMatrix() {
        return voxToMicronMatrix;
    }

    public float[] getLocationInMicrometers(double x, double y, double z)
    {
        // Convert from image voxel coordinates to Cartesian micrometers
        // TmGeoAnnotation is in voxel coordinates
        Jama.Matrix voxLoc = new Jama.Matrix(new double[][] {
                {x, },
                {y, },
                {z, },
                {1.0, },
        });
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix micLoc = getVoxToMicronMatrix().times(voxLoc);
        return new float[] {
                (float) micLoc.get(0, 0),
                (float) micLoc.get(1, 0),
                (float) micLoc.get(2, 0)};
    }

    public float[] convertLocationToVoxels(float x, float y, float z)
    {
        // Convert from Cartesian micrometers to image voxel coordinates
        // TmGeoAnnotation is in voxel coordinates
        Jama.Matrix voxLoc = new Jama.Matrix(new double[][] {
                {x, },
                {y, },
                {z, },
                {1.0, },
        });
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix micLoc = getVoxToMicronMatrix().times(voxLoc);
        return new float[] {
                (float) micLoc.get(0, 0),
                (float) micLoc.get(1, 0),
                (float) micLoc.get(2, 0)};
    }

    public Jama.Matrix getMicronToVoxMatrix() {
        return micronToVoxMatrix;
    }

    public void updateVoxToMicronMatrices() {
        if (currentSample == null) {
            return;
        }
        String serializedVoxToMicronMatrix = currentSample.getVoxToMicronMatrix();
        if (serializedVoxToMicronMatrix == null) {
            FrameworkAccess.handleException(new Throwable("Null VoxMicron Matrix in Sample " + currentSample.getId()));
            return;
        }
        voxToMicronMatrix = MatrixUtilities.deserializeMatrix(serializedVoxToMicronMatrix, "voxToMicronMatrix");

        String serializedMicronToVoxMatrix = currentSample.getMicronToVoxMatrix();
        if (serializedMicronToVoxMatrix == null) {
            FrameworkAccess.handleException(new Throwable("Null MicronVox Matrix in Sample " + currentSample.getId()));
            return;
        }
        micronToVoxMatrix = MatrixUtilities.deserializeMatrix(serializedMicronToVoxMatrix, "micronToVoxMatrix");
    }

    public void setCurrentSample(TmSample currentSample) {
        this.currentSample = currentSample;
    }

    public TmWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    public void setCurrentWorkspace(TmWorkspace currentWorkspace) {
        this.currentWorkspace = currentWorkspace;
    }

    public TmViewState getCurrentView() {
        return currentView;
    }

    public void setCurrentReviews(TmReviewState currentReviews) {
        this.currentReviews = currentReviews;
    }

    public TmReviewState getCurrentReviews() {
        return currentReviews;
    }

    public void setCurrentView(TmViewState currentView) {
        this.currentView = currentView;
    }

    public TmSelectionState getCurrentSelections() {
        return TmSelectionState.getInstance();
    }

    public void setCurrentSelections(TmSelectionState currentSelections) {
        this.currentSelections = currentSelections;
    }

    public void saveCurrentWorkspace() throws Exception {
        tmDomainMgr.save(currentWorkspace);
    }

    public void saveWorkspace(TmWorkspace workspace) throws Exception {
        tmDomainMgr.save(workspace);
    }

    private Long getWsId() {
        if (currentWorkspace != null) {
            return currentWorkspace.getId();
        }
        else {
            return -1L;
        }
    }

    public NeuronModel getNeuronModel() {
        return neuronModel;
    }

    public void setNeuronModel(NeuronModel neuronModel) {
        this.neuronModel = neuronModel;
    }

    public TmNeuronTagMap getCurrentTagMap() {
        return currentTagMap;
    }

    public TmNeuronTagMap getAllTagMeta() {
        // if not set return an empty tag map
        return currentTagMap == null ? new TmNeuronTagMap() : currentTagMap;
    }

    public void setCurrentTagMap(TmNeuronTagMap currentTagMap) {
        this.currentTagMap = currentTagMap;
    }

    public TileFormat getTileFormat() {
        return tileFormat;
    }

    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;
    }

    public TileLoader getTileLoader() {
        return tileLoader;
    }

    public void setTileLoader(TileLoader tileLoader) {
        this.tileLoader = tileLoader;
    }

    public TileServer getTileServer() {
        return tileServer;
    }

    public void setTileServer(TileServer tileServer) {
        this.tileServer = tileServer;
    }

    public BoundingBox3d getSampleBoundingBox() {
        return sampleBoundingBox;
    }

    public void setSampleBoundingBox(BoundingBox3d sampleBoundingBox) {
        this.sampleBoundingBox = sampleBoundingBox;
    }

    public Vec3 getVoxelCenter() {
        return voxelCenter;
    }

    public void setVoxelCenter(Vec3 voxelCenter) {
        this.voxelCenter = voxelCenter;
    }


    public boolean checkOwnership(Long neuronID)  {
        return checkOwnership(NeuronManager.getInstance().getNeuronFromNeuronID(neuronID));
    }

    public static boolean checkOwnership(TmNeuronMetadata neuron)  {
        if (neuron.getOwnerKey().equals(AccessManager.getSubjectKey()))
            return true;
        if (neuron.getOwnerKey().equals(ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim())) {
            try {
                NeuronManager.getInstance().changeNeuronOwner(Arrays.asList(new TmNeuronMetadata[]{neuron}),
                        AccessManager.getAccessManager().getActualSubject());
                return true;
            } catch (Exception e) {
                FrameworkAccess.handleException(e);
                return false;
            }
        }
        JOptionPane.showMessageDialog(
                null,
                "Neuron " + neuron.getName() + " is owned by " + neuron.getOwnerKey() +
                        ". Ask them for ownership if you'd like to make changes.",
                "Neuron not owned",
                JOptionPane.WARNING_MESSAGE);
        return false;
    }

    public TmHistory getNeuronHistory() {
        return neuronHistory;
    }

    public void setNeuronHistory(TmHistory neuronHistory) {
        this.neuronHistory = neuronHistory;
    }
}
