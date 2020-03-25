package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.controller.NeuronManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronRenameAction"
)
@ActionRegistration(
        displayName = "Rename neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-R")
})
public class NeuronRenameAction extends EditAction {

    public NeuronRenameAction() {
        super("Rename");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager annotationModel = NeuronManager.getInstance();
        // REFACTOR: COPY OVER RENAME FUNCTIONALITY FROM LVV
        //annotationMgr.renameNeuron();
    }
}
