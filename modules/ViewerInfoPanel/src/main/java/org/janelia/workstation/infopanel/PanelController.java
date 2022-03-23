package org.janelia.workstation.infopanel;

import java.util.Collection;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.action.OpenTmSampleOrWorkspaceAction;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This will have access to setters, etc. on the panels, to provide
 * control feeds from external events.
 * @author fosterl
 */
public class PanelController {
    private AnnotationPanel annotationPanel;
    private WorkspaceNeuronList wsNeuronList;
    private WorkspaceInfoPanel wsInfoPanel;
    private FilteredAnnotationList filteredAnnotationList;
    private static final Logger log = LoggerFactory.getLogger(PanelController.class);
    
    
    public PanelController(
            AnnotationPanel annoPanel,
            FilteredAnnotationList filteredAnnotationList,
            WorkspaceNeuronList wsNeuronList,
            WorkspaceInfoPanel wsInfoPanel
    ) {
        this.annotationPanel = annoPanel;
        this.filteredAnnotationList = filteredAnnotationList;
        this.wsNeuronList = wsNeuronList;
        this.wsInfoPanel = wsInfoPanel;
        registerForEvents();
    }
    
    public void registerForEvents() {
        Events.getInstance().registerOnEventBus(this);
        ViewerEventBus.registerForEvents(this);
    }

    @Subscribe
    public void workspaceUnloaded(UnloadProjectEvent loadEvent) {
        annotationPanel.loadWorkspace(null);
        filteredAnnotationList.loadWorkspace(null);
        wsNeuronList.loadWorkspace(null);
        wsInfoPanel.loadWorkspace(null);
    }

    @Subscribe
    public void workspaceLoaded(LoadProjectEvent loadEvent) {
        TmWorkspace workspace = loadEvent.getWorkspace();
        annotationPanel.loadWorkspace(workspace);
        filteredAnnotationList.loadWorkspace(workspace);
        wsNeuronList.loadWorkspace(workspace);
        wsInfoPanel.loadWorkspace(workspace);
    }

    @Subscribe
    public void viewerOpened(ViewerOpenEvent openEvent) {
        annotationPanel.viewerOpened(openEvent);
    }

    @Subscribe
    public void viewerClosed(ViewerCloseEvent closeEvent) {
        annotationPanel.viewerClosed(closeEvent);
    }

    @Subscribe
    public void neuronsHidden(NeuronHideEvent event) {
        wsNeuronList.neuronsVisibilityChanged(event.getNeurons());
    }

    @Subscribe
    public void neuronsShown(NeuronUnhideEvent event) {
        wsNeuronList.neuronsVisibilityChanged(event.getNeurons());
    }

    @Subscribe
    public void neuronsCreated(NeuronCreateEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            wsNeuronList.addNeuronToModel(neuron);
        }
    }

    @Subscribe
    public void neuronsDeleted(NeuronDeleteEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            filteredAnnotationList.deleteNeuron(neuron);
            wsNeuronList.deleteFromModel(neuron);
        }
    }

    @Subscribe
    public void neuronsRenamed(NeuronUpdateEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        if (neurons==null)
            return;
        for (TmNeuronMetadata neuron : neurons) {
            if (neuron.getOwnerKey().equals(AccessManager.getSubjectKey()))
                filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.updateModel(neuron);
        }
    }

    @Subscribe
    public void sharedNeuronsUpdated(SharedNeuronUpdateEvent updateEvent) {
        Collection<TmNeuronMetadata> neurons = updateEvent.getNeurons();
        if (neurons==null)
            return;
        for (TmNeuronMetadata neuron : neurons) {
            wsNeuronList.updateNeuron(neuron);
        }
    }

    @Subscribe
    public void neuronsOwnerChanged (NeuronOwnerChangedEvent event) {
        for (TmNeuronMetadata neuron : event.getNeurons()) {
            wsNeuronList.updateModel(neuron);
        }
    }

    @Subscribe
    public void neuronsSelected(SelectionNeuronsEvent selectionEvent) {
        List<DomainObject> neurons = selectionEvent.getItems();
        if (selectionEvent.isClear())
            return;
        for (DomainObject neuron : neurons) {
            // note that we do not really support multiple selections yet, so only
            //  the last neuron in the list will end up selected
            filteredAnnotationList.loadNeuron((TmNeuronMetadata)neuron);
            wsNeuronList.selectNeuron((TmNeuronMetadata)neuron);
        }
    }

    @Subscribe
    public void vertexSelected(SelectionAnnotationEvent selectionEvent) {
        List<TmGeoAnnotation> vertices = (List<TmGeoAnnotation>)selectionEvent.getItems();

        if (vertices.size()==0)
            return;

        // for now only select one vertex; add multiple selection as appropriate
        TmGeoAnnotation vertexSelected = vertices.get(0);
        TmNeuronMetadata neuronSelected =  NeuronManager.getInstance().getNeuronFromNeuronID(vertexSelected.getNeuronId());
        if (neuronSelected==null)
            return;

       // filteredAnnotationList.selectAnnotation(vertexSelected);
    }

    @Subscribe
    public void neuronTagsChanged(NeuronTagsUpdateEvent tagEvent) {
        Collection<TmNeuronMetadata> neurons = tagEvent.getNeurons();
        wsNeuronList.neuronTagsChanged(neurons);
    }

    @Subscribe
    public void neuronSpatialFilterUpdated(NeuronSpatialFilterUpdateEvent spatialEvent) {
        wsNeuronList.updateNeuronSpatialFilter(spatialEvent.isEnabled(), spatialEvent.getDescription());
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        annotationPanel.loadWorkspace(workspace);
        filteredAnnotationList.loadWorkspace(workspace);
        wsNeuronList.loadWorkspace(workspace);
        wsInfoPanel.loadWorkspace(workspace);
    }

    @Subscribe
    public void annotationChanged(AnnotationUpdateEvent annoEvent) {
        Collection<TmGeoAnnotation> vertices = annoEvent.getAnnotations();
        for (TmGeoAnnotation vertex : vertices) {
            filteredAnnotationList.annotationChanged(vertex);
        }
    }

    @Subscribe
    public void annotationCreated(AnnotationCreateEvent annoEvent) {
        Collection<TmGeoAnnotation> vertices = annoEvent.getAnnotations();
        for (TmGeoAnnotation vertex : vertices) {
            filteredAnnotationList.annotationChanged(vertex);
        }
    }

    @Subscribe
    public void notesUpdated(AnnotationNotesUpdateEvent notesEvent) {
        Collection<TmGeoAnnotation> vertices = notesEvent.getAnnotations();
        for (TmGeoAnnotation vertex : vertices) {
            filteredAnnotationList.notesChanged(vertex);
        }
    }

    /* if the user wants their previous workspace loaded, kick off the OpenSampleorWorkspace Action
     for some consistency on how things are initialized
     */
    public void loadRecentWorkspace() {
        OpenTmSampleOrWorkspaceAction recentlyLoadedAction = OpenTmSampleOrWorkspaceAction.get();
        try {
            List<Reference> refs =  FrameworkAccess.getBrowsingController().getRecentlyOpenedHistory();

            DomainModel model = DomainMgr.getDomainMgr().getModel();
            List<DomainObject> children = model.getDomainObjects(refs);
            if (children.size()>0) {
                recentlyLoadedAction.setDomainObject(children.get(0));
                recentlyLoadedAction.performAction();
            }
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }

    }
}
