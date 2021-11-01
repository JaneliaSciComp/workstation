package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "NeuronRenameAction"
)
@ActionRegistration(
        displayName = "Rename neuron",
        lazy = true
)
public class NeuronRenameAction extends EditAction {

    private TmNeuronMetadata targetNeuron;

    public NeuronRenameAction() {
        this(null);
    }

    public NeuronRenameAction(TmNeuronMetadata targetNeuron) {
        super("Rename");
        this.targetNeuron = targetNeuron;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager annotationModel = NeuronManager.getInstance();

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

        final String neuronName = promptForNeuronName(targetNeuron.getName());
        if (neuronName == null) {
            return;
        }

        SimpleWorker renamer = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.renameCurrentNeuron(neuronName);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, annModel emits its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Exception( "Error while moving neurite!",
                        error));
            }
        };
        renamer.execute();
    }

    String promptForNeuronName(String suggestedName) {
        if (suggestedName == null) {
            suggestedName = "";
        }
        String neuronName = (String) JOptionPane.showInputDialog(
                null,
                "Neuron name:",
                "Name neuron",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list; absent = freeform
                suggestedName);
        if (neuronName == null || neuronName.length() == 0) {
            return null;
        } else {
            // turns out ? or * will mess with Java's file dialogs
            //  (something about how file filters works)
            if (neuronName.contains("?") || neuronName.contains("*")) {
                JOptionPane.showMessageDialog(
                        null,
                        "Neuron names can't contain the ? or * characters!",
                        "Could not rename neuron",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return neuronName;
        }
    }
}
