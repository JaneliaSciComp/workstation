package org.janelia.console.viewerapi.commands;

import org.janelia.console.viewerapi.Command;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 * Applies Command design pattern to the act of manually showing or hiding a neuron
 * @author brunsc
 */
public class ToggleNeuronVisibilityCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command, Notifier
{
    private final NeuronModel neuron;
    private final boolean wasVisible;
    private boolean doNotify = true;
    
    public ToggleNeuronVisibilityCommand(
            NeuronModel neuron) 
    {
        this.neuron = neuron;
        this.wasVisible = neuron.isVisible();
    }
    
    // Command-like semantics execute is almost a synonym for redo()
    @Override
    public boolean execute() {
        neuron.setVisible(! wasVisible);
        if (doesNotify())
            neuron.getVisibilityChangeObservable().notifyObservers();
        return true;
    }
    
    @Override
    public String getPresentationName() {
        if (wasVisible)
            return "Hide Neuron '" + neuron.getName() + "'";
        else
            return "Show Neuron '" + neuron.getName() + "'";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        execute();
    }
    
    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.setVisible(wasVisible);
        if (doesNotify())
            neuron.getVisibilityChangeObservable().notifyObservers();
    }

    @Override
    public void setNotify(boolean doNotify) {
        this.doNotify = doNotify;
    }

    @Override
    public boolean doesNotify() {
        return doNotify;
    }
}
