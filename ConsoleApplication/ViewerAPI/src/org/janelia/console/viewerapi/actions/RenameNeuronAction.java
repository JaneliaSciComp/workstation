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
package org.janelia.console.viewerapi.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.RenameNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to rename an existing neuron
 */

public final class RenameNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronModel neuron;
    private final NeuronSet workspace;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public RenameNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            NeuronModel neuron)
    {
        super("Rename Neuron...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        this.neuron = neuron;        
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        String oldName = neuron.getName();
        String newName = JOptionPane.showInputDialog(parentWidget, 
                "Enter new name for Neuron '" + oldName + "':", 
                oldName);
        if (newName == null)
            return; // User pressed "Cancel"
        if (newName.length() < 1)
            return; // We don't name things with the empty string
        if (newName.equals(oldName))
            return; // Nothing changed
        RenameNeuronCommand cmd = new RenameNeuronCommand(neuron, newName);        
        // cmd.setNotify(true); // TODO: add a renaming signal
        String fromTo =  " from '" + oldName + "' to '" + newName + "'";
        if (cmd.execute()) {
            log.info("Neuron name changed" + fromTo);
            UndoRedo.Manager undoRedo = workspace.getUndoRedo();
            if (undoRedo != null)
                undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
        }
        else {
            JOptionPane.showMessageDialog(
                    parentWidget,
                    "Failed to change neuron name" + fromTo,
                    "Failed to change neuron name" + fromTo,
                    JOptionPane.WARNING_MESSAGE);                
        }
    }
}
