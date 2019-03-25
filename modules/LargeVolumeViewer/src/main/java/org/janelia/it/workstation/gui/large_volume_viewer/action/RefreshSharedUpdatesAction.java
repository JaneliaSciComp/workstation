package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

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
