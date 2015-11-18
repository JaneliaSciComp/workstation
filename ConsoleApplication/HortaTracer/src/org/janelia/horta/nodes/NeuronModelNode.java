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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.geometry3d.Box3;
// import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.VantageInterface;
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
public class NeuronModelNode extends AbstractNode
{
    private final NeuronModel neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public NeuronModelNode(NeuronModel neuron) {
        super(Children.create(new NeuronModelChildFactory(neuron), true), Lookups.singleton(neuron));
        setDisplayName(neuron.getName()); //  + " (" + neuron.getVertexes().size() + " vertices)");
        this.neuron = neuron;
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/neuron1.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    private VantageInterface getVantage() {
        Node node = this;
        while (node != null) {
            node = node.getParentNode();
            if (node instanceof HortaWorkspaceNode) {
                return ((HortaWorkspaceNode)node).getVantage();
            }
        }
        return null;
    }
    
    @Override
    public Action[] getActions(boolean popup) {
        List<Action> result = new ArrayList<>();
        // Maybe expose "center on" action
        // 0 - is there a camera to actually center on?
        final VantageInterface vantage = getVantage();
        if ((vantage != null) && neuron.getVertexes().size() > 0) {
            result.add(new CenterOnNeuronAction(neuron, vantage));
        }
        return result.toArray(new Action[result.size()]);
    }
    
    public int getSize() {return neuron.getVertexes().size();}
    public boolean isVisible() {return neuron.isVisible();}
    public void setVisible(boolean visible) {
        if (neuron.isVisible() == visible)
            return;
        neuron.setVisible(visible);
        neuron.getVisibilityChangeObservable().notifyObservers();
        triggerRepaint();
    }
    public Color getColor() {return neuron.getColor();}
    public void setColor(Color color) {
        if (neuron.getColor().equals(color))
            return;
        // logger.info("NeuronNode color set to "+color);
        neuron.setColor(color);
        neuron.getColorChangeObservable().notifyObservers();
        triggerRepaint();
    }
    public void triggerRepaint() {
        // logger.info("NeuronNode repaint triggered");
        // Maybe the parent node would have better access to repainting...
        HortaWorkspaceNode parentNode = (HortaWorkspaceNode)(getParentNode().getParentNode());
        if (parentNode != null)
            parentNode.triggerRepaint();
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Property prop;
            // visible
            prop = new PropertySupport.Reflection(this, boolean.class, "isVisible", "setVisible"); 
            prop.setName("visible");
            set.put(prop); 
            // size
            prop = new PropertySupport.Reflection(this, int.class, "getSize", null); 
            prop.setName("size"); 
            set.put(prop); 
            // color
            prop = new PropertySupport.Reflection(this, Color.class, "getColor", "setColor"); 
            prop.setName("color");
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf


    private static class CenterOnNeuronAction extends AbstractAction
    {
        private final NeuronModel neuron;
        private final VantageInterface vantage;
        
        public CenterOnNeuronAction(NeuronModel neuron, VantageInterface vantage)
        {
            putValue(NAME, "Center on this neuron");
            this.neuron = neuron;
            this.vantage = vantage;
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            // 1 - compute neuron bounding box center point
            Box3 boundingBox = new Box3();
            for (NeuronVertex vertex: neuron.getVertexes())
                boundingBox.include(new Vector3(vertex.getLocation()));
            Vector3 centroid = boundingBox.getCentroid();
            // 2 - find actual vertex closest to that center point
            NeuronVertex closestVertex = neuron.getVertexes().iterator().next();
            float minDistSquared = centroid.distanceSquared(new Vector3(closestVertex.getLocation()));
            for (NeuronVertex vertex: neuron.getVertexes()) {
                float d2 = centroid.distanceSquared(new Vector3(vertex.getLocation()));
                if (d2 < minDistSquared) {
                    minDistSquared = d2;
                    closestVertex = vertex;
                }
            }
            // 3 - center on that vertex
            Vector3 center = new Vector3(closestVertex.getLocation());
            vantage.setFocus(center.getX(), center.getY(), center.getZ());
            // 4 - adjust scale
            float maxScale = 5.0f;
            for (int i = 0; i < 3; ++i) {
                float boxEdge = boundingBox.max.get(i) - boundingBox.min.get(i);
                maxScale = Math.max(maxScale, boxEdge);
            }
            vantage.setSceneUnitsPerViewportHeight(maxScale);

            vantage.notifyObservers();
        }
    }

}
