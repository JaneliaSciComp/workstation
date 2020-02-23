package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.AnnotationModel;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

public class RefreshSharedUpdatesAction extends EditAction {

    public RefreshSharedUpdatesAction() {
        super("Refresh Shared Updates");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationModel annotationModel = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().getAnnotationModel();
        annotationModel.refreshNeuronUpdates();
    }
}
