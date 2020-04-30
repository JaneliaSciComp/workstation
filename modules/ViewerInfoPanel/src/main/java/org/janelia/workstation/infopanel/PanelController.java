package org.janelia.workstation.infopanel;

import java.util.Collection;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.controller.AnnotationCategory;
import org.janelia.workstation.controller.EventBusRegistry;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.eventbus.AnnotationEvent;
import org.janelia.workstation.controller.eventbus.LoadEvent;
import org.janelia.workstation.controller.eventbus.SelectionEvent;
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
            WorkspaceNeuronList wsNeuronList
    ) {
        this.annotationPanel = annoPanel;
        this.filteredAnnotationList = filteredAnnotationList;
        this.wsNeuronList = wsNeuronList;
        registerForEvents();
    }
    
    public void registerForEvents() {
        EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SAMPLEWORKSPACE).register(this);
        EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.ANNOTATION).register(this);
        EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SELECTION).register(this);
    }

    public void workspaceUnloaded(LoadEvent loadEvent) {
        annotationPanel.loadWorkspace(null);
        filteredAnnotationList.loadWorkspace(null);
        wsNeuronList.loadWorkspace(null);
        wsInfoPanel.loadWorkspace(null);
    }

    @Subscribe
    public void workspaceLoaded(LoadEvent loadEvent) {
        if (loadEvent.getType()!= LoadEvent.Type.PROJECT_COMPLETE || loadEvent.getWorkspace()==null)
            return;

        TmWorkspace workspace = loadEvent.getWorkspace();
        annotationPanel.loadWorkspace(workspace);
        filteredAnnotationList.loadWorkspace(workspace);
        wsNeuronList.loadWorkspace(workspace);
        wsInfoPanel.loadWorkspace(workspace);
    }

    @Subscribe
    public void neuronsCreated(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.CREATE || annoEvent.getCategory()!= AnnotationCategory.NEURON)
            return;

        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            wsNeuronList.addNeuronToModel(neuron);
        }
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        wsNeuronList.loadWorkspace(workspace);
    }

    @Subscribe
    public void neuronsDeleted(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.DELETE || annoEvent.getCategory()!= AnnotationCategory.NEURON)
            return;

        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.deleteFromModel(neuron);
        }
    }

    @Subscribe
    public void neuronsRenamed(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.RENAME || annoEvent.getCategory()!= AnnotationCategory.NEURON)
            return;

        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        for (TmNeuronMetadata neuron : neurons) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.updateModel(neuron);
        }
    }

    @Subscribe
    public void neuronsSelected(SelectionEvent selectionEvent) {
        if (selectionEvent.getType()!= SelectionEvent.Type.SELECT || selectionEvent.getCategory()!= AnnotationCategory.NEURON)
            return;

        List<DomainObject> neurons = selectionEvent.getItems();
        for (DomainObject neuron : neurons) {
            filteredAnnotationList.loadNeuron((TmNeuronMetadata)neuron);
            wsNeuronList.selectNeuron((TmNeuronMetadata)neuron);
        }
    }

    @Subscribe
    public void neuronTagsChanged(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.UPDATE|| annoEvent.getCategory()!= AnnotationCategory.TAG)
            return;

        Collection<TmNeuronMetadata> neurons = annoEvent.getNeurons();
        wsNeuronList.neuronTagsChanged(neurons);
    }

    @Subscribe
    public void neuronSpatialFilterUpdated(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.SPATIAL_FILTER || annoEvent.getCategory()!= AnnotationCategory.NEURON)
            return;

        wsNeuronList.updateNeuronSpatialFilter(annoEvent.isEnabled(), annoEvent.getDescription());
    }

    @Subscribe
    public void annotationChanged(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.UPDATE || annoEvent.getCategory()!= AnnotationCategory.VERTEX)
            return;

        Collection<TmGeoAnnotation> vertices = annoEvent.getVertices();
        for (TmGeoAnnotation vertex : vertices) {
            filteredAnnotationList.annotationChanged(vertex);
        }
    }

    @Subscribe
    public void notesUpdated(AnnotationEvent annoEvent) {
        if (annoEvent.getType()!= AnnotationEvent.Type.UPDATE || annoEvent.getCategory()!= AnnotationCategory.NOTE)
            return;

        Collection<TmGeoAnnotation> vertices = annoEvent.getVertices();
        for (TmGeoAnnotation vertex : vertices) {
            filteredAnnotationList.notesChanged(vertex);
        }
    }

        public void editNote(TmGeoAnnotation annotation) {
          //  mgr.addEditNote(annotation.getNeuronId(), annotation.getId());
        }
}
