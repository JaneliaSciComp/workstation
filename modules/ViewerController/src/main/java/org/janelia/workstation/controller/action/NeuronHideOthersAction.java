package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.action.EditAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronHideOtherNeuronsAction"
)
@ActionRegistration(
        displayName = "Hide others (make only the selected neuron visible)",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "S-H")
})
public class NeuronHideOthersAction extends EditAction {

    public NeuronHideOthersAction() {
        super("Hide others");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    //    AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
     //   annotationMgr.hideUnselectedNeurons();
    }
}
