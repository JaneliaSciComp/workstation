package org.janelia.workstation.controller.action;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeuronNamePrompter {
    /**
     * pop a dialog that asks for a name for a neuron;
     * returns null if the user didn't make a choice;
     * validates neuron name
     */
    public static String promptForNeuronName(String suggestedName) {
        if (suggestedName == null) {
            suggestedName = "";
        }
        String neuronName = (String) JOptionPane.showInputDialog(
                null,
                "Neuron name (may not contain: * \" / \\ < > : | ?):",
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
            if (!isNameValid(neuronName)) {
                JOptionPane.showMessageDialog(
                        null,
                        "Neuron names can't contain the * \" / \\ < > : | ? characters!",
                        "Could not create neuron",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return neuronName;
        }
    }

    private static boolean isNameValid(String name) {
        // we don't allow these characters because they are not valid filenames
        //  for Windows (and Linux and Mac as a much smaller subset of these
        //  characters); we use the neuron name as filename on export

        // yes, you need exactly those slashes in the regex
        Pattern p = Pattern.compile("[\\\\*\"/<>:|?]");
        Matcher m = p.matcher(name);
        return !m.find();
    }
}
