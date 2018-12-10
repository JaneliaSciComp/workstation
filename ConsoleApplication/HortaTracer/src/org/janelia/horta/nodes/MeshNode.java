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

import java.awt.Event;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class MeshNode extends AbstractNode
{
    private final MeshActor meshActor;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public MeshNode(final MeshActor meshActor) {
        super(Children.create(new MeshChildFactory(), true), Lookups.singleton(meshActor));
        this.meshActor = meshActor;
        String name = meshActor.getMeshName();
        setDisplayName(name);
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/mesh.png");
    }    
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    @Override
    public Action[] getActions (boolean popup) {
        return new Action[] { 
            new DeleteAction(this.meshActor) 
        };
    }
    
    public boolean isVisible() {
        return meshActor.isVisible();
    }
    
    public void setVisible(boolean visible) {
        if (meshActor.isVisible() == visible)
            return;
        meshActor.setVisible(visible);
        triggerRepaint();
    }
    
    public String getName() {
        return meshActor.getMeshName();
    }
    
    public void setName(String name) {
        NeuronTracerTopComponent hortaTracer = NeuronTracerTopComponent.getInstance();
        hortaTracer.updateObjectMeshName(meshActor.getMeshName(), name);        
        meshActor.setMeshName(name);
        setDisplayName(name);
    }
    
    public void triggerRepaint() {
        // logger.info("NeuronNode repaint triggered");
        // Maybe the parent node would have better access to repainting...
        HortaWorkspaceNode parentNode = (HortaWorkspaceNode)getParentNode();
        if (parentNode != null)
            parentNode.triggerRepaint();
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Node.Property prop;
            // size
            prop = new PropertySupport.Reflection(this, boolean.class, "isVisible", "setVisible"); 
            prop.setName("visible"); 
            set.put(prop); 
            
            // name
            prop = new PropertySupport.Reflection(this, String.class, "getName", "setName"); 
            prop.setName("name"); 
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    }
    
    private class DeleteAction extends AbstractAction {
        MeshActor mesh;
        public DeleteAction(MeshActor mesh) {
            putValue (NAME, "DELETE");
            this.mesh = mesh;
        }
        
        @Override
        public void actionPerformed (ActionEvent e) {
            NeuronTracerTopComponent.getInstance().removeMeshActor(mesh);
        }
    }
}
