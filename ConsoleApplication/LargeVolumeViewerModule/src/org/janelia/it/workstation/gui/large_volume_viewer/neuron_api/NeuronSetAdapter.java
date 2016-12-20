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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.console.viewerapi.model.BasicNeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.NeuronVertexUpdateObservable;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
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
implements NeuronSet// , LookupListener
{
    private TmWorkspace workspace; // LVV workspace, as opposed to Horta workspace
    private TmSample sample;
    private AnnotationModel annotationModel;
    private final GlobalAnnotationListener globalAnnotationListener;
    private final TmGeoAnnotationModListener annotationModListener;
    private HortaMetaWorkspace cachedHortaWorkspace = null;
    private final Lookup.Result<HortaMetaWorkspace> hortaWorkspaceResult = Utilities.actionsGlobalContext().lookupResult(HortaMetaWorkspace.class);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public NeuronSetAdapter()
    {
        super("LVV Neurons", new NeuronList());
        globalAnnotationListener = new MyGlobalAnnotationListener();
        annotationModListener = new MyTmGeoAnnotationModListener();
        hortaWorkspaceResult.addLookupListener(new NSALookupListener());
    }
    
    @Override
    public NeuronModel createNeuron(String neuronName) {
        TmNeuronMetadata neuron;
        try {
            neuron = annotationModel.createNeuron(neuronName);
        } catch (Exception ex) {
            logger.warn("Error creating neuron",ex);
            return null;
        }
        return new NeuronModelAdapter(neuron, annotationModel, workspace, sample);
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
        if (workspace == null)
            return false;
        if (! workspace.getName().equals(getName()))
            getNameChangeObservable().setChanged();
        this.workspace = workspace;
        this.sample = annotationModel.getCurrentSample();
        NeuronList nl = (NeuronList) neurons;
        if (nl.wrap(workspace, annotationModel))
            getMembershipChangeObservable().setChanged();
        return true;
    }

    private void repaintHorta() {
        if (cachedHortaWorkspace == null)
            return;
        // Below is the way to trigger a repaint, without changing the viewpoint
        cachedHortaWorkspace.setChanged();
        cachedHortaWorkspace.notifyObservers();                
    }

    private class MyTmGeoAnnotationModListener implements TmGeoAnnotationModListener
    {
        
        private NeuronModelAdapter neuronModelForTmGeoAnnotation(TmGeoAnnotation annotation) 
        {
            // TODO: Use a more efficient index here...            
            // Find neuron
            Long neuronId = annotation.getNeuronId();
            NeuronModelAdapter neuron = null;
            for (NeuronModel neuron0 : NeuronSetAdapter.this) {
                neuron = (NeuronModelAdapter)neuron0;
                if (neuron.getTmNeuronMetadata().getId().equals(neuronId))
                    break;
            }
            return neuron;
        }
        
        @Override
        public void annotationAdded(TmGeoAnnotation annotation)
        {
            // logger.info("annotationAdded");
            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            // updateEdges(); // Brute force approach reanalyzes all edges            
            // Surgical approach only adds the one new edge
            // (vertex is added implicitly)
                        
            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(annotation);
            if (neuron == null) {
                logger.error("could not find NeuronModel for newly added TmGeoAnnotation");
                return;
            }
            
            NeuronVertex newVertex = neuron.addVertex(annotation);
            if (newVertex == null) {
                logger.error("NeuronModelAdapter.addVertex() returned null");
                return;
            }
            neuron.getGeometryChangeObservable().setChanged(); // set here because its hard to detect otherwise
            // Trigger a Horta repaint  for instant GUI feedback
            // NOTE - assumes this callback is only invoked from one-at-a-time manual addition
            final boolean doRecenterHorta = false;
            if (cachedHortaWorkspace != null) 
            {
                if (doRecenterHorta) 
                {
                    // 1) recenter on annotation location in Horta, just like in LVV
                    float recenter[] = newVertex.getLocation();
                    cachedHortaWorkspace.getVantage().setFocus(recenter[0], recenter[1], recenter[2]);
                    cachedHortaWorkspace.getVantage().setChanged();
                    cachedHortaWorkspace.getVantage().notifyObservers();
                }

                // 2) repaint Horta now, to update view without further user interaction
                // Below is the way to trigger a repaint, without changing the viewpoint
                repaintHorta();
                // Emit annotation added signal, to update Horta spatial index
                NeuronVertexCreationObservable addedSignal = neuron.getVertexCreatedObservable();
                addedSignal.setChanged();
                addedSignal.notifyObservers(new VertexWithNeuron(newVertex, neuron));
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
                // Create an optimized container of deleted vertices
                Map<NeuronModel, Collection<NeuronVertex>> deletedVerticesByNeuron =
                        new HashMap<>();
                for (TmGeoAnnotation deletedAnnotation : annotations) 
                {
                    NeuronList nl = (NeuronList) neurons;
                    Long neuronId = deletedAnnotation.getNeuronId();
                    if (! nl.hasCachedNeuronId(neuronId))
                        continue; // Never had that neuron instantiated, so ignore
                    NeuronModelAdapter neuron = nl.getCachedNeuron(neuronId);
                    Long vertexId = deletedAnnotation.getId();
                    if (! neuron.hasCachedVertex(vertexId))
                        continue; // Optimization to ignore vertices that were never instantiated
                    NeuronVertex vertex = neuron.getVertexForAnnotation(deletedAnnotation);
                    // Create container for this neuron vertices, if necessary
                    if (! deletedVerticesByNeuron.containsKey(neuron))
                        deletedVerticesByNeuron.put(neuron, new ArrayList<NeuronVertex>());
                    deletedVerticesByNeuron.get(neuron).add(vertex);
                }
                // Send out one signal per neuron
                for (NeuronModel neuron : deletedVerticesByNeuron.keySet()) {
                    neuron.getVertexesRemovedObservable().setChanged();
                    Collection<NeuronVertex> deletedVertices = deletedVerticesByNeuron.get(neuron);
                    neuron.getVertexesRemovedObservable().notifyObservers(
                            new VertexCollectionWithNeuron(deletedVertices, neuron));
                }
                
                // Repaint Horta now, to update view without further user interaction
                // (but do not recenter, as LVV does not recenter in this situation either)
                repaintHorta();
            }
        }

        @Override
        public void annotationReparented(TmGeoAnnotation annotation)
        {
            sanityCheckWorkspace(); // beware of shifting sands beneath us...
            Long neuronId = annotation.getNeuronId();
            updateEdges(); // updateEdges() is required for post-merge update in Horta. TODO: is performance optimization needed here?
            // TODO: is this linear search really the best way to get the neuron that goes with this annotation?
            for (NeuronModel neuron0 : NeuronSetAdapter.this) {
                NeuronModelAdapter neuron = (NeuronModelAdapter)neuron0;
                if (neuron.getTmNeuronMetadata().getId().equals(neuronId)) {
                    NeuronVertex parentVertex = neuron.getVertexForAnnotation(annotation);
                    if (parentVertex == null) 
                        return;
                    // TODO: - react somehow to the reparenting
                }
            }
            logger.info("annotationReparented");
        }

        @Override
        public void annotationMoved(TmGeoAnnotation movedAnnotation) {
            sanityCheckWorkspace();
            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(movedAnnotation);
            if (neuron == null) {
                logger.warn("Could not find neuron for moved anchor");
                return;
            }
            NeuronVertex movedVertex = neuron.getVertexForAnnotation(movedAnnotation);
            if (movedVertex == null) {
                logger.info("Skipping moved anchor not yet instantiated in Horta");
                return;
            }
            NeuronVertexUpdateObservable signal = neuron.getVertexUpdatedObservable();
            signal.setChanged();
            signal.notifyObservers(new VertexWithNeuron(movedVertex, neuron));
            logger.info("annotationMoved");
            repaintHorta();
        }

        @Override
        public void annotationNotMoved(TmGeoAnnotation annotation)
        {
            logger.info("annotationNotMoved");
            // updateEdges();
        }

        @Override
        public void annotationRadiusUpdated(TmGeoAnnotation annotation) {
            sanityCheckWorkspace();
            NeuronModelAdapter neuron = neuronModelForTmGeoAnnotation(annotation);
            if (neuron == null) {
                logger.warn("Could not find neuron for reradiused anchor");
                return;
            }
            NeuronVertex movedVertex = neuron.getVertexForAnnotation(annotation);
            if (movedVertex == null) {
                logger.info("Skipping reradiused anchor not yet instantiated in Horta");
                return;
            }
            NeuronVertexUpdateObservable signal = neuron.getVertexUpdatedObservable();
            signal.setChanged();
            signal.notifyObservers(new VertexWithNeuron(movedVertex, neuron));
            logger.info("annotationRadiusUpdated");
            repaintHorta();
        }
    }


    private class MyGlobalAnnotationListener implements GlobalAnnotationListener {
        
        @Override
        public void workspaceLoaded(TmWorkspace workspace)
        {
            logger.debug("Workspace loaded");
            setWorkspace(workspace);
            // Propagate LVV "workspaceLoaded" signal to Horta NeuronSet::membershipChanged signal
            getMembershipChangeObservable().setChanged();
            getNameChangeObservable().setChanged();
            getNameChangeObservable().notifyObservers();
            getMembershipChangeObservable().notifyObservers();
        }

        @Override
        public void neuronSelected(TmNeuronMetadata neuron)
        {}

        @Override
        public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style)
        {
            if (updateOneNeuronStyle(neuron, style)) {
                repaintHorta();
            }
        }
            
        private boolean updateOneNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style)
        {
            if (neuron == null)
                return false;
            if (style == null)
                return false;
            NeuronList nl = (NeuronList) neurons;
            if (! nl.hasCachedNeuronId(neuron.getId()))
                return false; // Don't instantiate the neuron now, if it is not previously instantiated.

            // Update Horta color when LVV color changes
            boolean result = false;
            NeuronModel neuronModel = nl.neuronModelForTmNeuron(neuron);

            Color newColor = style.getColor();
            if (! newColor.equals(neuronModel.getColor())) {
                neuronModel.setColor(newColor);
                neuronModel.getColorChangeObservable().notifyObservers();
                result = true;
            }
            
            boolean vis = style.isVisible();
            if (vis != neuronModel.isVisible()) {
                neuronModel.setVisible(vis);
                neuronModel.getVisibilityChangeObservable().notifyObservers();
                result = true;
            }
            
            return result;
        }

        @Override
        public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStylemap)
        {
            if (neuronStylemap == null)
                return;
            
            // bulk color/visibility change
            boolean bChanged = false;
            for (Map.Entry<TmNeuronMetadata, NeuronStyle> entry : neuronStylemap.entrySet()) {
                if (updateOneNeuronStyle(entry.getKey(), entry.getValue()))
                    bChanged = true;
            }
            
            if (bChanged)
                repaintHorta();
        }

        @Override
        public void neuronTagsChanged(List<TmNeuronMetadata> neuronList)
        {}
        
    }
    
    private static class NeuronList implements Collection<NeuronModel>
    {
        private TmWorkspace workspace;
        private TmSample sample;
        private final Map<Long, NeuronModelAdapter> cachedNeurons = new HashMap<>();
        private AnnotationModel annotationModel;
        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        
        private NeuronModel neuronModelForTmNeuron(TmNeuronMetadata tmNeuron) 
        {
            if (tmNeuron == null)
                return null;
            Long guid = tmNeuron.getId();
            if (! cachedNeurons.containsKey(guid)) {
                // NeuronStyle neuronStyle = neuronStyleMap.get(guid);
                cachedNeurons.put(guid, new NeuronModelAdapter(tmNeuron, annotationModel, workspace, sample));
            }
            return cachedNeurons.get(guid);
        }
        
        private boolean hasCachedNeuronId(Long neuronId) {
            return cachedNeurons.containsKey(neuronId);
        }
        
        public NeuronModelAdapter getCachedNeuron(Long neuronId) {
            return cachedNeurons.get(neuronId);
        }
        
        @Override
        public int size()
        {
            if (workspace == null) 
                return 0;
            return annotationModel.getNeuronList().size();
        }

        @Override
        public boolean isEmpty()
        {
            if (workspace == null) 
                return true;
            return annotationModel.getNeuronList().isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            if (workspace == null)
                return false;
            if (annotationModel.getNeuronList().contains(o))
                return true;
            if (! ( o instanceof NeuronModelAdapter ))
                return false;
            NeuronModelAdapter neuron = (NeuronModelAdapter) o;
            TmNeuronMetadata tmNeuronMetadata = neuron.getTmNeuronMetadata();
            return annotationModel.getNeuronList().contains(tmNeuronMetadata);
        }

        @Override
        public Iterator<NeuronModel> iterator()
        {
            if (workspace == null) {
                // return empty iterator
                return new ArrayList<NeuronModel>().iterator();
            }
            final Iterator<TmNeuronMetadata> it = annotationModel.getNeuronList().iterator();
            return new Iterator<NeuronModel>() {

                @Override
                public boolean hasNext()
                {
                    return it.hasNext();
                }

                @Override
                public NeuronModel next()
                {

                    TmNeuronMetadata neuron = it.next();
                    return neuronModelForTmNeuron(neuron);
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
            if (! ( o instanceof NeuronModelAdapter ))
                return false;
            
            NeuronModelAdapter neuron = (NeuronModelAdapter)o;
            TmNeuronMetadata tmn = neuron.getTmNeuronMetadata();
            TmNeuronMetadata previousNeuron = annotationModel.getCurrentNeuron();
            Long neuronId = tmn.getId();
            boolean removingCurrentNeuron = (previousNeuron.getId() == neuronId);

            if (! removingCurrentNeuron) 
                annotationModel.selectNeuron(tmn);
            try {
                annotationModel.deleteCurrentNeuron();
            } catch (Exception ex) {
                logger.warn("Error deleting neuron",ex);
                return false;
            }
            finally {
                if (! removingCurrentNeuron) // restore previous selected neuron
                    annotationModel.selectNeuron(previousNeuron);
            }

            cachedNeurons.remove(neuronId);

            return true;
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

        private boolean wrap(TmWorkspace workspace, AnnotationModel annotationModel)
        {
            if ( (annotationModel == this.annotationModel) && (workspace == this.workspace) )
                return false; // short circuit -- nothing changed
            
            this.annotationModel = annotationModel;
            this.workspace = workspace;
            this.sample = annotationModel.getCurrentSample();
            
            // If we get this far, either the annotationModel or the workspace changed,
            // so we should refresh our persistent cached NeuronModelAdapter objects
            
            Set<Long> oldNeurons = new HashSet<>(cachedNeurons.keySet());
            Set<Long> newNeurons = new HashSet<>();
            boolean neuronMembershipChanged = false;
            boolean neuronsWereRefreshed = false;
            for (TmNeuronMetadata tmNeuronMetadata : annotationModel.getNeuronList())
            {
                Long newId = tmNeuronMetadata.getId();
                newNeurons.add(newId);
                // Keep our NeuronModel instances persistent, even when the underlying
                // TmNeuronMetadata instance (annoyingly) changes
                if (oldNeurons.contains(newId)) { // we saw this neuron before!
                    NeuronModelAdapter neuron = cachedNeurons.get(newId);
                    neuron.updateWrapping(tmNeuronMetadata, annotationModel, workspace);
                    neuronsWereRefreshed = true;
                }
                else {
                    neuronMembershipChanged = true;
                }
            }

            // identify obsolete neurons AFTER identifying refreshable neurons
            oldNeurons.removeAll(newNeurons); // just the obsolete neurons
            for (Long obsoleteId : oldNeurons) {
                cachedNeurons.remove(obsoleteId);
                neuronMembershipChanged = true;
            }
            
            return neuronMembershipChanged; //  || neuronsWereRefreshed;
        }

    }
    
    class NSALookupListener implements LookupListener
    {
        @Override
        public void resultChanged(LookupEvent lookupEvent)
        {
            Collection<? extends HortaMetaWorkspace> allWorkspaces = hortaWorkspaceResult.allInstances();
            if (allWorkspaces.isEmpty())
                return;
            HortaMetaWorkspace metaWorkspace = allWorkspaces.iterator().next();
            if (metaWorkspace != cachedHortaWorkspace) {
                cachedHortaWorkspace = metaWorkspace;
            }
        }
    }
}
