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

import org.janelia.horta.modelapi.NeuroanatomyWorkspace;
import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TooManyListenersException;
import javax.swing.JComponent;
import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.ChoiceView;
import org.openide.explorer.view.IconView;
import org.openide.explorer.view.ListView;
import org.openide.explorer.view.MenuView;
import org.openide.explorer.view.OutlineView;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author Christopher Bruns
 */
@TopComponent.Description(
        preferredID = "HortaSceneEditorTopComponent", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS) 
@TopComponent.Registration( 
        mode = "editor", 
        openAtStartup = false) 
@ActionID( 
        category = "Window", id = "org.janelia.horta.nodes.HortaSceneEditorTopComponent") 
@ActionReference( 
        path = "Menu/Window/Horta") 
@TopComponent.OpenActionRegistration( 
        displayName = "#CTL_HortaSceneEditorAction") 
@NbBundle.Messages({ 
    "CTL_HortaSceneEditorAction=Scene Editor",
    "CTL_HortaSceneEditorTopComponent=Scene Editor",
    "HINT_HortaSceneEditorTopComponent=Horta Scene Editor"
})
// - See more at: https://platform.netbeans.org/tutorials/74/nbm-selection-1.html#sthash.NU9Nsszy.dpufpublic 
public class HortaSceneEditorTopComponent extends TopComponent
implements ExplorerManager.Provider
{
    // private final InstanceContent content = new InstanceContent();
    private final ExplorerManager mgr = new ExplorerManager();
    private final NeuroanatomyWorkspace workspace;
    
    // https://platform.netbeans.org/tutorials/74/nbm-selection-2.html
    private final BeanTreeView treeView = new BeanTreeView();
    // private final OutlineView treeView = new OutlineView(); // allows small area for dnd of parent
    // private final IconView treeView = new IconView(); // I see nothing
    // private final ListView treeView = new ListView(); // I see nothing
    // private final ChoiceView treeView = new ChoiceView(); // allows drop, but I don't understand the rest
    // private final MenuView treeView = new MenuView(); // weird


    /**
     * Creates new form NeuroanatomyWorkspaceEditorTopComponent
     */
    public HortaSceneEditorTopComponent()
    {
        initComponents();
        setName(Bundle.CTL_HortaSceneEditorTopComponent());
        setToolTipText(Bundle.HINT_HortaSceneEditorTopComponent());
        setDisplayName("Horta Scene Editor");
        
        workspace = new BasicNeuroanatomyWorkspace();
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));
        
        setLayout(new BorderLayout());
        add(
               treeView, 
               BorderLayout.CENTER);
        
        // setDisplayName("Horta Scene");
        // mgr.setRootContext(new AbstractNode(Children.create(new NeuroanatomyWorkspaceChildFactory(), true)));
        mgr.setRootContext(new NeuroanatomyWorkspaceNode(workspace));
        
        // Drop SWC files to create new neurons
        DropTargetListener dtl = new DropTargetListener() {
                        
            @Override
            public void dragEnter(DropTargetDragEvent dtde)
            {
                System.out.println("dragEnter");
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde)
            {
                System.out.println("dragOver");
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde)
            {
                System.out.println("dragActionChanged");
            }

            @Override
            public void dragExit(DropTargetEvent dte)
            {
                System.out.println("dragExit");
            }

            @Override
            public void drop(DropTargetDropEvent dtde)
            {
                System.out.println("drop");
                
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                {
                    dtde.rejectDrop();
                    return;
                }
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                Transferable t = dtde.getTransferable();
                try {
                    List<File> fileList = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : fileList) {
                        String extension = FilenameUtils.getExtension(f.getName());
                        if (extension.toUpperCase() == "SWC") {
                            NeuronReconstruction neuron = new BasicNeuronReconstruction(f);
                            workspace.getNeurons().add(neuron);
                        }
                    }
                } catch (UnsupportedFlavorException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
             }
        };

        JComponent dropComponent = this;
        DropTarget dropTarget = dropComponent.getDropTarget();
        if (dropTarget == null) {
            dropComponent.setDropTarget(new DropTarget(dropComponent, dtl));
        }
        else {
            try {
                dropTarget.addDropTargetListener(dtl);
            } catch (TooManyListenersException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager()
    {
        return mgr;
    }
}
