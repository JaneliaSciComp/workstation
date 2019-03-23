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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.openide.awt.UndoRedo;

/**
 *
 * @author Christopher Bruns
 */
public interface NeuronSet extends Collection<NeuronModel>
{
    boolean isReadOnly();
    
    // getMembershipChangeObservable() signals when whole neurons are added or removed from the collection
    ObservableInterface getMembershipChangeObservable();
    ObservableInterface getNameChangeObservable();
    String getName();
    NeuronModel createNeuron(String initialNeuronName);

    boolean isSpatialIndexValid();
    List<NeuronVertex> getAnchorsInMicronArea(double[] p1, double[] p2);
    List<NeuronVertex> getAnchorClosestToMicronLocation(double[] micronXYZ, int n);
    NeuronVertex getAnchorClosestToMicronLocation(double[] micronXYZ);

    NeuronModel getNeuronForAnchor(NeuronVertex anchor);

    UndoRedo.Manager getUndoRedo(); // Manage edit operations per neuron collection
    // Sometimes there is one anchor selected for edit operations
    NeuronVertex getPrimaryAnchor(); // can be null
    void setPrimaryAnchor(NeuronVertex anchor); // set to null to clear
    ObservableInterface getPrimaryAnchorObservable();
    
    NeuronModel getNeuronByGuid(Long guid);
    void addEditNote(NeuronVertex anchor);
    void addTraceEndNote(NeuronVertex anchor);
    void changeNeuronVisibility(List<TmNeuronMetadata> neuron, boolean visibility);
    void changeNeuronNonInteractable (List<TmNeuronMetadata> neuron, boolean interactable);
    void changeNeuronUserToggleRadius (List<TmNeuronMetadata> neuronList, boolean userToggleRadius);
    void changeNeuronUserProperties (List<TmNeuronMetadata> neuronList, List<String> properties, boolean toggle);
    CompletableFuture<Boolean> changeNeuronOwnership (Long neuronId);
    void addObjectMesh(TmObjectMesh mesh);
    void removeObjectMesh(String meshName);    
    void updateObjectMeshName(String oldName, String updatedName);
    void setSelectMode(boolean select);
}
