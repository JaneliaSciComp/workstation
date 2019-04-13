package org.janelia.console.viewerapi.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.RenameNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to rename an existing neuron
 */

public final class RenameNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronModel neuron;
    private final NeuronSet workspace;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public RenameNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            NeuronModel neuron)
    {
        super("Rename Neuron...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        this.neuron = neuron;        
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        String oldName = neuron.getName();
        String newName = JOptionPane.showInputDialog(parentWidget, 
                "Enter new name for Neuron '" + oldName + "':", 
                oldName);
        if (newName == null)
            return; // User pressed "Cancel"
        if (newName.length() < 1)
            return; // We don't name things with the empty string
        if (newName.equals(oldName))
            return; // Nothing changed
        RenameNeuronCommand cmd = new RenameNeuronCommand(neuron, newName);        
        // cmd.setNotify(true); // TODO: add a renaming signal
        String fromTo =  " from '" + oldName + "' to '" + newName + "'";
        if (cmd.execute()) {
            log.info("Neuron name changed" + fromTo);
            UndoRedo.Manager undoRedo = workspace.getUndoRedo();
            if (undoRedo != null)
                undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
        }
        else {
            JOptionPane.showMessageDialog(
                    parentWidget,
                    "Failed to change neuron name" + fromTo,
                    "Failed to change neuron name" + fromTo,
                    JOptionPane.WARNING_MESSAGE);                
        }
    }
}
