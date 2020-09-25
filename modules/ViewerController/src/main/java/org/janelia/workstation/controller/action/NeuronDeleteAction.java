package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "NeuronDeleteAction"
)
@ActionRegistration(
        displayName = "Delete neuron",
        lazy = true
)
public class NeuronDeleteAction extends EditAction {

    public NeuronDeleteAction() {
        super("Delete");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        int nAnnotations = currNeuron.getGeoAnnotationMap().size();
        int ans = JOptionPane.showConfirmDialog(
                null,
                String.format("%s has %d nodes; delete?", currNeuron.getName(), nAnnotations),
                "Delete neuron?",
                JOptionPane.OK_CANCEL_OPTION);
        if (ans == JOptionPane.OK_OPTION) {
            NeuronManager.getInstance().deleteCurrentNeuron();
        }
    }
}
