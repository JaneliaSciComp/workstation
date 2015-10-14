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
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.model.NeuronReconstruction;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.ReconstructionCollection;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class ReconstructionCollectionNode extends AbstractNode
{
    private final ReconstructionCollection neuronList;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public ReconstructionCollectionNode(ReconstructionCollection neuronList) {
        super(Children.create(new NeuronListChildFactory(neuronList), true), null);
        this.neuronList = neuronList;
        setDisplayName(neuronList.getName());
    }
    
    // Allow to drop SWC files on List, to add neurons
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
                            // If no neuron lists are available, create a new one.
                            NeuronReconstruction neuron = new BasicNeuronReconstruction(f);
                            neuronList.add(neuron);
                            neuronList.getMembershipChangeObservable().setChanged();
                            // workspace.setChanged();
                            neuronList.getMembershipChangeObservable().notifyObservers();
                            // neuron.getGeometryChangeObservable().setChanged();
                            // neuron.getGeometryChangeObservable().notifyObservers();
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
        return ImageUtilities.loadImage("org/janelia/horta/images/neuron_group.png");
    }    
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public int getSize() {return neuronList.size();}
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Property prop;
            // size
            prop = new PropertySupport.Reflection(this, int.class, "getSize", null); 
            prop.setName("size"); 
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    }

    private static class NeuronListChildFactory extends ChildFactory<NeuronReconstruction>
    {
        private final ReconstructionCollection neuronList;
        
        public NeuronListChildFactory(ReconstructionCollection neuronList) {
            this.neuronList = neuronList;
        }

        @Override
        protected boolean createKeys(List<NeuronReconstruction> toPopulate)
        {
            toPopulate.addAll(neuronList);
            return true;
        }

        @Override
        protected Node createNodeForKey(NeuronReconstruction key) {
            return new NeuronReconstructionNode(key);
        }
    }
    
}
