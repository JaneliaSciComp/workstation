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

package org.janelia.console.viewerapi.model;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;

/**
 * Seeds a new neuron with a single root anchor
 * @author brunsc
 */
public class CreateNeuronCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command
{
    private final String initialNeuronName;
    private final float[] initialCoordinates;
    private final float initialRadius;
    
    private NeuronModel newNeuron = null;
    private final NeuronSet workspace;
    
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
        newNeuron = workspace.createNeuron(initialNeuronName);
        if (newNeuron == null)
            return false;
        NeuronVertex root = newNeuron.appendVertex(
                null, initialCoordinates, initialRadius);
        if (root == null) {
            workspace.remove(newNeuron);
            return false;
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
            if (! workspace.remove(newNeuron)) {
                newNeuron = null;
                die();
            }
        } catch (Exception exc) {
            // Something went wrong. Perhaps this neuron no longer exists
            newNeuron = null;
            die(); // This Command object is no longer useful
        }
    }
}
