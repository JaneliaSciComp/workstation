package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmOperation;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Horta",
        id = "NeuronDeleteAction"
)
@ActionRegistration(
        displayName = "Delete neuron",
        lazy = true
)
public class NeuronDeleteAction extends EditAction {

    // be careful to set this to null after any use (
    private TmNeuronMetadata targetNeuron;

    public NeuronDeleteAction() {
        this(null);
    }

    public NeuronDeleteAction(TmNeuronMetadata targetNeuron) {
        super("Delete");
        this.targetNeuron = targetNeuron;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        long startTime = System.currentTimeMillis();
        if (targetNeuron == null) {
            targetNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
            if (targetNeuron == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "No selected neuron!",
                        "No neuron to delete!",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (!targetNeuron.getOwnerKey().equals(AccessManager.getSubjectKey())) {
            JOptionPane.showMessageDialog(null,
                    "Unable to delete this neuron",
                    "Can't Delete Non-owned Neuron",
                    JOptionPane.WARNING_MESSAGE);
            this.targetNeuron = null;
            return;
        }
        int nAnnotations = targetNeuron.getGeoAnnotationMap().size();
        int ans = JOptionPane.showConfirmDialog(
                null,
                String.format("%s has %d nodes; delete?", targetNeuron.getName(), nAnnotations),
                "Delete neuron?",
                JOptionPane.OK_CANCEL_OPTION);
        if (ans == JOptionPane.OK_OPTION) {
            NeuronManager.getInstance().deleteNeuron(targetNeuron);
        }
        this.targetNeuron = null;
        long endTime = System.currentTimeMillis();
        TmViewerManager.getInstance().logOperation(TmOperation.Activity.DELETE_NEURON,
                null, endTime-startTime);
    }
}
