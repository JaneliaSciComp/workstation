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

package org.janelia.console.viewerapi.model;

import java.awt.Color;
import java.util.Collection;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 *
 * @author Christopher Bruns
 */
public interface NeuronModel 
extends Hideable, NonInteractable, UserVisible, UserToggleRadius
{
    String getName();
    void setName(String name);
    
    Color getColor();
    void setColor(Color color);
    
    Long getNeuronId();
    
    String getOwnerKey();
    void setOwnerKey(String ownerKey);
    
    // Signals when the color of this neuron is toggled on or off
    ObservableInterface getColorChangeObservable();
    
    Collection<NeuronVertex> getVertexes();
    Collection<NeuronEdge> getEdges();
    
    // Adding a vertex is so common that it gets its own signal
    NeuronVertexCreationObservable getVertexCreatedObservable(); // vertices added to neuron
    NeuronVertexUpdateObservable getVertexUpdatedObservable(); // vertex changed
    NeuronVertexDeletionObservable getVertexesRemovedObservable(); // vertices removed from neuron
    
    // Probably too much overhead to attach a listener to every vertex, so listen to vertex changes
    // at the neuron level.
    ObservableInterface getGeometryChangeObservable(); // vertices changed location or radius
    
    // Signals when the visibility of this neuron is toggled on or off
    ObservableInterface getVisibilityChangeObservable();

    // Custom methods to help hook into LVV model from Horta
    NeuronVertex appendVertex(NeuronVertex parentVertex, float[] micronXyz, float radius);
    boolean mergeNeurite(NeuronVertex source, NeuronVertex target);
    boolean updateNeuronRadius(TmNeuronMetadata neuron, float radius);
    boolean splitNeurite(NeuronVertex anchor1, NeuronVertex anchor2);
    boolean transferNeurite(NeuronVertex anchor);
    boolean moveVertex(NeuronVertex vertex, float[] micronXyz);
    boolean updateVertexRadius(NeuronVertex vertex, float micronRadius);
    
    boolean deleteVertex(NeuronVertex doomedVertex);

    NeuronVertex getVertexByGuid(Long guid);
}
