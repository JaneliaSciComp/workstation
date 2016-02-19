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

package org.janelia.horta.command;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 *
 * @author brunsc
 */
public class AppendNeuronVertexCommand 
extends AbstractUndoableEdit
implements UndoableEdit 
{
    private final NeuronModel neuron;
    private final NeuronVertex parentVertex;
    private NeuronVertex newVertex = null;
    private final float[] coordinates;
    private final float radius;
    
    public AppendNeuronVertexCommand(
            NeuronModel neuron, 
            NeuronVertex parentVertex,
            float[] micronXyz,
            float radius) 
    {
        this.neuron = neuron;
        this.parentVertex = parentVertex;
        this.coordinates = micronXyz;
        this.radius = radius;
        super.undo(); // Flip initial state to not done yet
    }
    
    // Command-like semantics execute is a synonym for redo()
    public void execute() {
        redo();
    }
    
    public NeuronVertex getAppendedVertex() {
        return newVertex;
    }
    
    @Override
    public String getPresentationName() {
        return "Append Neuron Vertex";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        newVertex = neuron.appendVertex(parentVertex, coordinates, radius);
    }
    
    @Override
    public void undo() {
        super.undo();
        // neuron.deleteVertex(newVertex); // TODO:
    }

}
