package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.action.EditAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronCreateAction"
)
@ActionRegistration(
        displayName = "Create a new neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-Insert")
})
public class NeuronCreateAction extends EditAction {
    
    public NeuronCreateAction() {
        super("Create neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      //  AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
   //     annotationMgr.createNeuron();
    }
}
