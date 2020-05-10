package org.janelia.workstation.infopanel;

import java.util.Collection;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
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
        if (loadEvent.isSample())
            return;

        TmWorkspace workspace = loadEvent.getWorkspace();
        annotationPanel.loadWorkspace(workspace);
        filteredAnnotationList.loadWorkspace(workspace);
        wsNeuronList.loadWorkspace(workspace);
        wsInfoPanel.loadWorkspace(workspace);
    }

    @Subscribe
    public void neuronsCreated(NeuronCreateEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            wsNeuronList.addNeuronToModel(neuron);
        }
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        wsNeuronList.loadWorkspace(workspace);
    }

    @Subscribe
    public void neuronsDeleted(NeuronDeleteEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.deleteFromModel(neuron);
        }
    }

    @Subscribe
    public void neuronsRenamed(NeuronUpdateEvent annoEvent) {
        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.updateModel(neuron);
        }
    }

    @Subscribe
    public void neuronsSelected(SelectionNeuronsEvent selectionEvent) {
        List<DomainObject> neurons = selectionEvent.getItems();
        for (DomainObject neuron : neurons) {
            filteredAnnotationList.loadNeuron((TmNeuronMetadata)neuron);
            wsNeuronList.selectNeuron((TmNeuronMetadata)neuron);
        }
    }

    @Subscribe
    public void neuronTagsChanged(NeuronTagsUpdateEvent tagEvent) {
        Collection<TmNeuronMetadata> neurons = tagEvent.getNeurons();
        wsNeuronList.neuronTagsChanged(neurons);
    }

    @Subscribe
    public void neuronSpatialFilterUpdated(NeuronSpatialFilterUpdateEvent spatialEvent) {
        wsNeuronList.updateNeuronSpatialFilter(spatialEvent.isEnabled(), spatialEvent.getDescription());
    }

    @Subscribe
    public void annotationChanged(AnnotationUpdateEvent annoEvent) {
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
}
