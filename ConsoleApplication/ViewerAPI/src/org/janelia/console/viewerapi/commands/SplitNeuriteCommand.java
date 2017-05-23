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
