package org.janelia.console.viewerapi.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.ToggleNeuronVisibilityCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to show or hide an existing neuron
 */

public final class ToggleNeuronVisibilityAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronModel neuron;
    private final NeuronSet workspace;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public ToggleNeuronVisibilityAction(
            Component parentWidget,
            NeuronSet workspace,
            NeuronModel neuron)
    {
        super((neuron.isVisible() ? "Hide" : "Show")
               + " Neuron");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        this.neuron = neuron;        
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        ToggleNeuronVisibilityCommand cmd = new ToggleNeuronVisibilityCommand(neuron);
        cmd.setNotify(true);
        boolean wasVisible = neuron.isVisible();
        if (cmd.execute()) {
            log.info("Neuron '{}' {}", neuron.getName(), wasVisible ? "hidden" : "made visible");
            UndoRedo.Manager undoRedo = workspace.getUndoRedo();
            if (undoRedo != null)
                undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
        }
        else {
            String msg = "Failed to " 
                    + (wasVisible ? "hide" : "show")
                    + " neuron '"
                    + neuron.getName()
                    + "'";
            JOptionPane.showMessageDialog(
                    parentWidget, msg, msg,
                    JOptionPane.WARNING_MESSAGE);                
        }
    }
}
