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

package org.janelia.horta;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.janelia.horta.actions.LoadHortaTileAtFocusAction;
import org.janelia.horta.actors.TetVolumeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create popup menus related to Ktx volume tile blocks.
 * @author brunsc
 */
class KtxBlockMenuBuilder {

    private boolean preferKtx = true;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public boolean isPreferKtx() {
        return preferKtx;
    }

    void setPreferKtx(boolean doPreferKtx) {
        preferKtx = doPreferKtx;
    }
    
    void populateMenus(final HortaMenuContext context) 
    {
        JMenu tilesMenu = new JMenu("Tiles");
        context.topMenu.add(tilesMenu);
        
         tilesMenu.add(new AbstractAction("Load Horta Tile At Cursor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.info("Load Horta Cursor Tile Action invoked");
                NeuronTracerTopComponent nttc = NeuronTracerTopComponent.getInstance();
                if (nttc == null)
                    return;
                try {
                    nttc.loadPersistentTileAtLocation(context.mouseXyz);
                } catch (IOException ex) {
                    // Exceptions.printStackTrace(ex);
                    logger.info("Tile load failed");
                }
            }
        });       
        
       tilesMenu.add(new AbstractAction("Load Horta Tile At Focus") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoadHortaTileAtFocusAction().actionPerformed(e);
            }
        });
        
        tilesMenu.add(new JPopupMenu.Separator());
        
        /* */
        JCheckBoxMenuItem enableVolumeCacheMenu = new JCheckBoxMenuItem(
                "Prefer rendered Ktx tiles", preferKtx);
        tilesMenu.add(enableVolumeCacheMenu);
        enableVolumeCacheMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                preferKtx = item.isSelected();
                item.setSelected(preferKtx);
            }
        });
        /* */
        
        tilesMenu.add(new JMenuItem(
                new AbstractAction("Clear all Volume Blocks")
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                TetVolumeActor.getInstance().clearAllBlocks();
                context.renderer.setIntensityBufferDirty();
                context.sceneWindow.getInnerComponent().repaint();
            }
        }));

    }
    
}
