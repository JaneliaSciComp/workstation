package org.janelia.console.viewerapi.commands;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 * Seeds a new neuron with a single root anchor
 * @author brunsc
 */
public class UpdateNeuronAnchorRadiusCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command
{
    private final NeuronModel neuron;
    private final NeuronVertex anchor;
    private final float initialRadius;
    private final float finalRadius;
    
    public UpdateNeuronAnchorRadiusCommand(
            NeuronModel neuron,
            NeuronVertex anchor,
            float initialRadius,
            float finalRadius)
    {
        this.neuron = neuron;
        this.anchor = anchor;
        this.initialRadius = initialRadius;
        this.finalRadius = finalRadius;
    }

    @Override
    public boolean execute() {
        return neuron.updateVertexRadius(anchor, finalRadius);
    }

    @Override
    public String getPresentationName() {
        return "Update Neuron Anchor Radius";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        if (! execute())
            die(); // Something went wrong. This Command object is no longer useful.
    }

    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.updateVertexRadius(anchor, initialRadius);
    }
}
