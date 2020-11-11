package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronToggleAction"
)
@ActionRegistration(
        displayName = "Toggle selected neuron's visibility",
        lazy = true
)
public class NeuronToggleAction extends EditAction {

    public NeuronToggleAction() {
        super("Toggle neuron visibility");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmNeuronMetadata currentNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (currentNeuron!=null)
            TmModelManager.getInstance().getCurrentView().toggleHidden(currentNeuron.getId());
    }
}
