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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.SelectPrimaryAnchorCommand;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SelectParentAnchorAction extends AbstractAction implements Action
{
    private final NeuronSet workspace;
    private final NeuronVertex newParentAnchor;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public SelectParentAnchorAction(NeuronSet workspace, NeuronVertex anchor)
    {
        super("Set Anchor As Parent");
        this.workspace = workspace;
        this.newParentAnchor = anchor;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        if (workspace == null)
            return;
        SelectPrimaryAnchorCommand cmd = new SelectPrimaryAnchorCommand(
                workspace,
                newParentAnchor);
        cmd.setNotify(true); // Top-level Commands notify their listeners
        try {
            if (cmd.execute()) {
                // Actions, like this one, are responsible for the undo/redo stack
                // log.info("Parent anchor selected");
                //UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                //if (undoRedo != null)
                 //   undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
            }
        }
        catch (Exception exc) {
            // log.info("Parent anchor selection failed");
        }              
    }
}
