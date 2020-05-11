package org.janelia.workstation.controller.action;

import java.awt.*;
import java.awt.event.ActionEvent;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.dialog.NeuronColorDialog;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.TmViewState;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

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

    /**
     * pop a dialog to choose neuron style; three variants work together to operate
     * from different input sources
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // called from annotation panel neuron gear menu "choose neuron style"
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            return;
        }
        if (TmSelectionState.getInstance().getCurrentNeuron() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "You must select a neuron to set it's color.",
                    "No neuron selected",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            chooseNeuronColor(TmSelectionState.getInstance().getCurrentNeuron());
        }
    }

    public void chooseNeuronColor(final TmNeuronMetadata neuron) {
        // called from neuron list, clicking on color swatch
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            return;
        }
        if (neuron == null) {
            // should not happen
            return;
        }

        Color color = askForNeuronColor(neuron);
        if (color != null) {
            TmViewState.setColorForNeuron(neuron.getId(), color);
        }
    }

    public static Color askForNeuronColor(TmNeuronMetadata neuron) {
        NeuronColorDialog dialog = new NeuronColorDialog(
                (Frame) SwingUtilities.windowForComponent(null),
                TmViewState.getColorForNeuron(neuron.getId()));
        dialog.setVisible(true);
        if (dialog.styleChosen()) {
            return dialog.getChosenColor();
        } else {
            return null;
        }
    }
}
