package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.action.EditAction;

public class RefreshSharedUpdatesAction extends EditAction {

    public RefreshSharedUpdatesAction() {
        super("Refresh Shared Updates");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager annotationModel = TmViewerManager.getInstance().getNeuronManager();
        //annotationModel.refreshNeuronUpdates();
    }
}
