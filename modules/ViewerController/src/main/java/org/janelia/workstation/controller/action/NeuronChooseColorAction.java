package org.janelia.workstation.controller.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.dialog.NeuronColorDialog;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "NeuronChooseColorAction"
)
@ActionRegistration(
        displayName = "Choose neuron color",
        lazy = true
)
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
        SimpleWorker colorSelector = new SimpleWorker() {
            TmNeuronMetadata newNeuron;
            @Override
            protected void doStuff() throws Exception {
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
                    if (neuron.getOwnerKey().equals(AccessManager.getSubjectKey())) {
                        neuron.setColor(color);
                        NeuronManager.getInstance().updateNeuronMetadata(neuron);
                    }
                    NeuronUpdateEvent neuronEvent = new NeuronUpdateEvent();
                    neuronEvent.setNeurons(Arrays.asList(neuron));
                    ViewerEventBus.postEvent(neuronEvent);
                }
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Exception( "Could not set neuron color",
                        error));
            }
        };
        colorSelector.execute();
    }

    public static Color askForNeuronColor(TmNeuronMetadata neuron) {
        Color initColor = TmViewState.getColorForNeuron(neuron.getId());
        if (initColor==null) {
            initColor = TmViewState.generateNewColor(neuron.getId());
        }

        NeuronColorDialog dialog = new NeuronColorDialog(
                null,
                initColor);
        dialog.setVisible(true);
        if (dialog.styleChosen()) {
            return dialog.getChosenColor();
        } else {
            return null;
        }
    }
}
