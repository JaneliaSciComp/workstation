package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.action.EditAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronHideAction"
)
@ActionRegistration(
        displayName = "Hide neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-H")
})
public class NeuronHideAction extends EditAction {

    public NeuronHideAction() {
        super("Hide neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       // AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
       // annotationMgr.setCurrentNeuronVisibility(false);
    }
}
