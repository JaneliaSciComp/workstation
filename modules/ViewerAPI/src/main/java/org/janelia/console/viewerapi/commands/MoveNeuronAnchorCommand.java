package org.janelia.console.viewerapi.commands;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * Seeds a new neuron with a single root anchor
 * @author brunsc
 */
public class MoveNeuronAnchorCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command
{
    private final TmNeuronMetadata neuron;
    private final TmGeoAnnotation anchor;
    private final float[] initialCoordinates;
    private final float[] finalCoordinates;
    
    public MoveNeuronAnchorCommand(
            TmNeuronMetadata neuron,
            TmGeoAnnotation anchor,
            float[] destinationXyz)
    {
        this.neuron = neuron;
        this.anchor = anchor;
        this.initialCoordinates = new float[] {
            anchor.getX().floatValue(),
            anchor.getY().floatValue(),
            anchor.getZ().floatValue()
        };
        this.finalCoordinates = new float[] {
            destinationXyz[0],
            destinationXyz[1],
            destinationXyz[2]
        };
    }

    @Override
    public boolean execute() {
        return false;
        //return neuron.moveVertex(anchor, finalCoordinates);
    }

    @Override
    public String getPresentationName() {
        return "Move Neuron Anchor";
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
        //neuron.moveVertex(anchor, initialCoordinates);
    }
}
