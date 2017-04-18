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
import org.janelia.it.jacs.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnnotationObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
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
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
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
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionAnnotationManager implements AnnotationManager {

    private static final Logger log = LoggerFactory.getLogger(SessionAnnotationManager.class);

    protected final ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    // quad view ui object
    protected QuadViewUi quadViewUi;

    // annotation model object
    protected final SessionAnnotationModel annotationModel;

    // For communicating annotations to Horta
    protected final NeuronSetAdapter neuronSetAdapter;
    
    private final TmDirectedSession initialObject;
    
    public SessionAnnotationManager(TmDirectedSession workspace) {
        
        this.annotationModel = new SessionAnnotationModel();
        
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
        annotationModel.loadComplete();
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
    }

    @Override
    public void pathTraced(Long neuronId, AnchoredVoxelPath path) {
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
    public void close() {
    }

    @Override
    public boolean editsAllowed() {
        return annotationModel.editsAllowed();
    }

    @Override
    public void deleteSubtreeRequested(Anchor anchor) {
    }

    @Override
    public void splitAnchorRequested(Anchor anchor) {
    }

    @Override
    public void rerootNeuriteRequested(Anchor anchor) {
    }

    @Override
    public void splitNeuriteRequested(Anchor anchor) {
    }

    @Override
    public void deleteLinkRequested(Anchor anchor) {
    }

    @Override
    public void addEditNoteRequested(Anchor anchor) {
    }

    @Override
    public void editNeuronTagsRequested(Anchor anchor) {
    }

    @Override
    public void anchorAdded(AnchorSeed seed) {
    }

    @Override
    public void moveAnchor(Anchor anchor) {
    }

    @Override
    public void moveAnnotation(Long neuronID, Long annotationID, Vec3 micronLocation) {
    }

    @Override
    public void mergeNeurite(Long sourceNeuronID, Long sourceAnnotationID, Long targetNeuronID, Long targetAnnotationID) {
    }

    @Override
    public void smartMergeNeuriteRequested(Anchor sourceAnchor, Anchor targetAnchor) {
    }

    @Override
    public void moveNeuriteRequested(Anchor anchor) {
    }

    @Override
    public void addAnchoredPath(Long neuronID, TmAnchoredPathEndpoints endpoints, List<List<Integer>> points) {
    }

    @Override
    public void addEditNote(Long neuronID, Long annotationID) {
    }

    @Override
    public String getNote(Long neuronID, Long annotationID) {
        return null;
    }

    @Override
    public void createNeuron() {
    }

    @Override
    public void deleteCurrentNeuron() {
    }

    @Override
    public void renameNeuron() {
    }

    @Override
    public void saveWorkspaceCopy() {
    }

    @Override
    public Long relativeAnnotation(Long neuronId, Long annID, AnnotationNavigationDirection direction) {
        return null;
    }

    @Override
    public void chooseNeuronColor() {
    }

    @Override
    public void chooseNeuronStyle(Anchor anchor) {
    }

    @Override
    public void chooseNeuronStyle(TmNeuronMetadata neuron) {
    }

    @Override
    public void setAllNeuronVisibility(boolean visibility) {
    }

    @Override
    public SimpleListenableFuture<Void> setBulkNeuronVisibility(Collection<TmNeuronMetadata> neuronList, boolean visibility) {
        return null;
    }

    @Override
    public void hideUnselectedNeurons() {
    }

    @Override
    public void toggleSelectedNeurons() {
    }

    @Override
    public void setCurrentNeuronVisibility(boolean visibility) {
    }

    @Override
    public void setNeuronVisibility(Anchor anchor, boolean visibility) {
    }

    @Override
    public NeuronStyle getNeuronStyle(TmNeuronMetadata neuron) {
        return null;
    }

    @Override
    public void setNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style) {
    }

    @Override
    public void saveQuadViewColorModel() {
    }

    @Override
    public void saveColorModel3d(ImageColorModel colorModel) {
    }

    @Override
    public void setAutomaticRefinement(boolean state) {
    }

    @Override
    public void setAutomaticTracing(boolean state) {
    }

    @Override
    public void tracePathToParent(PathTraceToParentRequest request) {
    }

    @Override
    public void exportNeuronsAsSWC(File swcFile, int downsampleModulo, Collection<TmNeuronMetadata> neurons) {
    }

    @Override
    public void importSWCFiles(List<File> swcFiles) {
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
        return null;
    }

    @Override
    public void setNeuronColors(List<TmNeuronMetadata> neurons, Color color) throws Exception {
    }

    @Override
    public void addNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
    }

    @Override
    public void addNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
    }

    @Override
    public void removeNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
    }

    @Override
    public void removeNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
    }

    @Override
    public void clearNeuronTags(TmNeuronMetadata neuron) throws Exception {
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
        return null;
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
    }

    @Override
    public void setSWCDataConverter(SWCDataConverter converter) {
    }

    @Override
    public void selectNeuron(TmNeuronMetadata neuron) {
        annotationModel.selectNeuron(neuron);
    }

    @Override
    public void createWorkspace() {
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
        return null;
    }

    @Override
    public TmGeoAnnotation addRootAnnotation(TmNeuronMetadata neuron, Vec3 xyz) throws Exception {
        return null;
    }

    @Override
    public TmGeoAnnotation addChildAnnotation(TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception {
        return null;
    }

    @Override
    public void updateAnnotationRadius(Long neuronID, Long annotationID, float radius) throws Exception {
    }

    @Override
    public void moveNeurite(TmGeoAnnotation tmGeoAnnotation, TmNeuronMetadata neuron) throws Exception {
    }

    @Override
    public void splitNeurite(Long neuronId, Long newRootId) throws Exception {
    }

    @Override
    public void deleteLink(TmGeoAnnotation link) throws Exception {
    }

    @Override
    public void fireSpatialIndexReady(TmAnnotationObject annotationObject) {
        annotationModel.fireSpatialIndexReady(annotationObject);
    }

    @Override
    public void generateRandomNeurons(Integer neuronCount, Integer meanPointsPerNeuron, BoundingBox3d boundingBox, Float branchProbability) {
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
