package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronShowAction"
)
@ActionRegistration(
        displayName = "Show neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-S")
})
public class NeuronShowAction extends EditAction {
    
    public NeuronShowAction() {
        super("Show neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annotationMgr.setCurrentNeuronVisibility(true);
    }
}
