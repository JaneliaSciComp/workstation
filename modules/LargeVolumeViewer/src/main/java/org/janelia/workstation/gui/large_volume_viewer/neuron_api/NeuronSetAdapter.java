package org.janelia.workstation.gui.large_volume_viewer.neuron_api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Stopwatch;
import org.janelia.console.viewerapi.model.BasicNeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.NeuronVertexUpdateObservable;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.workstation.controller.NeuronVertexAdapter;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.listener.BackgroundAnnotationListener;
import org.janelia.workstation.controller.listener.GlobalAnnotationListener;
import org.janelia.workstation.controller.listener.TaskReviewListener;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.util.MatrixUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expose NeuronSet interface, using in-memory data resident in LVV
 *
 * @author Christopher Bruns
 */
public class NeuronSetAdapter
        extends BasicNeuronSet
        implements NeuronSet, TaskReviewListener {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    TmWorkspace workspace; // LVV workspace, as opposed to Horta workspace
    NeuronManager annotationModel;
    private final GlobalAnnotationListener globalAnnotationListener;
    private HortaMetaWorkspace metaWorkspace = null;
    private final Lookup.Result<HortaMetaWorkspace> hortaWorkspaceResult = Utilities.actionsGlobalContext().lookupResult(HortaMetaWorkspace.class);
    private final NeuronList innerList;
    private Jama.Matrix voxToMicronMatrix;
    private Jama.Matrix micronToVoxMatrix;
    private final NeuronSetBackgroundAnnotationListener backgroundAnnotationListener;

    private NeuronSetAdapter(NeuronList innerNeuronList) {
        super("LVV neurons", innerNeuronList);
        this.innerList = innerNeuronList;
        this.globalAnnotationListener = new MyGlobalAnnotationListener();
        this.backgroundAnnotationListener = new NeuronSetBackgroundAnnotationListener();
        backgroundAnnotationListener.setGlobal(globalAnnotationListener);
        this.hortaWorkspaceResult.addLookupListener(new NSALookupListener());
    }

    public NeuronSetAdapter() {
        this(new NeuronList());
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    // see note in loadUserPreferences() re: calling back into annmgr for this stuff!
    public void changeNeuronVisibility(TmNeuronMetadata neuron, boolean visibility) {
      //  annotationModel.setNeuronVisibility(neuron, visibility);
    }

    @Override
    public void changeNeuronVisibility(List<TmNeuronMetadata> neuronList, boolean visible) {
        //annotationModel.setNeuronVisibility(neuronList, visible);
    }

    /*@Override
    public void changeNeuronNonInteractable(List<TmNeuronMetadata> neuronList, boolean interactable) {
        LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronNonInteractable(neuronList, interactable);
    }

    @Override
    public void changeNeuronUserToggleRadius(List<TmNeuronMetadata> neuronList, boolean userToggleRadius) {
        LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronUserToggleRadius(neuronList, userToggleRadius);
    }

    @Override
    public void changeNeuronUserProperties(List<TmNeuronMetadata> neuronList, List<String> properties, boolean toggle) {
        LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronUserProperties(neuronList, properties, toggle);
    }*/

    @Override
    public CompletableFuture<Boolean> changeNeuronOwnership(Long neuronId) {
        try {
            TmNeuronMetadata neuron = annotationModel.getNeuronFromNeuronID(neuronId);
            return LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().getAnnotationModel().getNeuronModel().requestOwnershipChange(neuron);
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
        return null;
    }

    private void updateVoxToMicronMatrices(TmSample sample) {
        // If we try to get the matrix too early, it comes back null, so populate just-in-time
        if (sample == null) {
            LOG.error("Attempt to get voxToMicronMatrix for null sample");
            return;
        }
        String serializedVoxToMicronMatrix = sample.getVoxToMicronMatrix();
        if (serializedVoxToMicronMatrix == null) {
            LOG.error("Found null voxToMicronMatrix");
            return;
        }
        voxToMicronMatrix = MatrixUtilities.deserializeMatrix(serializedVoxToMicronMatrix, "voxToMicronMatrix");

        String serializedMicronToVoxMatrix = sample.getMicronToVoxMatrix();
        if (serializedMicronToVoxMatrix == null) {
            LOG.error("Found null micronToVoxMatrix");
            return;
        }
        micronToVoxMatrix = MatrixUtilities.deserializeMatrix(serializedMicronToVoxMatrix, "micronToVoxMatrix");
    }

    Jama.Matrix getVoxToMicronMatrix() {
        if (voxToMicronMatrix != null)
            return voxToMicronMatrix;
        updateVoxToMicronMatrices(TmModelManager.getInstance().getCurrentSample());
        return voxToMicronMatrix;
    }

    Jama.Matrix getMicronToVoxMatrix() {
        if (micronToVoxMatrix != null)
            return micronToVoxMatrix;
        updateVoxToMicronMatrices(TmModelManager.getInstance().getCurrentSample());
        return micronToVoxMatrix;
    }

    @Override
    public boolean isSpatialIndexValid() {
        //return spatialIndex.isValid();
        return false;
    }

    @Override
    public NeuronModel getNeuronByGuid(Long guid) {
        TmNeuronMetadata neuronMetadata = annotationModel.getNeuronFromNeuronID(guid);
        return innerList.neuronModelForTmNeuron(neuronMetadata);
    }

    public NeuronModel getNeuronForAnnotation(TmGeoAnnotation annotation) {
        return neuronModelForTmGeoAnnotation(annotation);
    }

    @Override
    public NeuronModel getNeuronForAnchor(NeuronVertex anchor) {
        if (!(anchor instanceof NeuronVertexAdapter))
            return null;
        TmGeoAnnotation annotation = ((NeuronVertexAdapter) anchor).getTmGeoAnnotation();
        return getNeuronForAnnotation(annotation);
    }

    @Override
    public TmGeoAnnotation getAnnotationForAnchor(NeuronVertex anchor) {
        if (! (anchor instanceof NeuronVertexAdapter))
            return null;
        return ((NeuronVertexAdapter)anchor).getTmGeoAnnotation();
    }
    
    @Override
    public NeuronModel createNeuron(String neuronName) {
        TmNeuronMetadata neuron;
        try {
            neuron = annotationModel.createNeuron(neuronName);
            getMembershipChangeObservable().setChanged();
        } catch (Exception ex) {
            LOG.warn("Error creating neuron", ex);
            return null;
        }
        return new NeuronModelAdapter(neuron, this);
    }

    public void observe(NeuronManager annotationModel) {
        if (annotationModel == null)
            return; // can't watch nothing?
        if (this.annotationModel == annotationModel)
            return; // already watching this model
        // Stop listening to whatever we were listening to earlier
        NeuronManager oldAnnotationModel = this.annotationModel;
        this.annotationModel = annotationModel;
        if (oldAnnotationModel != null) {
            //oldAnnotationModel.removeGlobalAnnotationListener(globalAnnotationListener);
           // oldAnnotationModel.removeTmGeoAnnotationModListener(annotationModListener);
        }
        sanityCheckWorkspace();
       // annotationModel.addGlobalAnnotationListener(globalAnnotationListener);
       // annotationModel.addBackgroundAnnotationListener(backgroundAnnotationListener);
       // annotationModel.addTmGeoAnnotationModListener(annotationModListener);
       // annotationModel.addTaskReviewListener(this);
        getMembershipChangeObservable().notifyObservers();
        LOG.info("Observing new Annotation Model {}", annotationModel);
    }

    // Sometimes the TmWorkspace instance changes, even though the semantic workspace has not changed.
    // In this case, we need to scramble to distribute the new object instances
    // behind our stable NeuronSet/NeuronMode/NeuronVertex facade.
    private void sanityCheckWorkspace() {
        TmWorkspace w = TmModelManager.getInstance().getCurrentWorkspace();
        if (w == workspace) return; // unchanged
        LOG.info("Workspace changed");
        setWorkspace(w);
    }

    // Recache edge data structures after vertices change
    private boolean updateEdges() {
        boolean edgesChanged = false;
        for (NeuronModel neuron : this) {
            if (!(neuron instanceof NeuronModelAdapter))
                continue;
            NeuronModelAdapter n = (NeuronModelAdapter) neuron;
            if (n.updateEdges())
                edgesChanged = true;
        }
        return edgesChanged;
    }

    // TODO: setName()
    @Override
    public String getName() {
        if (workspace != null)
            return workspace.getName();
        else
            return super.getName();
    }

    private boolean setWorkspace(TmWorkspace workspace) {
        if (this.workspace == workspace)
            return false;
        if (workspace == null)
            return false;
        if (!workspace.getName().equals(getName()))
            getNameChangeObservable().setChanged();
        this.workspace = workspace;
        TmSample sample = TmModelManager.getInstance().getCurrentSample();
        if (this.metaWorkspace != null)
            this.metaWorkspace.setSample(sample);
        updateVoxToMicronMatrices(sample);
        NeuronList nl = (NeuronList) neurons;
        nl.wrap(this);

        getMembershipChangeObservable().setChanged();
        return true;
    }

    private void setMetaWorkspace(HortaMetaWorkspace metaWorkspace) {
        if (this.metaWorkspace == metaWorkspace)
            return;
        this.metaWorkspace = metaWorkspace;
        this.metaWorkspace.setSample(TmModelManager.getInstance().getCurrentSample());
        this.metaWorkspace.setTagMetadata(TmModelManager.getInstance().getAllTagMeta());

        getMetaWorkspace().setChanged();
        getMetaWorkspace().notifyObservers();

        // load mesh objects
        if (workspace != null) {
            List<TmObjectMesh> meshList = workspace.getObjectMeshList();

            if (meshList != null) {
                for (TmObjectMesh mesh : meshList) {
                    this.metaWorkspace.addMeshActors(mesh);
                }
            }
        }
    }

    public HortaMetaWorkspace getMetaWorkspace() {
        return metaWorkspace;
    }

    private void repaintHorta() {
        if (getMetaWorkspace() == null)
            return;
        // Below is the way to trigger a repaint, without changing the viewpoint
        getMetaWorkspace().setChanged();
        getMetaWorkspace().notifyObservers();
    }

    // repaint for single neuron changes
    private void repaintHorta(NeuronModel neuron) {
        if (getMetaWorkspace() == null)
            return;
        // Below is the way to trigger a repaint, without changing the viewpoint
        getMetaWorkspace().setChanged();
        getMetaWorkspace().notifyObservers(neuron);
    }

    private NeuronModelAdapter neuronModelForTmGeoAnnotation(TmGeoAnnotation annotation) {
        if (annotation == null)
            return null;
        Long neuronId = annotation.getNeuronId();
        TmNeuronMetadata neuronMetadata = annotationModel.getNeuronFromNeuronID(neuronId);
        return innerList.neuronModelForTmNeuron(neuronMetadata);
    }

    @Override
    public void addObjectMesh(TmObjectMesh mesh) {
        try {
            TmModelManager.getInstance().getCurrentWorkspace().addObjectMesh(mesh);
         //   annotationModel.saveCurrentWorkspace();
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }
    
    @Override
    public void removeObjectMesh(String meshName) {
    }

    @Override
    public void updateObjectMeshName(String oldName, String updatedName) {
        try {
            List<TmObjectMesh> objectMeshes = TmModelManager.getInstance().getCurrentWorkspace().getObjectMeshList();
            for (TmObjectMesh objectMesh : objectMeshes) {
                if (objectMesh.getName().equals(oldName)) {
                    objectMesh.setName(updatedName);
                   // annotationModel.saveCurrentWorkspace();
                    break;
                }
            }
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }

    @Override
    public void neuronBranchReviewed(TmNeuronMetadata neuron, List<TmGeoAnnotation> annList) {
        // determine the neuronvertices for each of these and add them to the model
        NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
        if (!neuronModel.getReviewMode())
            neuronModel.setReviewMode(true);
        List<NeuronVertex> vertexList = new ArrayList<>();
        if (annList!=null && annList.size()>0) {
            for (TmGeoAnnotation annotation : annList) {
                NeuronVertex vertex = neuronModel.getVertexForAnnotation(annotation);
                if (vertex!=null)
                    vertexList.add(vertex);
            }
            neuronModel.addReviewedVertices(vertexList);
        }
        neuronModel.getColorChangeObservable().hasChanged();
        neuronModel.getColorChangeObservable().notifyObservers();
        repaintHorta();
    }

    private class MyTmGeoAnnotationModListener {
        public void annotationAdded(TmGeoAnnotation annotation) {

            LOG.debug("annotationAdded");

            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            // updateEdges(); // Brute force approach reanalyzes all edges            
            // Surgical approach only adds the one new edge
            // (vertex is added implicitly)

            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(annotation);
            if (neuron == null) {
                LOG.error("could not find NeuronModel for newly added TmGeoAnnotation");
                return;
            }

            NeuronVertex newVertex = neuron.addVertex(annotation);
            if (newVertex == null) {
                LOG.error("NeuronModelAdapter.addVertex() returned null");
                return;
            }
            neuron.getGeometryChangeObservable().setChanged(); // set here because its hard to detect otherwise
            // Trigger a Horta repaint  for instant GUI feedback
            // NOTE - assumes this callback is only invoked from one-at-a-time manual addition
            final boolean doRecenterHorta = true;
            if (getMetaWorkspace() != null) {
                if (doRecenterHorta) {
                    // 1) recenter on annotation location in Horta, just like in LVV
                    float recenter[] = newVertex.getLocation();
                    getMetaWorkspace().getVantage().setFocus(recenter[0], recenter[1], recenter[2]);
                    getMetaWorkspace().getVantage().setChanged();
                    getMetaWorkspace().getVantage().notifyObservers();
                }

                // 2) repaint Horta now, to update view without further user interaction
                // Below is the way to trigger a repaint, without changing the viewpoint
                repaintHorta();
                // Emit annotation added signal, to update Horta spatial index
                NeuronVertexCreationObservable addedSignal = neuron.getVertexCreatedObservable();
                addedSignal.setChanged();
               // addedSignal.notifyObservers(new VertexWithNeuron(newVertex, neuron));
            }

            LOG.debug("Adding vertex: {}", newVertex);
         //   spatialIndex.addToIndex(newVertex);
        }

        public void annotationsDeleted(List<TmGeoAnnotation> annotations) {

            LOG.debug("annotationDeleted");

            sanityCheckWorkspace(); // beware of shifting sands beneath us...

            // TODO - surgically remove only edges related to these particular vertices
            updateEdges(); // Brute force approach reanalyzes all edges

            // Create an optimized container of deleted vertices
            Map<NeuronModel, Collection<NeuronVertex>> deletedVerticesByNeuron
                    = new HashMap<>();
            for (TmGeoAnnotation deletedAnnotation : annotations) {
                NeuronList nl = (NeuronList) neurons;
                Long neuronId = deletedAnnotation.getNeuronId();
                if (!nl.hasCachedNeuronId(neuronId))
                    continue; // Never had that neuron instantiated, so ignore
                NeuronModelAdapter neuron = nl.getCachedNeuron(neuronId);
                Long vertexId = deletedAnnotation.getId();
                if (!neuron.hasCachedVertex(vertexId))
                    continue; // Optimization to ignore vertices that were never instantiated
                NeuronVertex vertex = neuron.getVertexForAnnotation(deletedAnnotation);
                // Create container for this neuron vertices, if necessary
                if (!deletedVerticesByNeuron.containsKey(neuron))
                    deletedVerticesByNeuron.put(neuron, new ArrayList<NeuronVertex>());
                deletedVerticesByNeuron.get(neuron).add(vertex);

                LOG.debug("Removing vertex: {}", vertex);
            //    spatialIndex.removeFromIndex(vertex);
            }

            // Send out one signal per neuron
            for (NeuronModel neuron : deletedVerticesByNeuron.keySet()) {
                neuron.getVertexesRemovedObservable().setChanged();
                Collection<NeuronVertex> deletedVertices = deletedVerticesByNeuron.get(neuron);
               // neuron.getVertexesRemovedObservable().notifyObservers(
                 //       new VertexCollectionWithNeuron(deletedVertices, neuron));
            }

            // Repaint Horta now, to update view without further user interaction
            // (but do not recenter, as LVV does not recenter in this situation either)
            repaintHorta();
        }

        public void annotationReparented(TmGeoAnnotation annotation, Long prevNeuronId) {

            LOG.debug("annotationReparented");

            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            updateEdges(); // updateEdges() is required for post-merge update in Horta. TODO: is performance optimization needed here?

            if (innerList.hasCachedNeuronId(prevNeuronId)) {
                NeuronModelAdapter oldNeuronModel = innerList.getCachedNeuron(prevNeuronId);
                NeuronVertex oldVertex = oldNeuronModel.getVertexForAnnotation(annotation);
                if (oldVertex != null) {
                    LOG.debug("Removing vertex: {}", oldVertex);
               //     spatialIndex.removeFromIndex(oldVertex);
                }
            }

            NeuronModelAdapter neuronModel = neuronModelForTmGeoAnnotation(annotation);
            if (neuronModel == null) {
                LOG.error("could not find NeuronModel for newly added TmGeoAnnotation");
                return;
            }
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Adding vertex: {}", neuronVertex);
               // spatialIndex.addToIndex(neuronVertex);
            }

            NeuronVertex reparentedVertex = neuronModel.getVertexForAnnotation(annotation);

            NeuronVertexUpdateObservable signal = neuronModel.getVertexUpdatedObservable();
            signal.setChanged();
           // signal.notifyObservers(new VertexWithNeuron(reparentedVertex, neuronModel));

            repaintHorta();
        }

        public void annotationMoved(TmGeoAnnotation movedAnnotation) {

            LOG.debug("annotationMoved");

            sanityCheckWorkspace();
            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(movedAnnotation);
            if (neuron == null) {
                LOG.warn("Could not find neuron for moved anchor");
                return;
            }
            NeuronVertex movedVertex = neuron.getVertexForAnnotation(movedAnnotation);
            if (movedVertex == null) {
                LOG.info("Skipping moved anchor not yet instantiated in Horta");
                return;
            }

            LOG.debug("Updating vertex: {}", movedVertex);
          //  spatialIndex.updateIndex(movedVertex);

            NeuronVertexUpdateObservable signal = neuron.getVertexUpdatedObservable();
            signal.setChanged();
          //  signal.notifyObservers(new VertexWithNeuron(movedVertex, neuron));

            repaintHorta();
        }

        public void annotationNotMoved(TmGeoAnnotation annotation) {
            LOG.debug("annotationNotMoved");
            // updateEdges();
        }

        public void annotationRadiusUpdated(TmGeoAnnotation annotation) {

            LOG.debug("annotationRadiusUpdated");

            sanityCheckWorkspace();
            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(annotation);
            if (neuron == null) {
                LOG.warn("Could not find neuron for reradiused anchor");
                return;
            }
            NeuronVertex movedVertex = neuron.getVertexForAnnotation(annotation);
            if (movedVertex == null) {
                LOG.info("Skipping reradiused anchor not yet instantiated in Horta");
                return;
            }
            NeuronVertexUpdateObservable signal = neuron.getVertexUpdatedObservable();
            signal.setChanged();
           // signal.notifyObservers(new VertexWithNeuron(movedVertex, neuron));
            repaintHorta();
        }
    }

    @Override
    public void addEditNote(NeuronVertex anchor) {
        if (anchor instanceof NeuronVertexAdapter) {
            TmGeoAnnotation annotation = ((NeuronVertexAdapter) anchor).getTmGeoAnnotation();
           // LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().addEditNote(annotation.getNeuronId(), annotation.getId());
        }
    }

    @Override
    public void addTraceEndNote(NeuronVertex anchor) {
        if (anchor instanceof NeuronVertexAdapter) {
            TmGeoAnnotation annotation = ((NeuronVertexAdapter) anchor).getTmGeoAnnotation();
           // LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNote(annotation.getNeuronId(), annotation.getId(), PredefinedNote.TRACED_END.getNoteText());
        }
    }

    @Override
    public void addUnique1Note(NeuronVertex anchor) {
        if (anchor instanceof NeuronVertexAdapter) {
            TmGeoAnnotation annotation = ((NeuronVertexAdapter) anchor).getTmGeoAnnotation();
           // LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNote(annotation.getNeuronId(), annotation.getId(), PredefinedNote.UNIQUE_1.getNoteText());
        }
    }

    @Override
    public void addUnique2Note(NeuronVertex anchor) {
        if (anchor instanceof NeuronVertexAdapter) {
            TmGeoAnnotation annotation = ((NeuronVertexAdapter) anchor).getTmGeoAnnotation();
           // LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNote(annotation.getNeuronId(), annotation.getId(), PredefinedNote.UNIQUE_2.getNoteText());
        }
    }

    @Override
    public void setSelectMode(boolean select) {
        annotationModel.setSelectMode(select);
    }

    @Override
    public void selectVertex(NeuronVertex anchor) {
        TmGeoAnnotation annotation = getAnnotationForAnchor(anchor);
        if (annotation!=null)
            annotationModel.selectPoint(annotation.getNeuronId(), annotation.getId());
    }

    private class NeuronSetBackgroundAnnotationListener implements BackgroundAnnotationListener {

        GlobalAnnotationListener global;

        public void setGlobal(GlobalAnnotationListener global) {
            this.global = global;
        }

        @Override
        public void neuronModelChanged(TmNeuronMetadata neuron) {
            // Remove all the existing cached vertices for this neuron
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            for (NeuronVertex neuronVertex : neuronModel.getCachedVertexes()) {
                LOG.debug("Removing cached vertex: {}", neuronVertex);
               // spatialIndex.removeFromIndex(neuronVertex);
            }
            // merge in the latest vertices and update the geometry
            neuronModel.mergeNeuronData(neuron);

            // Re-create all the vertices for the neuron, and re-add them to the spatial index
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Re-adding vertex: {}", neuronVertex);
               // spatialIndex.addToIndex(neuronVertex);
            }

            //  repaintHorta(neuronModel);
        }

        @Override
        public void neuronModelCreated(TmNeuronMetadata neuron) {
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Adding vertex: {}", neuronVertex);
            //    spatialIndex.addToIndex(neuronVertex);
            }
            neuronModel.getGeometryChangeObservable().setChanged();
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
        }

        @Override
        public void neuronModelDeleted(TmNeuronMetadata neuron) {
            LOG.info("Neuron deleted: {}", neuron);
            Collection<NeuronVertex> deletedVertices = new ArrayList<>();
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            if (neuronModel==null)
                return;
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Removing vertex: {}", neuronVertex);
         //       spatialIndex.removeFromIndex(neuronVertex);
                deletedVertices.add(neuronVertex);
            }
            neuronModel.getVertexesRemovedObservable().setChanged();
          //  neuronModel.getVertexesRemovedObservable().notifyObservers(
             //       new VertexCollectionWithNeuron(deletedVertices, neuronModel));

            neuronModel.getGeometryChangeObservable().setChanged();
            innerList.removeFromCache(neuron.getId());
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
        }

        @Override
        public void neuronOwnerChanged(TmNeuronMetadata neuron) {
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            neuronModel.setOwnerKey(neuron.getOwnerKey());
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
        }

    }

    private class MyGlobalAnnotationListener implements GlobalAnnotationListener {

        @Override
        public void workspaceUnloaded(final TmWorkspace workspace) {
            //spatialIndex.clear();
        }

        @Override
        public void workspaceLoaded(final TmWorkspace workspace) {
            LOG.info("Workspace loaded");
            setWorkspace(workspace);

            if (workspace == null) {
                //spatialIndex.clear();
            } else {
                final ProgressHandle progress = ProgressHandleFactory.createHandle("Building spatial index");
                progress.setInitialDelay(0);
                progress.start();
                progress.switchToIndeterminate();

                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                  //      spatialIndex.rebuildIndex(innerList);
                    }

                    @Override
                    protected void hadSuccess() {
                        progress.finish();
                        // Let LVV know that index is done  
                        annotationModel.fireSpatialIndexReady(workspace);
                        // load user preferences
                        try {
                            TmViewerManager.getInstance().loadUserPreferences();
                            repaintHorta();
                        } catch (Exception error) {
                            FrameworkAccess.handleException(error);
                        }
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress.finish();
                        FrameworkAccess.handleException(error);
                    }
                };
                worker.execute();
            }
        }

        @Override
        public void spatialIndexReady(TmWorkspace workspace) {
            // Propagate LVV "workspaceLoaded" signal to Horta NeuronSet::membershipChanged signal
            getMembershipChangeObservable().setChanged();
            getNameChangeObservable().setChanged();
            getNameChangeObservable().notifyObservers();
            getMembershipChangeObservable().notifyObservers();
        }

        @Override
        public void neuronCreated(TmNeuronMetadata neuron) {
            LOG.info("Neuron created: {}", neuron);
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Adding vertex: {}", neuronVertex);
               // spatialIndex.addToIndex(neuronVertex);
            }
            neuronModel.getGeometryChangeObservable().setChanged();
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
            repaintHorta(neuronModel);
        }
        
        @Override
        public void bulkNeuronsChanged(List<TmNeuronMetadata> addList, List<TmNeuronMetadata> deleteList) {

            Stopwatch stopwatch = Stopwatch.createStarted();
            LOG.debug("Neurons Updates: Adds: {}, Deletes: {}", addList, deleteList);
            for (TmNeuronMetadata neuron : addList) {
                NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
                neuronModel.getGeometryChangeObservable().setChanged();                
                for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                //    spatialIndex.addToIndex(neuronVertex);
                }
                getMembershipChangeObservable().notifyObservers(neuronModel);
            }
            
            for (TmNeuronMetadata neuron : deleteList) {
                Collection<NeuronVertex> deletedVertices = new ArrayList<>();
                NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
                if (neuron.getOwnerKey().equals(AccessManager.getSubjectKey()))
                    continue;
                for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                    LOG.debug("Removing vertex: {}", neuronVertex);
                  //  spatialIndex.removeFromIndex(neuronVertex);
                    deletedVertices.add(neuronVertex);
                }
                neuronModel.getVertexesRemovedObservable().setChanged();
              //  neuronModel.getVertexesRemovedObservable().notifyObservers(
                     //   new VertexCollectionWithNeuron(deletedVertices, neuronModel));
                neuronModel.getGeometryChangeObservable().setChanged();
                innerList.removeFromCache(neuron.getId());
                getMembershipChangeObservable().setChanged();
                getMembershipChangeObservable().notifyObservers(neuronModel);
            }
            repaintHorta();
            LOG.info("TOTAL HORTA UPDATE: {}",stopwatch.elapsed().toMillis());
            stopwatch.stop();
        }

        @Override
        public void neuronDeleted(TmNeuronMetadata neuron) {
            LOG.info("Neuron deleted: {}", neuron);
            Collection<NeuronVertex> deletedVertices = new ArrayList<>();
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Removing vertex: {}", neuronVertex);
            //    spatialIndex.removeFromIndex(neuronVertex);
                deletedVertices.add(neuronVertex);
            }
            neuronModel.getVertexesRemovedObservable().setChanged();
        //    neuronModel.getVertexesRemovedObservable().notifyObservers(
              //      new VertexCollectionWithNeuron(deletedVertices, neuronModel));

            neuronModel.getGeometryChangeObservable().setChanged();
            innerList.removeFromCache(neuron.getId());
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
            repaintHorta(neuronModel);
        }

        @Override
        public void neuronChanged(TmNeuronMetadata neuron) {

            LOG.info("Neuron changed: {}", neuron);
            NeuronModelAdapter neuronModel = innerList.neuronModelForTmNeuron(neuron);
            
            // Remove all the existing cached vertices for this neuron
            for (NeuronVertex neuronVertex : neuronModel.getCachedVertexes()) {
                LOG.debug("Removing cached vertex: {}", neuronVertex);
             //   spatialIndex.removeFromIndex(neuronVertex);
            }
            // Re-create all the vertices for the neuron, and re-add them to the spatial index
            neuronModel.loadNewVertices(neuron);
            // reload the vertices into the model
            // Re-create all the vertices for the neuron from the neuron passed, not the model (which might be out of date)
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                LOG.debug("Re-adding vertex: {}", neuronVertex);
             //   spatialIndex.addToIndex(neuronVertex);
            }

            // Recreate edges from the updated vertex list
            getMembershipChangeObservable().setChanged();
            getMembershipChangeObservable().notifyObservers(neuronModel);
            repaintHorta(neuronModel);
        }

        @Override
        public void neuronRenamed(TmNeuronMetadata neuron) {
        }

        @Override
        public void neuronsOwnerChanged(List<TmNeuronMetadata> neuronList) {
        }

        @Override
        public void neuronSelected(TmNeuronMetadata neuron) {
        }

        @Override
        public void neuronRadiusUpdated(TmNeuronMetadata neuron) {

            LOG.debug("neuronRadiusUpdated");

            // neuron radius update can only be triggered if there are
            //  annotations, so this should be safe:
            NeuronModelAdapter neuronModel = neuronModelForTmGeoAnnotation(neuron.getRootAnnotations().get(0));
            neuronModel.getGeometryChangeObservable().setChanged();
            neuronModel.getGeometryChangeObservable().notifyObservers();
            repaintHorta(neuronModel);
        }

        private boolean notifyVisibilityChange(NeuronModel neuronModel) {
            neuronModel.getVisibilityChangeObservable().setChanged();
            neuronModel.getVisibilityChangeObservable().notifyObservers();
            return true;
        }
/*
        private boolean updateOneNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style) {
            // log.info("updateOneNeuronStyle");
            if (neuron == null)
                return false;
            if (style == null)
                return false;
            NeuronList nl = (NeuronList) neurons;
            if (!nl.hasCachedNeuronId(neuron.getId()))
                return false; // Don't instantiate the neuron now, if it is not previously instantiated.

            // Update Horta color when LVV color changes
            boolean result = false;
            NeuronModel neuronModel = nl.neuronModelForTmNeuron(neuron);

            Color newColor = style.getColor();
            if (!newColor.equals(neuronModel.getColor())) {
                neuronModel.setColor(newColor);
                neuronModel.getColorChangeObservable().notifyObservers();
                result = true;
            }

            boolean vis = style.isVisible();
            if (vis != neuronModel.isVisible()) {
                neuronModel.setVisible(vis);
                notifyVisibilityChange(neuronModel);
                // neuronModel.getVisibilityChangeObservable().notifyObservers();
                result = true;
            }
            boolean nonInteractable = style.isNonInteractable();
            if (nonInteractable != neuronModel.isNonInteractable()) {
                neuronModel.setNonInteractable(nonInteractable);
                notifyVisibilityChange(neuronModel);
                result = true;
            }
            boolean userToggleRadius = style.isUserToggleRadius();
            if (userToggleRadius != neuronModel.isUserToggleRadius()) {
                neuronModel.setUserToggleRadius(userToggleRadius);
                neuronModel.getGeometryChangeObservable().setChanged();
                neuronModel.getGeometryChangeObservable().notifyObservers();
                repaintHorta();
                result = true;
            }

            return result;
        }

     //   @Override
        public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStylemap) {
            // log.info("neuronStylesChanged");
            if (neuronStylemap == null)
                return;

            // bulk color/visibility change
            boolean bChanged = false;
            for (Map.Entry<TmNeuronMetadata, NeuronStyle> entry : neuronStylemap.entrySet()) {
                if (updateOneNeuronStyle(entry.getKey(), entry.getValue()))
                    bChanged = true;
            }
            if (bChanged)
                repaintHorta();
        }*/

        @Override
        public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {
            repaintHorta();
        }

    }

    private static class NeuronList implements Collection<NeuronModel> {

        // private TmWorkspace workspace;
        // private TmSample sample;
        private NeuronSetAdapter neuronSet;
        private final Map<Long, NeuronModelAdapter> cachedNeurons = new HashMap<>();
        // private NeuronManager annotationModel;
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        NeuronModelAdapter neuronModelForTmNeuron(TmNeuronMetadata tmNeuron) {
            if (tmNeuron == null) return null;
            Long guid = tmNeuron.getId();
            NeuronModelAdapter neuronModelAdapter = cachedNeurons.get(guid);
            if (neuronModelAdapter == null) {
                neuronModelAdapter = new NeuronModelAdapter(tmNeuron, neuronSet);
                cachedNeurons.put(guid, neuronModelAdapter);
            }
            return neuronModelAdapter;
        }

        void removeFromCache(Long guid) {
            if (guid == null) return;
            if (cachedNeurons.containsKey(guid)) {
                cachedNeurons.remove(guid);
            }
        }

        boolean hasCachedNeuronId(Long neuronId) {
            return cachedNeurons.containsKey(neuronId);
        }

        NeuronModelAdapter getCachedNeuron(Long neuronId) {
            return cachedNeurons.get(neuronId);
        }

        @Override
        public int size() {
            if (neuronSet == null)
                return 0;
            if (neuronSet.workspace == null)
                return 0;
            return neuronSet.annotationModel.getNeuronList().size();
        }

        @Override
        public boolean isEmpty() {
            if (neuronSet == null)
                return true;
            if (neuronSet.workspace == null)
                return true;
            return neuronSet.annotationModel.getNeuronList().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (neuronSet == null)
                return false;
            if (neuronSet.workspace == null)
                return false;
            if (neuronSet.annotationModel.getNeuronList().contains(o))
                return true;
            if (!(o instanceof NeuronModelAdapter))
                return false;
            NeuronModelAdapter neuron = (NeuronModelAdapter) o;
            TmNeuronMetadata tmNeuronMetadata = neuron.getTmNeuronMetadata();
            return neuronSet.annotationModel.getNeuronList().contains(tmNeuronMetadata);
        }

        @Override
        public Iterator<NeuronModel> iterator() {
            if ((neuronSet == null) || (neuronSet.workspace == null))
                // return empty iterator
                return new ArrayList<NeuronModel>().iterator();
            final Iterator<TmNeuronMetadata> it = neuronSet.annotationModel.getNeuronList().iterator();
            return new Iterator<NeuronModel>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public NeuronModel next() {

                    TmNeuronMetadata neuron = it.next();
                    return neuronModelForTmNeuron(neuron);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Object[] toArray() {
            NeuronModel[] result = new NeuronModel[size()];
            int i = 0;
            for (NeuronModel neuron : this) {
                result[i] = neuron;
                i++;
            }
            return result;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean add(NeuronModel e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof NeuronModelAdapter))
                return false;

            NeuronModelAdapter neuron = (NeuronModelAdapter) o;
            TmNeuronMetadata tmn = neuron.getTmNeuronMetadata();
            TmNeuronMetadata previousNeuron = TmSelectionState.getInstance().getCurrentNeuron();
            Long neuronId = tmn.getId();
            boolean removingCurrentNeuron = (previousNeuron != null) && (previousNeuron.getId() == neuronId);

            if (!removingCurrentNeuron)
                neuronSet.annotationModel.selectNeuron(tmn);
            try {
                neuronSet.annotationModel.deleteCurrentNeuron();
            } catch (Exception ex) {
                logger.warn("Error deleting neuron", ex);
                return false;
            } finally {
                if (!removingCurrentNeuron) // restore previous selected neuron
                    neuronSet.annotationModel.selectNeuron(previousNeuron);
            }

            cachedNeurons.remove(neuronId);

            return true;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean addAll(Collection<? extends NeuronModel> c) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private boolean wrap(NeuronSetAdapter neuronSet) {
            this.neuronSet = neuronSet;

            // If we get this far, either the annotationModel or the workspace changed,
            // so we should refresh our persistent cached NeuronModelAdapter objects
            Set<Long> oldNeurons = new HashSet<>(cachedNeurons.keySet());
            Set<Long> newNeurons = new HashSet<>();
            boolean neuronMembershipChanged = false;
            for (TmNeuronMetadata tmNeuronMetadata : neuronSet.annotationModel.getNeuronList()) {
                Long newId = tmNeuronMetadata.getId();
                newNeurons.add(newId);
                // Keep our NeuronModel instances persistent, even when the underlying
                // TmNeuronMetadata instance (annoyingly) changes
                if (oldNeurons.contains(newId)) { // we saw this neuron before!
                    NeuronModelAdapter neuron = cachedNeurons.get(newId);
                    neuron.updateWrapping(tmNeuronMetadata);
                } else {
                    neuronMembershipChanged = true;
                }
            }

            // identify obsolete neurons AFTER identifying refreshable neurons
            oldNeurons.removeAll(newNeurons); // just the obsolete neurons
            for (Long obsoleteId : oldNeurons) {
                cachedNeurons.remove(obsoleteId);
                neuronMembershipChanged = true;
            }

            return neuronMembershipChanged; //  || neuronsWereRefreshed;
        }

    }

    class NSALookupListener implements LookupListener {

        @Override
        public void resultChanged(LookupEvent lookupEvent) {
            Collection<? extends HortaMetaWorkspace> allWorkspaces = hortaWorkspaceResult.allInstances();
            if (allWorkspaces.isEmpty())
                return;
            HortaMetaWorkspace metaWorkspace = allWorkspaces.iterator().next();
            setMetaWorkspace(metaWorkspace);
        }
    }
}
