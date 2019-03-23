package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronHideOtherNeuronsAction"
)
@ActionRegistration(
        displayName = "Hide others (make only the selected neuron visible)",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-O")
})
public class NeuronHideOthersAction extends EditAction {

    public NeuronHideOthersAction() {
        super("Hide others");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annotationMgr.hideUnselectedNeurons();
    }
}
