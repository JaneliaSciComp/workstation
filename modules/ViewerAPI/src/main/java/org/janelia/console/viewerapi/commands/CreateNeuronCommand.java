package org.janelia.console.viewerapi.commands;

import java.awt.Color;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds a new neuron with a single root anchor
 * @author brunsc
 */
public class CreateNeuronCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command, Notifier
{
    private final NeuronSet workspace;
    private final String initialNeuronName;
    private final float[] initialCoordinates;
    private final float initialRadius;
    private NeuronModel newNeuron = null;
    private NeuronVertex rootVertex = null;
    private NeuronVertex previousParentAnchor = null;
    private boolean doNotify = true;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private Color neuronColor = null;
    
    public CreateNeuronCommand(
            NeuronSet workspace,
            String neuronName,
            float[] anchorXyz,
            float anchorRadius)
    {
        this.initialNeuronName = neuronName;
        this.initialCoordinates = anchorXyz;
        this.initialRadius = anchorRadius;
        this.workspace = workspace;
    }

    @Override
    public boolean execute() {
        previousParentAnchor = workspace.getPrimaryAnchor();
        newNeuron = workspace.createNeuron(initialNeuronName);
        if (newNeuron == null)
            return false;
        if (neuronColor == null)
            neuronColor = newNeuron.getColor(); // store color the first time
        else 
            newNeuron.setColor(neuronColor); // restore color after redo
        rootVertex = newNeuron.appendVertex(
                null, initialCoordinates, initialRadius);
        if (rootVertex == null) {
            workspace.remove(newNeuron);
            return false;
        }
        workspace.setPrimaryAnchor(rootVertex);
        if (doesNotify()) {
            workspace.getMembershipChangeObservable().notifyObservers();
            workspace.getPrimaryAnchorObservable().notifyObservers();
        }
        return true;
    }

    @Override
    public String getPresentationName() {
        return "Create Neuron '" + initialNeuronName + "'";
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
        if (newNeuron == null) {
            die();
            return;
        }
        // Sanity check that neuron remains embryonic
        if (newNeuron.getVertexes().size() > 1) {
            die();
            newNeuron = null;
            return;
        }
        try {
            // First remove the root vertex
            if (rootVertex != null) {
                try {
                    if (newNeuron.deleteVertex(rootVertex)) {
                    }
                    else {
                        die();
                    }
                } catch (Exception exc) {
                    // Something went wrong. Perhaps this anchor no longer exists
                    die(); // This Command object is no longer useful
                }
                rootVertex = null;
            }
            if (workspace.remove(newNeuron)) {
                workspace.setPrimaryAnchor(previousParentAnchor);
                if (doesNotify()) {
                    workspace.getMembershipChangeObservable().notifyObservers();
                    workspace.getPrimaryAnchorObservable().notifyObservers();
                }
            }
            else {
                newNeuron = null;
                die();
            }
        } 
        catch (Exception exc) {
            // Something went wrong. Perhaps this neuron no longer exists
            newNeuron = null;
            die(); // This Command object is no longer useful
        }
    }

    public NeuronVertex getAddedVertex() {
        return rootVertex;
    }
    
    public NeuronModel getNewNeuron() {
        return newNeuron;
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
