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

package org.janelia.it.workstation.gui.large_volume_viewer.neuron_api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.janelia.console.viewerapi.model.BasicNeuronSet;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 Expose NeuronSet interface, using in-memory data resident in LVV
 */
public class NeuronSetAdapter
extends BasicNeuronSet
implements NeuronSet
{
    private TmWorkspace workspace = null;
    private AnnotationModel annotationModel;
    private final GlobalAnnotationListener annotationListener;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public NeuronSetAdapter(TmWorkspace workspace)
    {
        super("LVV Neurons", new NeuronList(workspace));
        this.workspace = workspace;
        annotationListener = new AnnotationListener(this);
    }
    
    public void observe(AnnotationModel annotationModel)
    {
        if (annotationModel == null)
            return; // can't watch nothing?
        if (this.annotationModel == annotationModel)
            return; // already watching this model
        // Stop listening to whatever we were listening to earlier
        if (this.annotationModel != null)
            this.annotationModel.removeGlobalAnnotationListener(annotationListener);
        this.annotationModel = annotationModel;
        annotationModel.addGlobalAnnotationListener(annotationListener);
    }

    // TODO: setName()
    @Override
    public String getName()
    {
        if (workspace != null)
            return workspace.getName();
        else
            return super.getName();
    }
    
    private void setWorkspace(TmWorkspace workspace) {
        this.workspace = workspace;
    }


    private class AnnotationListener implements GlobalAnnotationListener {
        private final NeuronSetAdapter neuronSetAdapter;

        public AnnotationListener(NeuronSetAdapter neuronSetAdapter) {
            this.neuronSetAdapter = neuronSetAdapter;
        }
        
        @Override
        public void workspaceLoaded(TmWorkspace workspace)
        {
            logger.info("Workspace loaded");
            neuronSetAdapter.setWorkspace(workspace);
            NeuronList nl = (NeuronList) neurons;
            Map<Long, NeuronStyle> neuronStyleMap = annotationModel.getNeuronStyleMap();
            nl.wrap(workspace, neuronStyleMap);
            // Propagate LVV "workspaceLoaded" signal to Horta NeuronSet::membershipChanged signal
            getMembershipChangeObservable().setChanged();
            getNameChangeObservable().setChanged();
            getNameChangeObservable().notifyObservers();
            getMembershipChangeObservable().notifyObservers();
        }

        @Override
        public void neuronSelected(TmNeuron neuron)
        {}

        @Override
        public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style)
        {}
        
    }
    
    private static class NeuronList implements Collection<NeuronModel>
    {
        private TmWorkspace workspace;
        private final Map<Long, NeuronModel> cachedNeurons = new HashMap<>();
        private Map<Long, NeuronStyle> neuronStyleMap;
        // private final Jama.Matrix voxToMicronMatrix;
        
        public NeuronList(TmWorkspace workspace) {
            this.workspace = workspace;
            // this.voxToMicronMatrix = workspace.getVoxToMicronMatrix();
        }
        
        @Override
        public int size()
        {
            if (workspace == null) 
                return 0;
            return workspace.getNeuronList().size();
        }

        @Override
        public boolean isEmpty()
        {
            if (workspace == null) 
                return true;
            return workspace.getNeuronList().isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            if (workspace == null)
                return false;
            if (workspace.getNeuronList().contains(o))
                return true;
            if (! ( o instanceof NeuronModelAdapter ))
                return false;
            NeuronModelAdapter neuron = (NeuronModelAdapter) o;
            TmNeuron tmNeuron = neuron.getTmNeuron();
            return workspace.getNeuronList().contains(tmNeuron);
        }

        @Override
        public Iterator<NeuronModel> iterator()
        {
            if (workspace == null) {
                // return empty iterator
                return new ArrayList<NeuronModel>().iterator();
            }
            final Iterator<TmNeuron> it = workspace.getNeuronList().iterator();
            return new Iterator<NeuronModel>() {

                @Override
                public boolean hasNext()
                {
                    return it.hasNext();
                }

                @Override
                public NeuronModel next()
                {
                    TmNeuron neuron = it.next();
                    Long guid = neuron.getId();
                    if (! cachedNeurons.containsKey(guid)) {
                        NeuronStyle neuronStyle = neuronStyleMap.get(guid);
                        cachedNeurons.put(guid, new NeuronModelAdapter(neuron, neuronStyle, workspace));
                    }
                    return cachedNeurons.get(guid);
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Object[] toArray()
        {
            NeuronModel[] result = new NeuronModel[size()];
            int i = 0;
            for (NeuronModel neuron : this) {
                result[i] = neuron;
                i++;
            }
            return result;
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean add(NeuronModel e)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean addAll(Collection<? extends NeuronModel> c)
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void wrap(TmWorkspace workspace, Map<Long, NeuronStyle> neuronStyleMap)
        {
            this.workspace = workspace;
            this.neuronStyleMap = neuronStyleMap;
        }
        
    }
}
