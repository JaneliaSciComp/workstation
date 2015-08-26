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

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.apache.commons.io.FilenameUtils;
import org.janelia.geometry3d.Vantage;
import org.janelia.horta.modelapi.HortaWorkspace;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.Presenter;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 * Presentation layer for neuron reconstructions in Horta.
 * Following tutorial at https://platform.netbeans.org/tutorials/74/nbm-nodesapi2.html
 * @author Christopher Bruns
 */
public class NeuroanatomyWorkspaceNode extends AbstractNode
{
    private final HortaWorkspace workspace;
    private final NeuroanatomyWorkspaceChildFactory childFactory;
    
    public NeuroanatomyWorkspaceNode(HortaWorkspace workspace) {
        super(Children.create(new NeuroanatomyWorkspaceChildFactory(workspace), true), Lookups.singleton(workspace));
        this.workspace = workspace;
        // Recreate children, so we can keep a handle on the childFactory, for dynamic refresh
        childFactory = new NeuroanatomyWorkspaceChildFactory(workspace);
        setChildren(Children.create(childFactory, true));
        updateNeurons();
    }
    
    private void updateNeurons() {
        setDisplayName("Horta workspace" + " (" + workspace.getNeurons().size() + " neurons)");
        childFactory.publicRefresh(true);
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
                            updateNeurons();
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
    
    @Override
    public Action[] getActions(boolean popup) {
        return new Action[] {
            new AddNeuronAction(),
            new HideWorkspaceAction(),
        };
    }

    private class AddNeuronAction extends AbstractAction
    {
        public AddNeuronAction()
        {
            putValue(NAME, "Create New Neuron");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            workspace.getNeurons().add(new BasicNeuronReconstruction());

            // Refresh display of children
            updateNeurons();

            // JOptionPane.showMessageDialog(null, "New neuron created");
            
        }
    }
    
    private class HideWorkspaceAction extends AbstractAction implements Presenter.Popup
    {
        
        public HideWorkspaceAction()
        {
            putValue(NAME, "Show this workspace");
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (workspace == null) return;
            boolean toggled = ! workspace.isVisible();
            workspace.setVisible(toggled);
        }

        @Override
        public JMenuItem getPopupPresenter()
        {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem();
            if ((workspace != null) && (workspace.isVisible()))
                menuItem.setSelected(true);
            else
                menuItem.setSelected(false);
            menuItem.setAction(this);
            return menuItem;
        }
    }

}
