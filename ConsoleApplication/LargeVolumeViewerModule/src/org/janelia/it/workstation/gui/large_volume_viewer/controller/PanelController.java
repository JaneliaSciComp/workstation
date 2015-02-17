/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuriteTreePanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NoteListPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceNeuronList;

/**
 * This will have access to setters, etc. on the panels, to provide
 * control feeds from external events.
 * @author fosterl
 */
public class PanelController {
    private PanelGlobalListener globalListener;
    private AnnotationPanel annotationPanel;
    private NoteListPanel noteListPanel;
    private NeuriteTreePanel neuriteTreePanel;
    private WorkspaceNeuronList wsNeuronList;
    private LargeVolumeViewerTranslator lvvTranslator;
    
    public PanelController(
            AnnotationPanel annoPanel,
            NoteListPanel noteListPanel,
            NeuriteTreePanel neuriteTreePanel,
            WorkspaceNeuronList wsNeuronList,
            LargeVolumeViewerTranslator lvvTranslator
    ) {
        this.annotationPanel = annoPanel;
        this.noteListPanel = noteListPanel;
        this.neuriteTreePanel = neuriteTreePanel;
        this.wsNeuronList = wsNeuronList;
        this.lvvTranslator = lvvTranslator;

        PanelPanListener ppl = new PanelPanListener();
        this.neuriteTreePanel.setPanListener(ppl);
        this.noteListPanel.setPanListener(ppl);
        this.wsNeuronList.setPanListener(ppl);
        
        PanelTmGeoSelectListener ptgsl = new PanelTmGeoSelectListener();
        this.neuriteTreePanel.setAnnoSelectListener(ptgsl);
    }
    
    public void registerForEvents(AnnotationModel annotationModel) {
        globalListener = new PanelGlobalListener();
        annotationModel.addGlobalAnnotationListener(globalListener);
        PanelNotesUpdateListener pnul = new PanelNotesUpdateListener();
        annotationModel.setNotesUpdateListener(pnul);
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
            noteListPanel.loadWorkspace(workspace);
            wsNeuronList.loadWorkspace(workspace);
        }
        
        @Override
        public void neuronSelected(TmNeuron neuron) {
            neuriteTreePanel.loadNeuron(neuron);
            wsNeuronList.selectNeuron(neuron);
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
            noteListPanel.loadWorkspace(workspace);
        }
        
    }
}
