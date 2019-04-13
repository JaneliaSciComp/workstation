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
        id = "NeuronToggleAction"
)
@ActionRegistration(
        displayName = "Toggle selected neuron's visibility",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-V")
})
public class NeuronToggleAction extends EditAction {

    public NeuronToggleAction() {
        super("Toggle neuron visibility");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annotationMgr.toggleSelectedNeurons();
    }
}
