package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.NeuronManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronDeleteAction"
)
@ActionRegistration(
        displayName = "Delete neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-Delete")
})
public class NeuronDeleteAction extends EditAction {

    public NeuronDeleteAction() {
        super("Delete");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager.getInstance().deleteCurrentNeuron();
}
}
