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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.openide.awt.UndoRedo;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronSet 
// Don't extend a built in collection, because we need hash() and equals() to respect object identity.
// extends ArrayList<NeuronReconstruction>
implements NeuronSet
{
    private final String name;
    private final ComposableObservable membershipChangeObservable = new ComposableObservable();
    private final ComposableObservable nameChangeObservable = new ComposableObservable();
    private final ComposableObservable primaryAnchorObservable = new ComposableObservable();
    protected final Collection<NeuronModel> neurons;
    // private HortaMetaWorkspace cachedHortaWorkspace = null;
    private final UndoRedo.Manager undoRedo = new UndoRedo.Manager();
    private NeuronVertex currentParentAnchor;
    
    public BasicNeuronSet(String name, Collection<NeuronModel> contents) {
        this.name = name;
        this.neurons = contents;
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public ObservableInterface getMembershipChangeObservable()
    {
        return membershipChangeObservable;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int size()
    {
        return neurons.size();
    }

    @Override
    public boolean isEmpty()
    {
        return neurons.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return neurons.contains(o);
    }

    @Override
    public Iterator<NeuronModel> iterator()
    {
        return neurons.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return neurons.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return neurons.toArray(a);
    }

    @Override
    public boolean add(NeuronModel e)
    {
        boolean result = neurons.add(e);
        if (result)
            membershipChangeObservable.setChanged();
        return result;
    }

    @Override
    public boolean remove(Object o)
    {
        boolean result = neurons.remove(o);
        if (result)
            membershipChangeObservable.setChanged();
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return neurons.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends NeuronModel> c)
    {
        boolean result = neurons.addAll(c);
        if (result)
            membershipChangeObservable.setChanged();
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean result = neurons.removeAll(c);
        if (result)
            membershipChangeObservable.setChanged();
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean result = neurons.retainAll(c);
        if (result)
            membershipChangeObservable.setChanged();
        return result;
    }

    @Override
    public void clear()
    {
        boolean result = neurons.size() > 0;
        neurons.clear();
        if (result)
            membershipChangeObservable.setChanged();
    }

    @Override
    public ObservableInterface getNameChangeObservable()
    {
        return nameChangeObservable;
    }

    @Override
    public NeuronModel createNeuron(String initialNeuronName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UndoRedo.Manager getUndoRedo() {
        return undoRedo;
    }

    @Override
    public NeuronVertex getPrimaryAnchor() {
        return currentParentAnchor;
    }

    @Override
    public void setPrimaryAnchor(NeuronVertex anchor) {
        if (anchor == currentParentAnchor)
            return; // no change
        currentParentAnchor = anchor;
        getPrimaryAnchorObservable().setChanged();
    }

    @Override
    public ObservableInterface getPrimaryAnchorObservable() {
        return primaryAnchorObservable;
    }

    @Override
    public boolean isSpatialIndexValid() {
        return false;
    }
    
    @Override
    public NeuronModel getNeuronForAnchor(NeuronVertex anchor) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<NeuronVertex> getAnchorClosestToMicronLocation(double[] micronXYZ, int n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public NeuronVertex getAnchorClosestToMicronLocation(double[] micronXYZ) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<NeuronVertex> getAnchorsInMicronArea(double[] p1, double[] p2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override 
    public NeuronModel getNeuronByGuid(Long guid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void addEditNote(NeuronVertex anchor) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
        
    @Override
    public void addTraceEndNote(NeuronVertex anchor) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void changeNeuronVisibility(List<TmNeuronMetadata> neuron, boolean visible) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeNeuronNonInteractable(List<TmNeuronMetadata> neuron, boolean interactable) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeNeuronUserToggleRadius(List<TmNeuronMetadata> neuronList, boolean userToggleRadius) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeNeuronUserProperties(List<TmNeuronMetadata> neuronList, List<String> properties, boolean toggle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CompletableFuture<Boolean> changeNeuronOwnership(Long neuronId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObjectMesh(TmObjectMesh mesh) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void removeObjectMesh(String meshName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateObjectMeshName(String oldName, String updatedName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSelectMode(boolean select) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
