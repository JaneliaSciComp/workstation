package org.janelia.horta.controller;

import java.util.ArrayList;

import com.google.common.eventbus.Subscribe;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.janelia.gltools.MeshActor;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.workstation.controller.model.annotations.neuron.VertexCollectionWithNeuron;
import org.janelia.workstation.controller.model.annotations.neuron.VertexWithNeuron;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.horta.TracingInteractor;
import org.janelia.horta.render.NeuronMPRenderer;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.listener.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HortaManager helps synchronize neuron models between Horta and Large Volume Viewer.
 * HortaManager listens for neuron editing changes in a NeuronSet in the Netbeans Lookup
 * and HortaManager broadcasts those edits
 *
 * @author David Schauder
 */
public class HortaManager {

    private TmWorkspace workspace;
    private NeuronTracerTopComponent topComponent;
    private NeuronMPRenderer renderer;
    private TracingInteractor guiManager;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<NeuronCreationListener> neuronCreationListeners = new ArrayList<>();
    private List<NeuronDeletionListener> neuronDeletionListeners = new ArrayList<>();
    private List<NeuronUpdateListener> neuronUpdateListeners = new ArrayList<>();
    private List<NeuronVertexCreationListener> vertexCreationListeners = new ArrayList<>();
    private List<NeuronVertexDeletionListener> vertexDeletionListeners = new ArrayList<>();
    private List<NeuronVertexUpdateListener> vertexUpdateListeners = new ArrayList<>();
    private List<NeuronSelectionListener> neuronSelectionListeners = new ArrayList<>();
    private List<NeuronWorkspaceChangeListener> neuronWorkspaceChangeListeners = new ArrayList<>();
    
    public HortaManager(NeuronTracerTopComponent topComponent, NeuronMPRenderer renderer, TracingInteractor guiManager) {
        workspace = TmModelManager.getInstance().getCurrentWorkspace();
        this.guiManager = guiManager;
        this.topComponent = topComponent;
        this.renderer = renderer;
        guiManager.setDefaultWorkspace(workspace);
        ViewerEventBus.registerForEvents(this);
    }
    
    public void addNeuronCreationListener(NeuronCreationListener listener) {
        neuronCreationListeners.add(listener);
    }

    public void addNeuronUpdateListener(NeuronUpdateListener listener) {
        neuronUpdateListeners.add(listener);
    }

    public void addNeuronDeletionListener(NeuronDeletionListener listener) {
        neuronDeletionListeners.add(listener);
    }
    
    public void addNeuronVertexCreationListener(NeuronVertexCreationListener listener) {
        vertexCreationListeners.add(listener);
    }

    public void addNeuronVertexDeletionListener(NeuronVertexDeletionListener listener) {
        vertexDeletionListeners.add(listener);
    }

    public void addNeuronVertexUpdateListener(NeuronVertexUpdateListener listener) {
        vertexUpdateListeners.add(listener);
    }

    public void addNeuronSelectionListener(NeuronSelectionListener listener) {
        neuronSelectionListeners.add(listener);
    }
    
    void addWorkspaceChangeListener(NeuronWorkspaceChangeListener listener) {
        neuronWorkspaceChangeListeners.add(listener);
    }

    // When Horta TopComponent opens
    public void onOpened() {
        try {
            renderer.clearNeuronReconstructions();
            if (TmModelManager.getInstance().getCurrentSample() == null) {
                return;
            }
            if (TmModelManager.getInstance().getCurrentWorkspace() != null) {
                for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
                    renderer.addNeuronActors(neuron);
                }
            }

            topComponent.initSampleLocation();
            topComponent.redrawNow();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
    
    // When Horta TopComponent closes
    public void onClosed() {
        // strip down all things in the workspace
        renderer.clearNeuronReconstructions();
    }

    @Subscribe
    public void neuronBranchReviewed(NeuronBranchReviewedEvent event) {
        Collection<TmGeoAnnotation> annotations = event.getAnnotations();

        if (annotations.size()>0) {
            TmGeoAnnotation branchNode = annotations.iterator().next();
            List<TmNeuronMetadata> neurons = Arrays.asList(new TmNeuronMetadata[]{
                    NeuronManager.getInstance().getNeuronFromNeuronID(branchNode.getNeuronId())});
            for (NeuronUpdateListener listener: neuronUpdateListeners) {
                listener.neuronsUpdated(neurons);
            }
        }
    }

    @Subscribe
    public void workspaceClosed(UnloadProjectEvent event) {
        try {
            renderer.clearNeuronReconstructions();
            TetVolumeActor.getInstance().clearAllBlocks();
            List<MeshActor> meshActorList = renderer.getMeshActors();
            for (MeshActor meshActor: meshActorList) {
                renderer.removeMeshActor(meshActor);
            }
            guiManager.createContext().clearParent();
            topComponent.redrawNow();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }


    @Subscribe
    public void workspaceLoaded(LoadNeuronsEvent event) {
        try {
            renderer.clearNeuronReconstructions();

            if (TmModelManager.getInstance().getCurrentWorkspace() != null) {
                for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
                    renderer.addNeuronActors(neuron);
                }
            }
            topComponent.initSampleLocation();
            if (TmModelManager.getInstance().getCurrentWorkspace()!=null) {
                topComponent.initColorModel();
                topComponent.initMeshes();
            }
            topComponent.loadStartupPreferences();
            topComponent.redrawNow();
        } catch (Exception e) {
            FrameworkAccess.handleException("Problem loading workspace into Horta", e);
        }
    }

    @Subscribe
    private void neuronUpdate(NeuronUpdateEvent event) {
        for (NeuronUpdateListener listener: neuronUpdateListeners) {
            listener.neuronsUpdated(event.getNeurons());
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void neuronsHide(NeuronHideEvent event) {
        for (NeuronUpdateListener listener: neuronUpdateListeners) {
            listener.neuronsUpdated(event.getNeurons());
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void neuronsUnhide(NeuronUnhideEvent event) {
        for (NeuronUpdateListener listener: neuronUpdateListeners) {
            listener.neuronsUpdated(event.getNeurons());
        }
        topComponent.redrawNow();
    }
    @Subscribe
    private void neuronCreated(NeuronCreateEvent event) {
        for (NeuronCreationListener listener: neuronCreationListeners) {
            listener.neuronsCreated(event.getNeurons());
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void neuronDeleted(NeuronDeleteEvent event) {
        for (NeuronDeletionListener listener: neuronDeletionListeners) {
            listener.neuronsDeleted(event.getNeurons());
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void vertexCreated(AnnotationCreateEvent event) {
        TmGeoAnnotation annotation = event.getAnnotations().iterator().next();
        if (annotation==null)
            return;

        VertexWithNeuron vn = new VertexWithNeuron(annotation, NeuronManager.getInstance().getNeuronFromNeuronID(annotation.getNeuronId()));
        for (NeuronVertexCreationListener listener: vertexCreationListeners) {
            listener.neuronVertexCreated(vn);
        }
        topComponent.redrawNow();
    }


    @Subscribe
    private void vertexDeleted(AnnotationDeleteEvent event) {
        TmGeoAnnotation annotation = event.getAnnotations().iterator().next();
        if (annotation==null)
            return;

        VertexCollectionWithNeuron vn = new VertexCollectionWithNeuron(event.getAnnotations(), NeuronManager.getInstance().getNeuronFromNeuronID(annotation.getNeuronId()));
        for (NeuronVertexDeletionListener listener: vertexDeletionListeners) {
            listener.neuronVertexesDeleted(vn);
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void vertexSelected(SelectionAnnotationEvent event) {
        List<TmGeoAnnotation> annList = event.getItems();
        if (!annList.isEmpty()) {
            TmGeoAnnotation annotation = annList.get(0);
            if (annotation==null)
                return;

            for (NeuronSelectionListener listener: neuronSelectionListeners) {
                listener.vertexSelected(annotation);
            }
            topComponent.redrawNow();
        }
    }

    @Subscribe
    private void selectNeurons(SelectionNeuronsEvent event) {
        if (event.getItems().isEmpty()) {
            return;
        }
        TmNeuronMetadata neuron = (TmNeuronMetadata)event.getItems().iterator().next();
        if (neuron==null)
            return;

        for (NeuronSelectionListener listener: neuronSelectionListeners) {
            listener.neuronSelected(neuron);
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void vertexUpdated(AnnotationUpdateEvent event) {
        TmGeoAnnotation annotation = event.getAnnotations().iterator().next();
        if (annotation==null)
            return;

        VertexWithNeuron vn = new VertexWithNeuron(annotation, NeuronManager.getInstance().getNeuronFromNeuronID(annotation.getNeuronId()));
        for (NeuronVertexUpdateListener listener: vertexUpdateListeners) {
            listener.neuronVertexUpdated(vn);
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void workspaceChanged(WorkspaceUpdateEvent event) {
        for (NeuronWorkspaceChangeListener listener: neuronWorkspaceChangeListeners) {
            listener.workspaceChanged(event.getWorkspace());
        }
        topComponent.redrawNow();
    }

    @Subscribe
    private void spatialFilterUpdated(NeuronSpatialFilterUpdateEvent event) {
        renderer.clearNeuronReconstructions();

        if (TmModelManager.getInstance().getCurrentWorkspace() != null) {
            for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
                renderer.addNeuronActors(neuron);
            }
        }
    }

    TmWorkspace getWorkspace() {
        return workspace;
    }
}
