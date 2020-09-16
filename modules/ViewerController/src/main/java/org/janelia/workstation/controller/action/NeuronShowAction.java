package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.action.EditAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronShowAction"
)
@ActionRegistration(
        displayName = "Show neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "S")
})
public class NeuronShowAction extends EditAction {
    
    public NeuronShowAction() {
        super("Show neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        //annotationMgr.setCurrentNeuronVisibility(true);
    }
}
