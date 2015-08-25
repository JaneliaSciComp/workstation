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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.janelia.horta.modelapi.NeuroanatomyWorkspace;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;

/**
 * Presentation layer for neuron reconstructions in Horta.
 * Following tutorial at https://platform.netbeans.org/tutorials/74/nbm-nodesapi2.html
 * @author Christopher Bruns
 */
public class NeuroanatomyWorkspaceNode extends AbstractNode
{
    private final NeuroanatomyWorkspace workspace;
    private final NeuroanatomyWorkspaceChildFactory factory;
    
    public NeuroanatomyWorkspaceNode(NeuroanatomyWorkspace workspace) {
        super(Children.create(new NeuroanatomyWorkspaceChildFactory(workspace), true), Lookups.singleton(workspace));
        setDisplayName("Neuroanatomy workspace");
        this.workspace = workspace;
        // Recreate children, so we can keep a handle on the factory, for dynamic refresh
        factory = new NeuroanatomyWorkspaceChildFactory(workspace);
        setChildren(Children.create(factory, true));
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
            factory.publicRefresh(true);

            JOptionPane.showMessageDialog(null, "New neuron created");
            
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
