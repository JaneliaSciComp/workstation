/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
