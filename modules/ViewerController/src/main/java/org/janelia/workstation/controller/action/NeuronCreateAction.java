package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronCreateAction"
)
@ActionRegistration(
        displayName = "Create a new neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-Insert")
})
public class NeuronCreateAction extends EditAction {
    
    public NeuronCreateAction() {
        super("Create neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // prompt the user for a name, but suggest a standard name
        final String neuronName = promptForNeuronName(getNextNeuronName());

        if (neuronName != null) {
            // create it:
            SimpleWorker creator = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    NeuronManager.getInstance().createNeuron(neuronName);
                }

                @Override
                protected void hadSuccess() {
                    // nothing here, annModel emits its own signals
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
     * given a workspace, return a new generic neuron name (probably something
     * like "New neuron 12", where the integer is based on whatever similarly
     * named neurons exist already)
     */
    private String getNextNeuronName() {
        // go through existing neuron names; try to parse against
        //  standard template; create list of integers found
        ArrayList<Long> intList = new ArrayList<Long>();
        Pattern pattern = Pattern.compile("Neuron[ _]([0-9]+)");
        for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
            if (neuron.getName() != null) {
                Matcher matcher = pattern.matcher(neuron.getName());
                if (matcher.matches()) {
                    intList.add(Long.parseLong(matcher.group(1)));
                }
            }
        }

        // construct new name from standard template; use largest integer
        //  found + 1; starting with max = 0 has the effect of always starting
        //  at at least 1, if anyone has named their neurons with negative numbers
        Long maximum = 0L;
        if (intList.size() > 0) {
            for (Long l : intList) {
                if (l > maximum) {
                    maximum = l;
                }
            }
        }
        return String.format("Neuron %d", maximum + 1);
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
