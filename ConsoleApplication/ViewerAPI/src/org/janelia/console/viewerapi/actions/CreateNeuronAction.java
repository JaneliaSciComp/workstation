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
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.CreateNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.UndoRedo;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Edit",
        id = "org.janelia.console.viewerapi.actions.CreateNeuronAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateNeuronAction"
)
@ActionReference(path = "Menu/Edit", position = 2100, separatorBefore = 2000)
@Messages("CTL_CreateNeuronAction=Create a New Neuron Model Here...")
public final class CreateNeuronAction implements ActionListener 
{
    private final NeuronCreationContext creationContext;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public CreateNeuronAction(NeuronCreationContext context)
    {
        this.creationContext = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        // TODO: come up with a unique neuron name
        String defaultName = "Neuron 1";

        //  showInputDialog(Component parentComponent, Object message, String title, int messageType, Icon icon, Object[] selectionValues, Object initialSelectionValue)
        Object neuronName = JOptionPane.showInputDialog(
                creationContext.parentWidget,
                "Create new neuron here?",
                "Create new neuron",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defaultName); // default button
        if (neuronName == null) {
            return; // User pressed "Cancel"
        }
        CreateNeuronCommand cmd = new CreateNeuronCommand(
                creationContext.workspace,
                neuronName.toString(),
                creationContext.anchorXyz,
                creationContext.anchorRadius);
        String errorMessage = "Failed to create neuron";
        try {
            if (cmd.execute()) {
                log.info("Neuron created");
                NeuronVertex addedVertex = cmd.getAddedVertex();
                if (addedVertex != null) {
                    NeuronSet workspace = creationContext.workspace;
                    workspace.setPrimaryAnchor(addedVertex);
                    UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                    if (undoRedo != null)
                        undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
                }
            }
        }
        catch (Exception exc) {
            errorMessage += ":\n" + exc.getMessage();
        }
        JOptionPane.showMessageDialog(
                creationContext.parentWidget,
                errorMessage,
                "Failed to create neuron",
                JOptionPane.WARNING_MESSAGE);                
    }
    
    public static class NeuronCreationContext
    {
        private final Component parentWidget;
        private final NeuronSet workspace;
        private final float[] anchorXyz = new float[3];
        private final float anchorRadius;
        
        public NeuronCreationContext(
                Component parentWidget,
                NeuronSet workspace,
                float[] anchorXyz,
                float anchorRadius)
        {
            this.parentWidget = parentWidget;
            this.workspace = workspace;
            System.arraycopy(anchorXyz, 0, this.anchorXyz, 0, 3);
            this.anchorRadius = anchorRadius;
        }
    }
}
