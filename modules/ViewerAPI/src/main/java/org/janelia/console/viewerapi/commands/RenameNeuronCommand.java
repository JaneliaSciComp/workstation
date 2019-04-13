package org.janelia.console.viewerapi.commands;

import org.janelia.console.viewerapi.Command;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 * Applies Command design pattern to the act of renaming a neuron
 * @author brunsc
 */
public class RenameNeuronCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command
{
    private final NeuronModel neuron;
    private final String newName;
    private final String oldName;
    
    public RenameNeuronCommand(
            NeuronModel neuron,
            String newName) 
    {
        this.neuron = neuron;
        this.newName = newName;
        this.oldName = neuron.getName();
    }
    
    // Command-like semantics execute is almost a synonym for redo()
    @Override
    public boolean execute() {
        neuron.setName(newName);
        return true;
    }
    
    @Override
    public String getPresentationName() {
        return "Rename Neuron '" + oldName + "' to '"+ newName + "'";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        execute();
    }
    
    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.setName(oldName);
    }
}
