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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelChildFactory extends ChildFactory< VertexSubset >
{
    private final NeuronModel neuron;
    private final Map<NeuronVertex, Collection<NeuronVertex>> vertexNeighbors = new HashMap<>();
    private final VertexSubset tips = new VertexSubset(
            new HashSet<NeuronVertex>(), 
            "Tips",
            vertexNeighbors, 1); // zero or one neighbor
    private final VertexSubset branchPoints = new VertexSubset(
            new HashSet<NeuronVertex>(), 
            "Branch Points",
            vertexNeighbors, 3); // more than two neighbors
    
    public NeuronModelChildFactory(NeuronModel neuron) {
        this.neuron = neuron;
    }

    private void insertDirectionalPair(NeuronVertex vertex1, NeuronVertex vertex2) {
        if (! vertexNeighbors.containsKey(vertex1))
            vertexNeighbors.put(vertex1, new HashSet<NeuronVertex>());
        vertexNeighbors.get(vertex1).add(vertex2);
    }
    
    private void insertEdge(NeuronEdge edge) {
        Iterator<NeuronVertex> it = edge.iterator();
        NeuronVertex vertex1 = it.next();
        NeuronVertex vertex2 = it.next();
        insertDirectionalPair(vertex1, vertex2);
        insertDirectionalPair(vertex2, vertex1);
    }
    
    private void refreshCaches() {
        // Precache all vertex neighbors
        // TODO: somehow manage updates efficiently, or at all...
        vertexNeighbors.clear();
        for ( NeuronEdge edge : neuron.getEdges() ) {
            insertEdge(edge);
        }
        tips.getVertices().clear();
        branchPoints.getVertices().clear();
        for ( NeuronVertex vertex : neuron.getVertexes()) {
            Collection<NeuronVertex> neighbors = vertexNeighbors.get(vertex);
            int neighborCount = 0;
            if (neighbors != null)
                neighborCount = neighbors.size();
            if (neighborCount < 2)
                tips.getVertices().add(vertex);
            else if (neighborCount > 2)
                branchPoints.getVertices().add(vertex);
        }        
    }
    
    @Override
    protected boolean createKeys(List< VertexSubset > toPopulate)
    {
        refreshCaches();
        if (tips.getVertices().size() > 0)
            toPopulate.add(tips);
        if (branchPoints.getVertices().size() > 0)
            toPopulate.add(branchPoints);
        return true;
    }

    @Override
    protected Node createNodeForKey(VertexSubset key) {
        return new VertexSubsetNode(key);
    }
}
