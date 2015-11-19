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
import java.util.List;
import java.util.Map;
import org.janelia.console.viewerapi.model.BasicNeuronSet;
import org.janelia.console.viewerapi.model.HortaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 Expose NeuronSet interface, using in-memory data resident in LVV
 */
public class NeuronSetAdapter
extends BasicNeuronSet
implements NeuronSet, LookupListener
{
    private TmWorkspace workspace; // LVV workspace, as opposed to Horta workspace
    private AnnotationModel annotationModel;
    private final GlobalAnnotationListener globalAnnotationListener;
    private final TmGeoAnnotationModListener annotationModListener;
    private HortaWorkspace cachedHortaWorkspace = null;
    private Lookup.Result<HortaWorkspace> hortaWorkspaceResult = Utilities.actionsGlobalContext().lookupResult(HortaWorkspace.class);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public NeuronSetAdapter()
    {
        super("LVV Neurons", new NeuronList());
        globalAnnotationListener = new MyGlobalAnnotationListener();
        annotationModListener = new MyTmGeoAnnotationModListener();
        hortaWorkspaceResult.addLookupListener(this);
    }
    
    public void observe(AnnotationModel annotationModel)
    {
        if (annotationModel == null)
            return; // can't watch nothing?
        if (this.annotationModel == annotationModel)
            return; // already watching this model
        // Stop listening to whatever we were listening to earlier
        if (this.annotationModel != null) {
            this.annotationModel.removeGlobalAnnotationListener(globalAnnotationListener);
            this.annotationModel.removeTmGeoAnnotationModListener(annotationModListener);
        }
        this.annotationModel = annotationModel;
        annotationModel.addGlobalAnnotationListener(globalAnnotationListener);
        annotationModel.addTmGeoAnnotationModListener(annotationModListener);
    }
    
    // Sometimes the TmWorkspace instance changes, even though the semantic workspace has not changed.
    // In this case, we need to scramble to distribute the new object instances
    // behind our stable NeuronSet/NeuronMode/NeuronVertex facade.
    private void sanityCheckWorkspace() {
        TmWorkspace w = this.annotationModel.getCurrentWorkspace();
        if (w == workspace) return; // unchanged
        logger.info("Workspace changed");
        setWorkspace(w);
    }
    
    // Recache edge data structures after vertices change
    private boolean updateEdges() {
        boolean edgesChanged = false;
        for (NeuronModel neuron : this) {
            if (! (neuron instanceof NeuronModelAdapter))
                continue;
            NeuronModelAdapter n = (NeuronModelAdapter)neuron;
            if (n.updateEdges())
                edgesChanged = true;
        }
        return edgesChanged;
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
    
    private boolean setWorkspace(TmWorkspace workspace) {
        if (this.workspace == workspace)
            return false;
        this.workspace = workspace;
        NeuronList nl = (NeuronList) neurons;
        nl.wrap(workspace, annotationModel);
        return true;
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent)
    {
        Collection<? extends HortaWorkspace> allWorkspaces = hortaWorkspaceResult.allInstances();
        if (allWorkspaces.isEmpty())
            return;
        HortaWorkspace workspace = allWorkspaces.iterator().next();
        if (workspace != cachedHortaWorkspace) {
            cachedHortaWorkspace = workspace;
        }
    }

    private class MyTmGeoAnnotationModListener implements TmGeoAnnotationModListener
    {
        
        @Override
        public void annotationAdded(TmGeoAnnotation annotation)
        {
            // logger.info("annotationAdded");
            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            // updateEdges(); // Brute force approach reanalyzes all edges            
            // Surgical approach only adds the one new edge
            // (vertex is added implicitly)
            Long neuronId = annotation.getNeuronId();
            for (NeuronModel neuron0 : NeuronSetAdapter.this) {
                NeuronModelAdapter neuron = (NeuronModelAdapter)neuron0;
                if (neuron.getTmNeuron().getId().equals(neuronId)) {
                    neuron.addVertex(annotation);
                    neuron.getGeometryChangeObservable().setChanged(); // set here because its hard to detect otherwise
                    // Trigger a Horta repaint  for instant GUI feedback
                    // NOTE - assumes this callback is only invoked from one-at-a-time manual addition
                    final boolean doRecenterHorta = false;
                    if (doRecenterHorta) 
                    {
                        if (cachedHortaWorkspace != null) 
                        {
                            // 1) recenter on annotation location in Horta, just like in LVV
                            // Use a temporary NeuronVertexAdapter, just to get the location in micrometer units
                            NeuronVertex nv = new NeuronVertexAdapter(annotation, workspace);
                            float recenter[] = nv.getLocation();
                            cachedHortaWorkspace.getVantage().setFocus(recenter[0], recenter[1], recenter[2]);
                            cachedHortaWorkspace.getVantage().setChanged();

                            // 2) repaint Horta now, to update view without further user interaction
                            cachedHortaWorkspace.getVantage().notifyObservers();
                            // Below is the way to trigger a repaint, without changing the viewpoint
                            // cachedHortaWorkspace.setChanged();
                            // cachedHortaWorkspace.notifyObservers();
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void annotationsDeleted(List<TmGeoAnnotation> annotations)
        {
            // logger.info("annotationDeleted");
            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            
            // TODO - surgically remove only edges related to these particular vertices
            updateEdges(); // Brute force approach reanalyzes all edges
            
            if (cachedHortaWorkspace != null) 
            {
                // Repaint Horta now, to update view without further user interaction
                // (but do not recenter, as LVV does not recenter in this situation either)
                cachedHortaWorkspace.setChanged();
                cachedHortaWorkspace.notifyObservers();
            }
        }

        @Override
        public void annotationReparented(TmGeoAnnotation annotation)
        {
            logger.info("annotationReparented");
            updateEdges();
        }

        @Override
        public void annotationNotMoved(TmGeoAnnotation annotation)
        {
            logger.info("annotationNotMoved");
            updateEdges();
        }
    }


    private class MyGlobalAnnotationListener implements GlobalAnnotationListener {
        
        @Override
        public void workspaceLoaded(TmWorkspace workspace)
        {
            logger.info("Workspace loaded");
            setWorkspace(workspace);
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
        private final Map<Long, NeuronModelAdapter> cachedNeurons = new HashMap<>();
        private AnnotationModel annotationModel;
        // private Map<Long, NeuronStyle> neuronStyleMap;
        
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
                        // NeuronStyle neuronStyle = neuronStyleMap.get(guid);
                        cachedNeurons.put(guid, new NeuronModelAdapter(neuron, annotationModel, workspace));
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

        private void wrap(TmWorkspace workspace, AnnotationModel annotationModel)
        {
            // I assume annotation model never changes...
            assert( (this.annotationModel == annotationModel) || (this.annotationModel == null));
            this.annotationModel = annotationModel;
            
            // Sadly, sometimes the workspace instance does change out from underneath us...
            if ( (this.workspace != null) && (this.workspace != workspace) ) {
                this.workspace = workspace;
                // Keep our NeuronModel instances persistent, even when the underlying
                // TmNeuron instance (annoyingly) changes
                for (TmNeuron tmNeuron : workspace.getNeuronList()) {
                    Long id = tmNeuron.getId();
                    if (cachedNeurons.containsKey(id)) {
                        NeuronModelAdapter neuron = cachedNeurons.get(id);
                        neuron.updateWrapping(tmNeuron, annotationModel, workspace);
                    }
                }
            }
            this.workspace = workspace;
        }
        
    }
}
