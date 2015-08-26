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
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.janelia.horta.modelapi.NeuronVertex;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronReconstructionNode extends AbstractNode
{
    private NeuronReconstruction neuron;

    public NeuronReconstructionNode(NeuronReconstruction neuron) {
        super(Children.create(new NeuronReconstructionChildFactory(neuron), true), Lookups.singleton(neuron));
        setDisplayName(neuron.getName() + " (" + neuron.getVertexes().size() + ")");
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
    
    private Vantage getVantage() {
        Node node = this;
        while (node != null) {
            node = node.getParentNode();
            if (node instanceof NeuroanatomyWorkspaceNode) {
                return ((NeuroanatomyWorkspaceNode) node).getVantage();
            }
        }
        return null;
    }
    
    @Override
    public Action[] getActions(boolean popup) {
        List<Action> result = new ArrayList<>();
        // Maybe expose "center on" action
        // 0 - is there a camera to actually center on?
        final Vantage vantage = getVantage();
        if ((vantage != null) && neuron.getVertexes().size() > 0) {
            result.add(new AbstractAction() {
                {
                    putValue(NAME, "Center on this neuron");
                }
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // 1 - compute neuron bounding box center point
                    Box3 boundingBox = new Box3();
                    for (NeuronVertex vertex: neuron.getVertexes())
                        boundingBox.include(vertex.getLocation());
                    Vector3 centroid = boundingBox.getCentroid();
                    // 2 - find actual vertex closest to that center point
                    NeuronVertex closestVertex = neuron.getVertexes().iterator().next();
                    float minDistSquared = centroid.distanceSquared(closestVertex.getLocation());
                    for (NeuronVertex vertex: neuron.getVertexes()) {
                        float d2 = centroid.distanceSquared(vertex.getLocation());
                        if (d2 < minDistSquared) {
                            minDistSquared = d2;
                            closestVertex = vertex;
                        }
                    }
                    // 3 - center on that vertex
                    Vector3 center = closestVertex.getLocation();
                    vantage.setFocusPosition(center);
                    vantage.notifyObservers();
                }
            });
        }
        return result.toArray(new Action[result.size()]);
    }
}
