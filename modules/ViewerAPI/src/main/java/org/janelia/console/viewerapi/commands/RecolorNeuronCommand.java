package org.janelia.console.viewerapi.commands;

import java.awt.Color;
import org.janelia.console.viewerapi.Command;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 * Applies Command design pattern to the act of manually adding one vertex to a neuron
 * @author brunsc
 */
public class RecolorNeuronCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command, Notifier
{
    private final NeuronModel neuron;
    private final Color newColor;
    private final Color oldColor;
    private boolean doNotify = true;
    
    public RecolorNeuronCommand(
            NeuronModel neuron,
            Color newColor,
            Color originalColor) 
    {
        this.neuron = neuron;
        this.newColor = newColor;
        this.oldColor = originalColor;
    }
    
    // Command-like semantics execute is almost a synonym for redo()
    @Override
    public boolean execute() {
        neuron.setColor(newColor);
        if (doesNotify())
            neuron.getColorChangeObservable().notifyObservers();
        return true;
    }
    
    private static String nameForColor(Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return hex;
    }
    
    @Override
    public String getPresentationName() {
        return "Color Neuron " + neuron.getName() + " " + nameForColor(newColor);
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        execute();
    }
    
    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.setColor(oldColor);
        if (doesNotify())
            neuron.getColorChangeObservable().notifyObservers();
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
