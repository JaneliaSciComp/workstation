package org.janelia.console.viewerapi.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to delete an existing neuron.
 */

public final class DeleteNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronSet workspace;
    private final NeuronModel doomedNeuron;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public DeleteNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            NeuronModel doomedNeuron)
    {
        super("Delete This Neuron '" + doomedNeuron.getName() + "'...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        this.doomedNeuron = doomedNeuron;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        String warning = "Really DELETE '" +
                doomedNeuron.getName() + 
                "'?\nContaining " +
                doomedNeuron.getVertexes().size() +
                " anchors?" +
                "\n !!! WARNING !!!" +
                "\n THIS CANNOT BE UNDONE";
        int result = JOptionPane.showConfirmDialog(
                parentWidget,
                warning, 
                "Really DELETE '" + doomedNeuron.getName() + "'?",
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.WARNING_MESSAGE);
        
        if (result != JOptionPane.OK_OPTION)
            return; // User pressed cancel
        
        // Actually delete the neuron
        if (workspace.remove(doomedNeuron)) {
            UndoRedo.Manager undoRedo = workspace.getUndoRedo();
            if (undoRedo != null)
                undoRedo.discardAllEdits(); // Deleting a neuron screws previous edits.
            workspace.getMembershipChangeObservable().notifyObservers();
        }
        else {
            JOptionPane.showMessageDialog(
                    parentWidget,
                    "Failed to delete neuron '" + doomedNeuron.getName() + "'",
                    "Failed to delete neuron '" + doomedNeuron.getName() + "'",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

}
