package org.janelia.console.viewerapi.commands;

import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 * Combines two separate neurites into one.
 * If the neurites are in separate neurons, the first neuron absorbes the 
 * second neurite.
 * @author brunsc
 */
public class SplitNeuriteCommand 
        extends AbstractUndoableEdit
        implements UndoableEdit, Command
{
    private final NeuronModel neuron;
    private final NeuronVertex firstAnchor;
    private final NeuronVertex secondAnchor;
    
    public SplitNeuriteCommand(
            NeuronSet neuronSet,
            NeuronVertex secondAnchor,
            NeuronVertex firstAnchor)
    {
        this.firstAnchor = firstAnchor;
        this.secondAnchor = secondAnchor;
        this.neuron = neuronSet.getNeuronForAnchor(firstAnchor);
    }
    
    @Override
    public boolean execute() {
        boolean bSplit = neuron.splitNeurite(secondAnchor, firstAnchor);
        if (! bSplit) {
            JOptionPane.showMessageDialog(
                    null,
                    "split failed",
                    "split failed",
                    JOptionPane.WARNING_MESSAGE);           
        }
        return bSplit;
    }
    
    @Override
    public String getPresentationName() {
        return "Split Neurite";
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
        boolean bMerged = neuron.mergeNeurite(secondAnchor, firstAnchor);
        if (! bMerged)
            die();
    }
    
}
