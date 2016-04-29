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

import java.util.ArrayList;
import org.janelia.console.viewerapi.listener.NeuronCreationListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import org.janelia.console.viewerapi.BasicGenericObservable;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.GenericObserver;
import org.janelia.console.viewerapi.listener.NeuronVertexCreationListener;
import org.janelia.console.viewerapi.listener.NeuronVertexDeletionListener;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexAdditionObserver;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObserver;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeuronManager helps synchronized neuron models between Horta and Large Volume Viewer
 * @author Christopher Bruns
 */
public class NeuronManager implements LookupListener
{
    // Use Lookup to access neuron models from LVV
    // Based on tutorial at https://platform.netbeans.org/tutorials/74/nbm-selection-1.html
    private Lookup.Result<NeuronSet> neuronsLookupResult = null;

    private final HortaMetaWorkspace workspace;
    private final Set<NeuronSet> currentNeuronSets = new HashSet<>();
    private final Set<NeuronModel> currentNeuronModels = new HashSet<>();
    private final Map<NeuronVertex, NeuronModel> vertexNeurons = new HashMap<>();
        
    private final NeuronVertexAdditionObserver vertexAdditionObserver = 
            new NeuronVertexAdditionObserver() {
                @Override
                public void update(GenericObservable<VertexWithNeuron> object, VertexWithNeuron data) {
                    if (vertexNeurons.containsKey(data.vertex)) 
                        return; // We already saw this vertex
                    vertexNeurons.put(data.vertex, data.neuron);
                    neuronVertexCreationObservable.setChangedAndNotifyObservers(data);
                }
            };
    private final NeuronVertexDeletionObserver vertexDeletionObserver = 
            new NeuronVertexDeletionObserver() {
                @Override
                public void update(GenericObservable<VertexCollectionWithNeuron> object, VertexCollectionWithNeuron data) {
                    for (NeuronVertex vertex : data.vertexes)
                        vertexNeurons.remove(vertex);
                    neuronVertexDeletionObservable.deleteAndNotify(data);
                }
            };
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final NeuronCreationObservable neuronCreationObservable = new NeuronCreationObservable();
    private final NeuronVertexCreationObservable neuronVertexCreationObservable =
            new NeuronVertexCreationObservable();
    private final NeuronVertexDeletionObservable neuronVertexDeletionObservable =
            new NeuronVertexDeletionObservable();
    
    public NeuronManager(HortaMetaWorkspace workspace) {
        this.workspace = workspace;
    }
    
    public void addNeuronCreationListener(NeuronCreationListener listener) {
        neuronCreationObservable.addNeuronCreationListener(listener);
    }
    
    public void addNeuronVertexCreationListener(NeuronVertexCreationListener listener) {
        neuronVertexCreationObservable.addNeuronVertexCreationListener(listener);
    }
    
    public void addNeuronVertexDeletionListener(NeuronVertexDeletionListener listener) {
        neuronVertexDeletionObservable.addNeuronVertexDeletionListener(listener);
    }
    
    public NeuronModel neuronForVertex(NeuronVertex vertex)
    {
        return vertexNeurons.get(vertex);
    }

    // When Horta TopComponent opens
    public void onOpened() {
        neuronsLookupResult = Utilities.actionsGlobalContext().lookupResult(NeuronSet.class);
        neuronsLookupResult.addLookupListener(this);
        checkNeuronLookup();
    }
    
    // When Horta TopComponent closes
    public void onClosed() {
        neuronsLookupResult.removeLookupListener(this);
    }
    
    // When the contents of the NeuronSet Lookup changes
    @Override
    public void resultChanged(LookupEvent le)
    {
        checkNeuronLookup();
    }
    
    private void addNeuronSet(NeuronSet set) {
        currentNeuronSets.add(set); // TODO: is this accounting needed?
        for (NeuronModel neuron : set) {
            addNeuronModel(neuron);
        }
        // Prepare to emit finer-grained signals in the future
        set.getMembershipChangeObservable().addObserver(new NeuronSetUpdater(set));
        // Notify observers now about the newly found neurons
        if (set.size() > 0) {
            neuronCreationObservable.addNewNeurons(set);
            neuronCreationObservable.notifyObservers();
        }
    }
    
    private void addNeuronModel(NeuronModel neuron) {
        currentNeuronModels.add(neuron);
        for (NeuronVertex vertex : neuron.getVertexes()) {
            vertexNeurons.put(vertex, neuron);
        }
        // Observe neuron changes
        neuron.getVertexAddedObservable().addObserver(vertexAdditionObserver);
        neuron.getVertexesRemovedObservable().addObserver(vertexDeletionObserver);
    }
    
    // Respond to changes in NeuronSet Lookup
    private void checkNeuronLookup() {
        Collection<? extends NeuronSet> allNeuronLists = neuronsLookupResult.allInstances();
        if (! allNeuronLists.isEmpty()) {
            boolean bWorkspaceChanged = false;
            // logger.info("Neuron Lookup found!");
            for (NeuronSet neuronList : allNeuronLists) {
                boolean bListAdded = workspace.getNeuronSets().add(neuronList);
                if (bListAdded) {
                    // logger.info("Added new neuron list!");
                    workspace.setChanged();
                    bWorkspaceChanged = true;

                    // Process the new neuron list - this code was in NeuronVertexSpatialIndex
                    addNeuronSet(neuronList);
                }
            }
            if (bWorkspaceChanged) {
                workspace.notifyObservers();
            }
        }
        else {
            // logger.info("Hey! There are no lists of neurons around.");
            // TODO - repond to lack of neuron collections.
        }
    }

    HortaMetaWorkspace getWorkspace() {
        return workspace;
    }

    
    private class NeuronSetUpdater implements Observer 
    {
        private final NeuronSet neuronSet;
        
        public NeuronSetUpdater(NeuronSet neuronSet) {
            this.neuronSet = neuronSet;
        }

        @Override
        public void update(Observable o, Object arg)
        {
            Collection<NeuronModel> newModels = new ArrayList<>();
            for (NeuronModel model : neuronSet) {
                if (currentNeuronModels.contains(model))
                    continue;
                newModels.add(model);
                addNeuronModel(model);
            }
            if (newModels.size() > 0) {
                neuronCreationObservable.addNewNeurons(newModels);
                neuronCreationObservable.notifyObservers();
            }
        }
        
    }
    
    
    private static class NeuronVertexCreationObservable {
        private final GenericObservable<VertexWithNeuron> observable = new BasicGenericObservable<>();

        public void addNeuronVertexCreationListener(NeuronVertexCreationListener listener) {
            observable.addObserver(new NeuronVertexCreationListenerAdapter(listener));
        }
        
        public void setChangedAndNotifyObservers(VertexWithNeuron vertexWithNeuron) {
            observable.setChanged();
            observable.notifyObservers(vertexWithNeuron);
        }

        private static class NeuronVertexCreationListenerAdapter 
        implements GenericObserver<VertexWithNeuron>
        {
            private final NeuronVertexCreationListener listener;

            private NeuronVertexCreationListenerAdapter(NeuronVertexCreationListener listener) {
                this.listener = listener;
            }

            @Override
            public void update(GenericObservable<VertexWithNeuron> object, VertexWithNeuron data) {
                listener.neuronVertexCreated(data);
            }
            
        }
    }
    
    // Watches NeuronModel signal, and rebroadcasts to NeuronVertexDeletionListeners
    private static class NeuronVertexDeletionObservable {
        private final GenericObservable<VertexCollectionWithNeuron> observable = new BasicGenericObservable<>();

        public void addNeuronVertexDeletionListener(NeuronVertexDeletionListener listener) {
            observable.addObserver(new NeuronVertexDeletionListenerAdapter(listener));
        }
        
        public void deleteAndNotify(VertexCollectionWithNeuron vertexesWithNeuron) {
            observable.setChanged();
            observable.notifyObservers(vertexesWithNeuron);
        }

        private static class NeuronVertexDeletionListenerAdapter 
        implements GenericObserver<VertexCollectionWithNeuron>
        {
            private final NeuronVertexDeletionListener listener;

            private NeuronVertexDeletionListenerAdapter(NeuronVertexDeletionListener listener) {
                this.listener = listener;
            }

            @Override
            public void update(GenericObservable<VertexCollectionWithNeuron> object, VertexCollectionWithNeuron data) {
                listener.neuronVertexesDeleted(data);
            }
            
        }
    }
    
    
    private class NeuronCreationObservable
    {
        private final GenericObservable<Collection<NeuronModel>> observable = new BasicGenericObservable<>();
        private final Collection<NeuronModel> recentlyAddedNeurons = new HashSet<>();
        
        public void addNeuronCreationListener(NeuronCreationListener listener) {
            observable.addObserver(new NeuronCreationListenerAdapter(listener));
        }

        public void addNewNeurons(Collection<NeuronModel> newNeurons) {
            if (newNeurons.isEmpty()) return;
            if (recentlyAddedNeurons.addAll(newNeurons)) {
                observable.setChanged();
            }
        }
        
        public void notifyObservers() {
            if (recentlyAddedNeurons.isEmpty()) return; // nothing to see here
            observable.notifyObservers(new ArrayList<>(recentlyAddedNeurons));
            recentlyAddedNeurons.clear();
        }
        
        private class NeuronCreationListenerAdapter
        implements GenericObserver<Collection<NeuronModel>>
        {
            private final NeuronCreationListener listener;

            public NeuronCreationListenerAdapter(NeuronCreationListener listener) {
                this.listener = listener;
            }

            @Override
            public void update(GenericObservable<Collection<NeuronModel>> object, Collection<NeuronModel> addedNeurons) {
                listener.neuronsCreated(addedNeurons);
            }
        }

    }

    
}
