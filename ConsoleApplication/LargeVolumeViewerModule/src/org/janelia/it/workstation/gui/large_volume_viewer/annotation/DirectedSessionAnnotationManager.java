package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Color;
import java.awt.HeadlessException;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnnotationObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.it.jacs.shared.utils.GeomUtils;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.workers.ResultWorker;
import org.janelia.it.workstation.browser.workers.SimpleListenableFuture;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.api.ModelTranslation;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwFocalPoint;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NotesUpdateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmAnchoredPathListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.NeuronSetAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation controller implementation for Directed Tracing Workflow sessions.
 * 
 * TODO: Many methods here throw UnsupportedOperationException because the interface was derived from the old 
 * AnnotationManager (now called BasicAnnotationManager). We need to continue refining the interfaces so that this class
 * implements a subset of the existing interface and the unused methods are eliminated. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DirectedSessionAnnotationManager implements AnnotationManager {

    private static final Logger log = LoggerFactory.getLogger(DirectedSessionAnnotationManager.class);

    protected final ActivityLogHelper activityLog = ActivityLogHelper.getInstance();
    protected final DirectedSessionAnnotationModel annotationModel;
    
    // For communicating annotations to Horta
    protected final NeuronSetAdapter neuronSetAdapter;

    protected QuadViewUi quadViewUi;

    private final TmDirectedSession initialObject;
    
    public DirectedSessionAnnotationManager(TmDirectedSession workspace) {
        
        this.annotationModel = new DirectedSessionAnnotationModel(this);
        
        this.neuronSetAdapter = new NeuronSetAdapter();
        neuronSetAdapter.observe(this);
        
        LargeVolumeViewerTopComponent.getInstance().registerNeurons(neuronSetAdapter);
        Events.getInstance().registerOnEventBus(annotationModel);
        
        this.initialObject = workspace;
        
    }
    
    @Override
    public TmDirectedSession getInitialObject() {
        return initialObject;
    }

    @Override
    public SimpleListenableFuture<TmSample> loadSample() {
        
        ResultWorker<TmSample> worker = new ResultWorker<TmSample>() {
            @Override
            protected TmSample createResult() throws Exception {
                TmSample sample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(initialObject);
                annotationModel.setSample(sample);
                return sample;
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        return worker.executeWithFuture();
    }

    @Override
    public SimpleListenableFuture<Void> load() {
                
        log.info("loadDomainObject({})", initialObject);
                
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.loadSession(initialObject);
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        return worker.executeWithFuture();
    }

    @Override
    public void loadComplete() {
        try {
            annotationModel.loadComplete();

            // Move the viewer to the first decision location
            DtwDecision nextDecision = annotationModel.getNextDecision();
            if (nextDecision!=null) {
                DtwFocalPoint viewingFocus = nextDecision.getViewingFocus();
                if (viewingFocus!=null) {
                    if (viewingFocus.getCenterLocation()!=null) {
                        Vec3 initialViewFocus = GeomUtils.parseVec3(viewingFocus.getCenterLocation());
                        log.info("Setting intial camera focus: {}", initialViewFocus);
                        quadViewUi.setCameraFocus(initialViewFocus);
                    }
                }
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    protected Long getSampleID() {
        if (initialObject != null) {
            return initialObject.getSampleRef().getTargetId();
        } 
        return null;
    }
    
    //-------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        activityLog.setTileFormat(getTileFormat(), getSampleID());
    }

    @Override
    public void update(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pathTraced(Long neuronId, AnchoredVoxelPath path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setQuadViewUi(QuadViewUi quadViewUi) {
        this.quadViewUi = quadViewUi;
    }

    @Override
    public void setLvvTranslator(LargeVolumeViewerTranslator lvvTranslator) {
    }

    @Override
    public TileFormat getTileFormat() {
        return quadViewUi.getTileServer().getLoadAdapter().getTileFormat();
    }

    @Override
    public Vec3 getMicrometerVecFromVoxelVec(Vec3 voxelVec) {
        TileFormat.MicrometerXyz mmXyz = getTileFormat().micrometerXyzForVoxelXyz(
                new TileFormat.VoxelXyz(
                        (int)voxelVec.getX(),
                        (int)voxelVec.getY(),
                        (int)voxelVec.getZ()), 
                CoordinateAxis.Z);
        return new Vec3(mmXyz.getX(), mmXyz.getY(), mmXyz.getZ());
    }
    
    @Override
    public void close() {
        Events.getInstance().unregisterOnEventBus(annotationModel);
    }

    @Override
    public boolean editsAllowed() {
        return annotationModel.editsAllowed();
    }

    @Override
    public void deleteSubtreeRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void splitAnchorRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rerootNeuriteRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void splitNeuriteRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteLinkRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEditNoteRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void editNeuronTagsRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void anchorAdded(AnchorSeed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveAnchor(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveAnnotation(Long neuronID, Long annotationID, Vec3 micronLocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mergeNeurite(Long sourceNeuronID, Long sourceAnnotationID, Long targetNeuronID, Long targetAnnotationID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void smartMergeNeuriteRequested(Anchor sourceAnchor, Anchor targetAnchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveNeuriteRequested(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAnchoredPath(Long neuronID, TmAnchoredPathEndpoints endpoints, List<List<Integer>> points) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEditNote(Long neuronID, Long annotationID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNote(Long neuronID, Long annotationID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createNeuron() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCurrentNeuron() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameNeuron() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveWorkspaceCopy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long relativeAnnotation(Long neuronId, Long annID, AnnotationNavigationDirection direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void chooseNeuronColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void chooseNeuronStyle(Anchor anchor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void chooseNeuronStyle(TmNeuronMetadata neuron) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAllNeuronVisibility(boolean visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleListenableFuture<Void> setBulkNeuronVisibility(Collection<TmNeuronMetadata> neuronList, boolean visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void hideUnselectedNeurons() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void toggleSelectedNeurons() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentNeuronVisibility(boolean visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNeuronVisibility(Anchor anchor, boolean visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NeuronStyle getNeuronStyle(TmNeuronMetadata neuron) {
        return ModelTranslation.translateNeuronStyle(neuron);
    }

    @Override
    public void setNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveQuadViewColorModel() {
        log.info("saveQuadViewColorModel()");
        try {
            TmDirectedSession session = annotationModel.getCurrentSession();
            if (session == null) {
                presentError("You must create a session to be able to save the color model!", "No session");
            }
            else {
                session.setColorModel(ModelTranslation.translateColorModel(quadViewUi.getImageColorModel()));
                log.info("Setting color model: {}",session.getColorModel());
                annotationModel.saveCurrentSession();
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    @Override
    public void saveColorModel3d(ImageColorModel colorModel) {
        log.info("saveColorModel3d()");
        try {
            TmDirectedSession session = annotationModel.getCurrentSession();
            if (session == null) {
                presentError("You must create a session to be able to save the color model!", "No session");
            }
            else {
                session.setColorModel3d(ModelTranslation.translateColorModel(colorModel));
                log.info("Setting 3d color model: {}",session.getColorModel3d());
                annotationModel.saveCurrentSession();
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    @Override
    public void setAutomaticRefinement(boolean state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAutomaticTracing(boolean state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void tracePathToParent(PathTraceToParentRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportNeuronsAsSWC(File swcFile, int downsampleModulo, Collection<TmNeuronMetadata> neurons) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importSWCFiles(List<File> swcFiles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TmAnnotationObject getCurrentAnnotationObject() {
        return initialObject;
    }
    
    @Override
    public TmWorkspace getCurrentWorkspace() {
        return null;
    }

    @Override
    public Collection<TmNeuronMetadata> getNeuronList() {
        return annotationModel.getNeuronList();
    }

    @Override
    public TmNeuronMetadata getCurrentNeuron() {
        return annotationModel.getCurrentNeuron();
    }

    @Override
    public NeuronSet getNeuronSet() {
        return neuronSetAdapter;
    }

    @Override
    public Set<String> getAvailableNeuronTags() {
        return new HashSet<>();
    }

    @Override
    public TmNeuronMetadata getNeuronFromNeuronID(Long neuronID) {
        return annotationModel.getNeuronFromNeuronID(neuronID);
    }

    @Override
    public boolean hasNeuronTag(TmNeuronMetadata neuron, String tag) {
        return false;
    }

    @Override
    public Set<String> getNeuronTags(TmNeuronMetadata neuron) {
        return new HashSet<>();
    }

    @Override
    public void setNeuronColors(List<TmNeuronMetadata> neurons, Color color) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearNeuronTags(TmNeuronMetadata neuron) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public TmSample getCurrentSample() {
        return annotationModel.getCurrentSample();
    }

    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return annotationModel.getSelectionModel();
    }

    @Override
    public FilteredAnnotationModel getFilteredAnnotationModel() {
        return annotationModel.getFilteredAnnotationModel();
    }

    @Override
    public void addTmGeoAnnotationModListener(TmGeoAnnotationModListener listener) {
        annotationModel.addTmGeoAnnotationModListener(listener);
    }

    @Override
    public void removeTmGeoAnnotationModListener(TmGeoAnnotationModListener listener) {
        annotationModel.removeTmGeoAnnotationModListener(listener);
    }

    @Override
    public void addTmAnchoredPathListener(TmAnchoredPathListener listener) {
        annotationModel.addTmAnchoredPathListener(listener);
    }

    @Override
    public void addGlobalAnnotationListener(GlobalAnnotationListener listener) {
        annotationModel.addGlobalAnnotationListener(listener);
    }

    @Override
    public void removeGlobalAnnotationListener(GlobalAnnotationListener listener) {
        annotationModel.removeGlobalAnnotationListener(listener);
    }

    @Override
    public void setViewStateListener(ViewStateListener listener) {
        annotationModel.setViewStateListener(listener);
    }

    @Override
    public void setNotesUpdateListener(NotesUpdateListener notesUpdateListener) {
        annotationModel.setNotesUpdateListener(notesUpdateListener);
    }

    @Override
    public void saveWorkspace(TmWorkspace workspace) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSWCDataConverter(SWCDataConverter converter) {
        annotationModel.setSWCDataConverter(converter);
    }

    @Override
    public void selectNeuron(TmNeuronMetadata neuron) {
        annotationModel.selectNeuron(neuron);
    }

    @Override
    public void createWorkspace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TmGeoAnnotation getGeoAnnotationFromID(Long neuronID, Long annotationID) {
        return annotationModel.getGeoAnnotationFromID(neuronID, annotationID);
    }

    /**
     * given the ID of an annotation, return an object wrapping it (or null)
     */
    public TmGeoAnnotation getGeoAnnotationFromID(TmNeuronMetadata foundNeuron, Long annotationID) {
        if (foundNeuron == null) {
            return null;
        }
        TmGeoAnnotation annotation = foundNeuron.getGeoAnnotationMap().get(annotationID);
        if (annotation == null) {
            log.warn("There is no annotation with id {} in neuron {}", annotationID, foundNeuron.getId());
        }
        return annotation;
    }
    
    @Override
    public TmGeoAnnotation getNeuriteRootAnnotation(TmGeoAnnotation annotation) {
        return annotationModel.getNeuriteRootAnnotation(annotation);
    }

    @Override
    public TmNeuronMetadata createNeuron(String name) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public TmGeoAnnotation addRootAnnotation(TmNeuronMetadata neuron, Vec3 xyz) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public TmGeoAnnotation addChildAnnotation(TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAnnotationRadius(Long neuronID, Long annotationID, float radius) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveNeurite(TmGeoAnnotation tmGeoAnnotation, TmNeuronMetadata neuron) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void splitNeurite(Long neuronId, Long newRootId) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteLink(TmGeoAnnotation link) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fireSpatialIndexReady(TmAnnotationObject annotationObject) {
        annotationModel.fireSpatialIndexReady(annotationObject);
    }

    @Override
    public void generateRandomNeurons(Integer neuronCount, Integer meanPointsPerNeuron, BoundingBox3d boundingBox, Float branchProbability) {
        throw new UnsupportedOperationException();
    }

    public SimpleListenableFuture<DtwDecision> nextDecision() {
        
        ResultWorker<DtwDecision> worker = new ResultWorker<DtwDecision>() {
            private DtwDecision nextDecision;

            @Override
            protected DtwDecision createResult() throws Exception {
                nextDecision = annotationModel.nextDecisionRequested();
                return nextDecision;
            }

            @Override
            protected void hadSuccess() {
                String initialViewFocus = nextDecision.getViewingFocus().getCenterLocation();
                if (initialViewFocus!=null) {
                    log.info("Setting decision camera focus: {}", initialViewFocus);
                    Vec3 viewFocusVoxel = GeomUtils.parseVec3(initialViewFocus);
                    Vec3 viewFocusMicrometer = getMicrometerVecFromVoxelVec(viewFocusVoxel);
                    quadViewUi.setCameraFocus(viewFocusMicrometer);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        return worker.executeWithFuture();
    }
    
    @Override
    public void presentError(String message, String title) throws HeadlessException {
        JOptionPane.showMessageDialog(
                ComponentUtil.getLVVMainWindow(),
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }
}
