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

package org.janelia.horta.nodes;

import java.awt.Color;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import org.apache.commons.io.FilenameUtils;
import org.janelia.geometry3d.Vantage;
import org.janelia.horta.modelapi.HortaWorkspace;
import org.janelia.console.viewerapi.model.NeuronReconstruction;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presentation layer for neuron reconstructions in Horta.
 * Following tutorial at https://platform.netbeans.org/tutorials/74/nbm-nodesapi2.html
 * @author Christopher Bruns
 */
public class HortaWorkspaceNode extends AbstractNode
{
    private final HortaWorkspace workspace;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public HortaWorkspaceNode(HortaWorkspace workspace) {
        super(Children.create(new HortaWorkspaceChildFactory(workspace), true), Lookups.singleton(workspace));
        this.workspace = workspace;
        updateDisplayName();
        // The factory is already listening on behalf of the children, 
        // so we just need to update the label, in case the neuron count has changed.
        workspace.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                updateDisplayName();
            }
        });
    }
    
    private void updateDisplayName() {
        setDisplayName("Horta workspace"); //  + " (" + workspace.getNeurons().size() + " neurons)");
    }
    
    public Vantage getVantage() {
        return workspace.getVantage();
    }
    
    @Override
    public PasteType getDropType(final Transferable transferable, int action, int index) {
        return new PasteType() {
            @Override
            public Transferable paste() throws IOException
            {
                System.out.println("Dropping neuron...");
                try {
                    List<File> fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : fileList) {
                        String extension = FilenameUtils.getExtension(f.getName());
                        if ( "SWC".equals(extension.toUpperCase()) ) {
                            NeuronReconstruction neuron = new BasicNeuronReconstruction(f);
                            workspace.getNeurons().add(neuron);
                            workspace.setChanged();
                            workspace.notifyObservers();
                            neuron.getGeometryChangeObservable().setChanged();
                            neuron.getGeometryChangeObservable().notifyObservers();
                        } else {
                        }
                    }
                } catch (UnsupportedFlavorException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return null;
            }
        };
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/brain-icon2.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public Integer getSize() {return workspace.getNeurons().size();}
    
    public Color getBackgroundColor() {
        return workspace.getBackgroundColor();
    }
    
    public void setBackgroundColor(Color color) {
        workspace.setBackgroundColor(color);
        workspace.notifyObservers();
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            // size - number of neurons
            Property prop = new PropertySupport.Reflection(this, Integer.class, "getSize", null); 
            prop.setName("size");
            set.put(prop);
            // color
            prop = new PropertySupport.Reflection(this, Color.class, "getBackgroundColor", "setBackgroundColor");
            prop.setName("color");
            set.put(prop);
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf

    void triggerRepaint()
    {
        // logger.info("Workspace repaint triggered");
        workspace.setChanged();
        workspace.notifyObservers();
    }
    
}
