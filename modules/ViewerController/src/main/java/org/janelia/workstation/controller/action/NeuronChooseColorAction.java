package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.action.EditAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronChooseColorAction"
)
@ActionRegistration(
        displayName = "Choose neuron color",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-C")
})
public class NeuronChooseColorAction extends EditAction {
    
    public NeuronChooseColorAction() {
        super("Choose neuron color..");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       // AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
       // annotationMgr.chooseNeuronColor();
    }
}
