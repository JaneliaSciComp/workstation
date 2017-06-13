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
package org.janelia.horta.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.LoadHortaTileAtFocusAction"
)
@ActionRegistration(
        iconBase = "org/janelia/horta/images/neuronTracerCubic16.png",
        displayName = "#CTL_LoadHortaTileAtFocusAction",
        key = "loadTileAtFocus"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 1300, separatorBefore = 1250),
    @ActionReference(path = "Shortcuts", name = "D-T")
})
@Messages("CTL_LoadHortaTileAtFocusAction=Load Horta Tile At Focus")

public final class LoadHortaTileAtFocusAction 
implements ActionListener
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.info("Load Horta Central Tile Action invoked");
        NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
        if (nttc == null)
            return;
        try {
            nttc.loadPersistentTileAtFocus();
        } catch (IOException ex) {
            // Exceptions.printStackTrace(ex);
            logger.info("Tile load failed");
        }
    }
}

