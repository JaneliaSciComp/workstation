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
import java.util.Collection;
import java.util.List;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Christopher Bruns
 */
public class VertexSubsetNode extends AbstractNode
{
    private final VertexSubset vertices;

    VertexSubsetNode(VertexSubset vertices) {
        super(Children.create(new VertexSubsetChildFactory(vertices), true), Lookups.singleton(vertices));
        this.vertices = vertices;
        setDisplayName(vertices.getName());
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/VertexTip2.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public int getSize() {return vertices.getVertices().size();}
    
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
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf

    
    private static class VertexSubsetChildFactory extends ChildFactory<NeuronVertex>
    {
        private final VertexSubset vertices;
        
        public VertexSubsetChildFactory(VertexSubset vertices) {
            this.vertices = vertices;
        }

        @Override
        protected boolean createKeys(List<NeuronVertex> toPopulate)
        {
            for (NeuronVertex neuron : vertices.getVertices()) {
                toPopulate.add(neuron);
            }
            return true;
        }
        
        @Override
        protected Node createNodeForKey(NeuronVertex vertex) {
            Collection<NeuronVertex> neighbors = vertices.getNeighborMap().get(vertex);
            return new NeuronVertexNode(vertex, neighbors);
        }
    }
}
