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

package org.janelia.horta;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.model.HortaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexAdditionObserver;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.geometry3d.Vector3;

/**
 * NeuronVertexIndex is intended to permit rapid access to a NeuronVertex, given XYZ.
 * First use is intended to be mouse selection of neuron vertices.
 * @author Christopher Bruns
 */
public class NeuronVertexSpatialIndex implements Collection<NeuronVertex>
{
    private KDTree<NeuronVertex> index = new KDTree<>(3);
    private final HortaWorkspace workspace;
    private final Set<NeuronSet> currentNeuronSets = new HashSet<>();
    private final Set<NeuronModel> currentNeuronModels = new HashSet<>();
    private final Map<NeuronVertex, NeuronModel> vertexNeurons = new HashMap<>();
    private final NeuronVertexAdditionObserver neuronModelObserver = new NeuronModelObserver();
    
    private final Observer workspaceUpdater;

    public NeuronVertexSpatialIndex(HortaWorkspace workspace) {
        this.workspace = workspace;
        this.workspaceUpdater = new WorkspaceUpdater(workspace);
        rebuildIndex(this.workspace);
        this.workspace.addObserver(workspaceUpdater);
    }
    
    public NeuronModel neuronForVertex(NeuronVertex vertex)
    {
        return vertexNeurons.get(vertex);
    }


    public NeuronVertex getNearest(double location[]) throws KeySizeException 
    {
        try {
            return index.nearest(location);
        }
        // KDTree sometimes raises an exception here...
        catch (java.lang.ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }
    
    public NeuronVertex getNearest(Vector3 xyz)
    {
        try {
            return getNearest(new double[] {xyz.getX(), xyz.getY(), xyz.getZ()});
        } catch (KeySizeException ex) {
            // Exceptions.printStackTrace(ex);
            return null;
        }
    }
    
    private double[] keyForVertex(NeuronVertex v)
    {
        float xyz[] = v.getLocation();
        return new double[] {xyz[0], xyz[1], xyz[2]};
    }
    
    private void rebuildIndex(HortaWorkspace workspace) {
        clear();
        for (NeuronSet set : workspace.getNeuronSets()) {
            addNeuronSet(set);
        }
    }
    
    private void addNeuronModel(NeuronModel neuron) {
        currentNeuronModels.add(neuron);
        for (NeuronVertex vertex : neuron.getVertexes()) {
            addNeuronVertex(neuron, vertex);
        }
        // Observe neuron changes
        neuron.getVertexAddedObservable().addObserver(neuronModelObserver);
    }
    
    public void addNeuronVertex(NeuronModel neuron, NeuronVertex vertex) {
        if (addPrivately(vertex)) {
            vertexNeurons.put(vertex, neuron);
        }
    }
    
    private void addNeuronSet(NeuronSet set) {
        // TODO: add vertexes in clever median order, to balance the tree
        currentNeuronSets.add(set);
        for (NeuronModel neuron : set) {
            addNeuronModel(neuron);
        }
        set.getMembershipChangeObservable().addObserver(new NeuronSetUpdater(set));
    }
    
    /// Collection interface below
    
    @Override
    public int size()
    {
        return index.size();
    }

    @Override
    public boolean isEmpty()
    {
        return index.size() == 0;
    }

    @Override
    public boolean contains(Object o)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<NeuronVertex> iterator()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object[] toArray()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean add(NeuronVertex e)
    {
        throw new UnsupportedOperationException();
    }
    
    private boolean addPrivately(NeuronVertex e)
    {
        try {
            double[] key = keyForVertex(e);
            index.insert(key, e);
        } catch (KeySizeException | KeyDuplicateException ex) {
            // Exceptions.printStackTrace(ex);
            return false;
        }
        return true;
    }

    @Override
    public boolean remove(Object o)
    {
        try {
            if (! (o instanceof NeuronVertex))
                return false;
            NeuronVertex v = (NeuronVertex) o;
            double[] k = keyForVertex(v);
            index.delete(k);
        } catch (KeySizeException ex) {
            // Exceptions.printStackTrace(ex);
            return false;
        } catch (KeyMissingException ex) {
            // Exceptions.printStackTrace(ex);
            return false;
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addAll(Collection<? extends NeuronVertex> c)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear()
    {
        index = new KDTree<>(3);
        currentNeuronSets.clear();
        currentNeuronModels.clear();
        vertexNeurons.clear();
    }

    private class NeuronModelObserver implements NeuronVertexAdditionObserver 
    {
        @Override
        public void update(GenericObservable<VertexWithNeuron> object, VertexWithNeuron data) {
            addNeuronVertex(data.neuron, data.vertex);
            // System.out.println("vertex added");
        }
    }

    private class WorkspaceUpdater implements Observer {
        private final HortaWorkspace workspace;
        
        public WorkspaceUpdater(HortaWorkspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public void update(Observable o, Object arg)
        {
            for (NeuronSet set : workspace.getNeuronSets()) {
                if (currentNeuronSets.contains(set))
                    continue; // We already know this set
                addNeuronSet(set);
            }
        }
    
    };
    
    private class NeuronSetUpdater implements Observer 
    {
        private final NeuronSet neuronSet;
        
        public NeuronSetUpdater(NeuronSet neuronSet) {
            this.neuronSet = neuronSet;
        }

        @Override
        public void update(Observable o, Object arg)
        {
            for (NeuronModel model : neuronSet) {
                if (currentNeuronModels.contains(model))
                    continue;
                addNeuronModel(model);
            }
        }
        
    }

}
