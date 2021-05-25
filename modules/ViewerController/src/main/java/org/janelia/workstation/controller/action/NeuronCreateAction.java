package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "NeuronCreateAction"
)
@ActionRegistration(
        displayName = "Create a new neuron",
        lazy = true
)
public class NeuronCreateAction extends EditAction {
    
    public NeuronCreateAction() {
        super("Create neuron");
    }

    public void execute(boolean initVertex, Vec3 vertexLoc) {
       createNeuron(initVertex, vertexLoc);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        createNeuron(false, null);
    }

    private void createNeuron(boolean createInitVertex, Vec3 vertexLoc) {
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // prompt the user for a name, but suggest a standard name
        final String neuronName = promptForNeuronName(NeuronManager.getNextNeuronName());
        if (neuronName != null) {
            // create it:
            SimpleWorker creator = new SimpleWorker() {
                TmNeuronMetadata newNeuron;
                @Override
                protected void doStuff() throws Exception {
                    newNeuron = NeuronManager.getInstance().createNeuron(neuronName);
                }

                @Override
                protected void hadSuccess() {
                    try {
                        if (createInitVertex) {
                            TmGeoAnnotation newAnn = NeuronManager.getInstance().addRootAnnotation(newNeuron, vertexLoc);
                        }
                    } catch (Exception e) {
                        FrameworkAccess.handleException(new Exception( "Could not create neuron",
                                error));
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(new Exception( "Could not create neuron",
                            error));
                }
            };
            creator.execute();
        }
    }

    /**
     * pop a dialog that asks for a name for a neuron;
     * returns null if the user didn't make a choice
     */
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
                        "Could not create neuron",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return neuronName;
        }
    }
}
