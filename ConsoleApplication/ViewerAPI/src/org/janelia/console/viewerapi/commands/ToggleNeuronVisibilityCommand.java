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

import org.janelia.console.viewerapi.Command;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 * Applies Command design pattern to the act of manually showing or hiding a neuron
 * @author brunsc
 */
public class ToggleNeuronVisibilityCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command, Notifier
{
    private final NeuronModel neuron;
    private final boolean wasVisible;
    private boolean doNotify = true;
    
    public ToggleNeuronVisibilityCommand(
            NeuronModel neuron) 
    {
        this.neuron = neuron;
        this.wasVisible = neuron.isVisible();
    }
    
    // Command-like semantics execute is almost a synonym for redo()
    @Override
    public boolean execute() {
        neuron.setVisible(! wasVisible);
        if (doesNotify())
            neuron.getVisibilityChangeObservable().notifyObservers();
        return true;
    }
    
    @Override
    public String getPresentationName() {
        if (wasVisible)
            return "Hide Neuron '" + neuron.getName() + "'";
        else
            return "Show Neuron '" + neuron.getName() + "'";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        execute();
    }
    
    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.setVisible(wasVisible);
        if (doesNotify())
            neuron.getVisibilityChangeObservable().notifyObservers();
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
