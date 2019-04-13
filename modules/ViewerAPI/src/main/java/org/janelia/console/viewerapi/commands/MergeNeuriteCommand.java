package org.janelia.console.viewerapi.commands;

import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combines two separate neurites into one.
 * If the neurites are in separate neurons, the first neuron absorbes the 
 * second neurite.
 * @author brunsc
 */
public class MergeNeuriteCommand 
        extends AbstractUndoableEdit
        implements UndoableEdit, Command
{
    private final NeuronSet neuronSet;
    private final NeuronModel firstNeuron;
    private final NeuronVertex firstAnchor;
    private final NeuronVertex secondAnchor;
    // 
    private NeuronModel secondNeuron;
    private final String secondNeuronName;
    private final Color secondNeuronColor;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public MergeNeuriteCommand(
            NeuronSet neuronSet,
            NeuronVertex secondAnchor,
            NeuronVertex firstAnchor)
    {
        this.neuronSet = neuronSet;
        this.firstAnchor = firstAnchor;
        this.secondAnchor = secondAnchor;
        this.firstNeuron = neuronSet.getNeuronForAnchor(firstAnchor);
        this.secondNeuron = neuronSet.getNeuronForAnchor(secondAnchor);
        secondNeuronName = secondNeuron.getName();
        secondNeuronColor = secondNeuron.getColor();
    }
    
    @Override
    public boolean execute() {
        boolean merged = firstNeuron.mergeNeurite(secondAnchor, firstAnchor);
        if (! merged) {
            JOptionPane.showMessageDialog(
                    null,
                    "merge failed",
                    "merge failed",
                    JOptionPane.WARNING_MESSAGE);           
        }
        return merged;
    }
    
    @Override
    public String getPresentationName() {
        return "Merge Neurites";
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
        log.info("Undo-ing MergeNeurites command");
        if (! firstNeuron.splitNeurite(firstAnchor, secondAnchor)) {
            die();
            return;
        }
        
        neuronSet.setSelectMode(false);
        if (! firstNeuron.equals(secondNeuron)) {
            // What if secondNeuron no longer exists?
            if (! neuronSet.contains(secondNeuron)) {
                log.info("Recreating neuron auto-deleted post merge");
                secondNeuron = neuronSet.createNeuron(secondNeuronName);
                secondNeuron.setColor(secondNeuronColor);
            }
            log.info("Transferring merged neurite back to its original neuron");
            if (! secondNeuron.transferNeurite(secondAnchor)) {
                die();
                return;
            }
        }
        
    }
    
}
