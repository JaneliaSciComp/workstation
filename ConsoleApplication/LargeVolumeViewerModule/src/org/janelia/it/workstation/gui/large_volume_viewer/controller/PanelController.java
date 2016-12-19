package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationList;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceInfoPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceNeuronList;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * This will have access to setters, etc. on the panels, to provide
 * control feeds from external events.
 * @author fosterl
 */
public class PanelController implements TmGeoAnnotationAnchorListener {
    private PanelGlobalListener globalListener;
    private AnnotationPanel annotationPanel;
    private WorkspaceNeuronList wsNeuronList;
    private WorkspaceInfoPanel wsInfoPanel;
    private LargeVolumeViewerTranslator lvvTranslator;
    private FilteredAnnotationList filteredAnnotationList;
    
    public PanelController(
            AnnotationPanel annoPanel,
            FilteredAnnotationList filteredAnnotationList,
            WorkspaceNeuronList wsNeuronList,
            LargeVolumeViewerTranslator lvvTranslator
    ) {
        this.annotationPanel = annoPanel;
        this.filteredAnnotationList = filteredAnnotationList;
        this.wsNeuronList = wsNeuronList;
        this.lvvTranslator = lvvTranslator;

        PanelPanListener ppl = new PanelPanListener();
        this.filteredAnnotationList.setPanListener(ppl);
        this.wsNeuronList.setPanListener(ppl);
        
        PanelTmGeoSelectListener ptgsl = new PanelTmGeoSelectListener();
        this.filteredAnnotationList.setAnnoSelectListener(ptgsl);

        this.lvvTranslator.addTmGeoAnchorListener(this);
    }
    
    public void registerForEvents(AnnotationModel annotationModel) {
        globalListener = new PanelGlobalListener();
        annotationModel.addGlobalAnnotationListener(globalListener);
        PanelNotesUpdateListener pnul = new PanelNotesUpdateListener();
        annotationModel.setNotesUpdateListener(pnul);
        PanelNeuronSelectedListener pnsl = new PanelNeuronSelectedListener(annotationModel);
        wsNeuronList.setNeuronSelectedListener(pnsl);
    }
    
    public void registerForEvents(AnnotationManager annotationManager) {
        PanelEditNoteRequestedListener penrl = new PanelEditNoteRequestedListener(annotationManager);
        filteredAnnotationList.setEditNoteRequestListener(penrl);
    }
    
    public void registerForEvents(WorkspaceInfoPanel wsip) {
        this.wsInfoPanel = wsip;
    }
    
    public void unregisterForEvents(AnnotationModel annotationModel) {
        annotationModel.removeGlobalAnnotationListener(globalListener);
        annotationModel.setNotesUpdateListener(null);
        globalListener = null;
    }
    
    private class PanelGlobalListener extends GlobalAnnotationAdapter {
        @Override
        public void workspaceLoaded(TmWorkspace workspace) {
            annotationPanel.loadWorkspace(workspace);
            filteredAnnotationList.loadWorkspace(workspace);
            wsNeuronList.loadWorkspace(workspace);
            wsInfoPanel.loadWorkspace(workspace);
        }
        
        @Override
        public void neuronSelected(TmNeuronMetadata neuron) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.selectNeuron(neuron);
        }

        @Override
        public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
            wsNeuronList.neuronStyleChanged(neuron, style);
        }

        @Override
        public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
            wsNeuronList.neuronStylesChanged(neuronStyleMap);
        }

        @Override
        public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {
            wsNeuronList.neuronTagsChanged(neuronList);
        }

    }
    
    private class PanelPanListener implements CameraPanToListener {

        @Override
        public void cameraPanTo(Vec3 location) {
            lvvTranslator.cameraPanTo(location);
        }
        
    }
    
    private class PanelTmGeoSelectListener implements AnnotationSelectionListener {

        @Override
        public void annotationSelected(Long annotationID) {
            lvvTranslator.fireNextParentEvent(annotationID);
        }
        
    }
    
    private class PanelNotesUpdateListener implements NotesUpdateListener {

        @Override
        public void notesUpdated(TmWorkspace workspace) {
            filteredAnnotationList.loadWorkspace(workspace);
        }
        
    }
    
    private class PanelNeuronSelectedListener implements NeuronSelectedListener {

        private AnnotationModel model;
        
        public PanelNeuronSelectedListener(AnnotationModel model) {
            this.model = model;
        }
        
        @Override
        public void selectNeuron(TmNeuronMetadata neuron) {
            model.selectNeuron(neuron);
        }
        
    }
    
    private class PanelEditNoteRequestedListener implements EditNoteRequestedListener {

        private AnnotationManager mgr;
        
        public PanelEditNoteRequestedListener(AnnotationManager mgr) {
            this.mgr = mgr;
        }
        
        @Override
        public void editNote(TmGeoAnnotation annotation) {
            mgr.addEditNote(annotation.getId());
        }
        
    }

    // TmGeoAnnotationAnchorListener methods
    // filtered annotation list, neurite tree list will need to listen
    public void anchorAdded(TmGeoAnnotation annotation) {
        filteredAnnotationList.annotationChanged(annotation);
    }

    public void anchorsAdded(List<TmGeoAnnotation> annotationList) {
        filteredAnnotationList.annotationsChanged(annotationList);
    }

    public void anchorDeleted(TmGeoAnnotation annotation) {
        filteredAnnotationList.annotationChanged(annotation);
    }

    public void anchorReparented(TmGeoAnnotation annotation) {
        filteredAnnotationList.annotationChanged(annotation);
    }

    public void anchorMovedBack(TmGeoAnnotation annotation) {
        filteredAnnotationList.annotationChanged(annotation);
    }

    @Override
    public void anchorMoved(TmGeoAnnotation anchor) {
        filteredAnnotationList.annotationChanged(anchor);
    }

    public void clearAnchors() {
        filteredAnnotationList.annotationsChanged(new ArrayList<TmGeoAnnotation>());
    }
}
