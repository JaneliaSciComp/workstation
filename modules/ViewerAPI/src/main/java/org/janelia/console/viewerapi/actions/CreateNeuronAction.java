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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.CreateNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to create a new neuron rooted at a particular XYZ location.
 */

public final class CreateNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronSet workspace;
    private final float[] anchorXyz = new float[3];
    private final float anchorRadius;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public CreateNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            float[] anchorXyz,
            float anchorRadius)
    {
        super("Create a New Neuron Model Here...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        System.arraycopy(anchorXyz, 0, this.anchorXyz, 0, 3);
        this.anchorRadius = anchorRadius;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        // come up with a unique neuron name
        String defaultName = getNextNeuronName();

        // ask the user to confirm creation, and to review name
        Object neuronName = JOptionPane.showInputDialog(
                parentWidget,
                "Create new neuron here?",
                "Create new neuron",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defaultName); // default button
        if (neuronName == null) {
            return; // User pressed "Cancel"
        }
        String errorMessage = "Failed to create neuron";
        try {
            CreateNeuronCommand cmd = new CreateNeuronCommand(
                    workspace,
                    neuronName.toString(),
                    anchorXyz,
                    anchorRadius);
            cmd.setNotify(true); // Because it's a top-level Command now
            if (cmd.execute()) {
                log.info("Neuron created");
                UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                if (undoRedo != null)
                    undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
                return;
            }
        }
        catch (Exception exc) {
            errorMessage += ":\n" + exc.getMessage();
        }
        JOptionPane.showMessageDialog(
                parentWidget,
                errorMessage,
                "Failed to create neuron",
                JOptionPane.WARNING_MESSAGE);                
    }    
    
    /**
     * Lifted/modified from LVV AnnotationManager.getNextNeuronName
     * given a workspace, return a new generic neuron name (probably something
     * like "Neuron 12", where the integer is based on whatever similarly
     * named neurons exist already)
     */
    private String getNextNeuronName() 
    {
        // go through existing neuron names; try to parse against
        //  standard template; remember largest integer found
        Pattern pattern = Pattern.compile("Neuron[ _]([0-9]+)");
        Long maximum = 0L;
        for (NeuronModel neuron : workspace) {
            if (neuron.getName() == null) {
                // skip unnamed neurons
                continue;
            }
            Matcher matcher = pattern.matcher(neuron.getName());
            if (matcher.matches()) {
                Long index = Long.parseLong(matcher.group(1));
                if (index > maximum)
                    maximum = index;
            }
        }
        return String.format("Neuron %d", maximum + 1);
    }

}
