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

import org.janelia.horta.blocks.KtxOctreeBlockTileSource;
import org.janelia.console.viewerapi.OsFilePathRemapper;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.actors.TetVolumeMeshActor;
import org.janelia.horta.blocks.BlockChooser;
import org.janelia.horta.blocks.BlockTileData;
import org.janelia.horta.blocks.BlockTileKey;
import org.janelia.horta.blocks.BlockTileResolution;
import org.janelia.horta.blocks.BlockTileSource;
import org.janelia.horta.blocks.GpuTileCache;
import org.janelia.horta.blocks.OneFineDisplayBlockChooser;
import org.janelia.horta.ktx.KtxData;
import org.janelia.horta.render.NeuronMPRenderer;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create popup menus related to Ktx volume tile blocks.
 * @author brunsc
 */
class KtxBlockMenus {
    
    private BlockTileSource cachedBlockTileSource;
    private boolean preferKtx = true;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    void populateMenus(final HortaMenuContext context) 
    {
        JMenu tilesMenu = new JMenu("Tiles");
        context.topMenu.add(tilesMenu);
        
        tilesMenu.add(new AbstractAction("Load Ktx Tile Here") {
            @Override
            public void actionPerformed(ActionEvent e) {
                BlockTileSource tileSource = context.ktxBlockTileSource;
                if (tileSource == null) {
                    tileSource = cachedBlockTileSource;
                } else {
                    cachedBlockTileSource = tileSource;
                }
                if (tileSource == null) {
                    String folderSelection = JOptionPane.showInputDialog(
                            context.topMenu,
                            "Where is the ktx brain image folder?",
                            "/nobackup2/mouselight/brunsc/ktxtest/2016-07-18b"
                    );
                    if (folderSelection == null) {
                        return; // User cancelled
                    }
                    File folder = new File(folderSelection);
                    if (!folder.exists()) {
                        // Maybe that was a linux path
                        folder = new File(OsFilePathRemapper.remapLinuxPath(folderSelection));
                    }
                    if (!folder.exists()) {
                        JOptionPane.showMessageDialog(
                                context.topMenu,
                                "ERROR: No such file or folder " + folderSelection,
                                "Ktx Folder Not Found",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    logger.info("Ktx source folder = " + folderSelection);
                    try {
                        cachedBlockTileSource = new KtxOctreeBlockTileSource(folder.toURI().toURL());
                        BlockChooser chooser = new OneFineDisplayBlockChooser();
                        GpuTileCache gpuCache = new GpuTileCache(chooser);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        JOptionPane.showMessageDialog(
                                context.topMenu,
                                "ERROR: Error reading ktx folder " + folder,
                                "ERROR: Error reading ktx folder " + folder,
                                JOptionPane.ERROR_MESSAGE);                    
                    }
                    tileSource = cachedBlockTileSource;
                    if (tileSource == null)
                        return;
                }
                Vector3 xyz = context.mouseXyz; // Use mouse location, as opposed to center focus location
                BlockTileResolution resolution = tileSource.getMaximumResolution();
                BlockTileKey key = tileSource.getBlockKeyAt(xyz, resolution);
                try {
                    if (tileSource.blockExists(key)) {
                        BlockTileData block = tileSource.loadBlock(key);
                        TetVolumeActor parentActor = TetVolumeActor.getInstance();
                        TetVolumeMeshActor blockActor = new TetVolumeMeshActor((KtxData) block, parentActor);
                        parentActor.addChild(blockActor);
                        NeuronMPRenderer renderer = context.renderer;
                        if ( ! renderer.containsVolumeActor(parentActor) ) { // just add singleton actor once...
                            parentActor.setBrightnessModel(renderer.getBrightnessModel());
                            renderer.addVolumeActor(parentActor);
                        }
                        context.renderer.setIntensityBufferDirty();
                        context.sceneWindow.getInnerComponent().repaint();
                    }
                    else {
                        JOptionPane.showMessageDialog(
                                context.topMenu,
                                "No tile found at this location " + key,
                                "No tile found at this location " + key,
                                JOptionPane.WARNING_MESSAGE);                        
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    JOptionPane.showMessageDialog(
                            context.topMenu,
                            "ERROR: Error loading ktx block " + key,
                            "ERROR: Error loading ktx block " + key,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });       
        
        tilesMenu.add(new JPopupMenu.Separator());
        
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
                throw new UnsupportedOperationException();
            }
        });
        
        tilesMenu.add(new JMenuItem(
                new AbstractAction("Clear all Volume Blocks")
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                TetVolumeActor.getInstance().getChildren().clear();
                context.renderer.setIntensityBufferDirty();
                context.sceneWindow.getInnerComponent().repaint();
            }
        }));

    }
    
}
