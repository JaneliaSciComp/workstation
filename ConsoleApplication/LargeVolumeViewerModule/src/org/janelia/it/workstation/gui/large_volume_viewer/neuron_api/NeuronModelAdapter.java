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

import Jama.Matrix;
import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.BasicNeuronVertexAdditionObservable;
import org.janelia.console.viewerapi.model.BasicNeuronVertexDeletionObservable;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexAdditionObservable;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObservable;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelAdapter implements NeuronModel
{
    private TmNeuron neuron;
    private final Long neuronId;
    private final VertexList vertexes;
    private final EdgeList edges;
    private final ObservableInterface colorChangeObservable = new ComposableObservable();
    private final ObservableInterface geometryChangeObservable = new ComposableObservable();
    private final ObservableInterface visibilityChangeObservable = new ComposableObservable();
    private final NeuronVertexAdditionObservable membersAddedObservable = 
            new BasicNeuronVertexAdditionObservable();
    private final NeuronVertexDeletionObservable membersRemovedObservable = 
            new BasicNeuronVertexDeletionObservable();
    private Color color = new Color(86, 142, 216); // default color is "neuron blue"
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // private NeuronStyle neuronStyle;
    // TODO: Stop using locally cached color and visibility, in favor of proper syncing with underlying Style
    private AnnotationModel annotationModel;
    private boolean bIsVisible; // TODO: sync visibility with LVV eventually. For now, we want fast toggle from Horta.
    private Color defaultColor = Color.GRAY;
    private Color cachedColor = null;
    private TmWorkspace workspace;

    public NeuronModelAdapter(TmNeuron neuron, AnnotationModel annotationModel, TmWorkspace workspace) 
    {
        this.neuron = neuron;
        this.neuronId = neuron.getId();
        this.annotationModel = annotationModel;
        bIsVisible = true; // TODO: 
        vertexes = new VertexList(neuron.getGeoAnnotationMap(), workspace);
        edges = new EdgeList(vertexes);
        this.workspace = workspace;
    }

    public boolean hasCachedVertex(Long vertexId) {
        return vertexes.hasCachedVertex(vertexId);
    }
        
    public NeuronVertex getVertexForAnnotation(TmGeoAnnotation annotation) {
        return vertexes.getVertexByGuid(annotation.getId());
    }
    
    // Special method for adding annotations from the Horta side
    @Override
    public NeuronVertex appendVertex(NeuronVertex parent, float[] micronXyz, float radius) 
    {
        if (! (parent instanceof NeuronVertexAdapter))
            return null; // TODO: error?
        NeuronVertexAdapter p = (NeuronVertexAdapter)parent;
        TmGeoAnnotation parentAnnotation = p.getTmGeoAnnotation();
        
        // Convert micron coordinates to voxel coordinates
        Matrix m2v = workspace.getMicronToVoxMatrix();
                // Convert from image voxel coordinates to Cartesian micrometers
        // TmGeoAnnotation is in voxel coordinates
        Jama.Matrix micLoc = new Jama.Matrix(new double[][] {
            {micronXyz[0], }, 
            {micronXyz[1], }, 
            {micronXyz[2], },
            {1.0, },
        });
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix voxLoc = m2v.times(micLoc);
        Vec3 voxelXyz = new Vec3(
            (float) voxLoc.get(0, 0), 
            (float) voxLoc.get(1, 0), 
            (float) voxLoc.get(2, 0) );
        NeuronVertex result = null;
        try {
            // TODO: radius
            TmGeoAnnotation ann = annotationModel.addChildAnnotation(parentAnnotation, voxelXyz);
            if (ann != null) {
                NeuronVertex vertex = vertexes.getVertexByGuid(ann.getId()); // new NeuronVertexAdapter(ann, workspace);
                result = vertex;
            }
        } catch (Exception ex) {
        }
        return result;
    }
    
    @Override
    public boolean mergeNeurite(NeuronVertex source, NeuronVertex target) {
        if (! (source instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter sourceNVA = (NeuronVertexAdapter)source;
        if (! (target instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter targetNVA = (NeuronVertexAdapter)target;
        Long sourceID = sourceNVA.getTmGeoAnnotation().getId();
        Long targetID = targetNVA.getTmGeoAnnotation().getId();
        try {
            annotationModel.mergeNeurite(sourceID, targetID);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean deleteVertex(NeuronVertex doomedVertex) {
        try {
            if (! (doomedVertex instanceof NeuronVertexAdapter) )
                return false;
            NeuronVertexAdapter nva = (NeuronVertexAdapter)doomedVertex;
            TmGeoAnnotation annotation = nva.getTmGeoAnnotation();
            annotationModel.deleteLink(annotation);
            return true;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    
    NeuronVertex addVertex(TmGeoAnnotation annotation)
    {
        Long vertexId = annotation.getId();
        assert(vertexes.containsKey(vertexId));
        Long parentId = annotation.getParentId();
        NeuronVertex newVertex = vertexes.getVertexByGuid(vertexId);
        // Add edge
        if (vertexId.equals(parentId)) 
            return newVertex; // Self parent, so no edge. TODO: maybe this never happens
        // comment from TmGeoAnnotation.java: "parentID is the neuron (if root annotation) or another TmGeoAnn"
        if (annotation.getNeuronId().equals(parentId))
            return newVertex; // It appears parentId==neuronId for (parentless) root/seed points
        assert(vertexes.containsKey(parentId));
        edges.add(new NeuronEdgeAdapter(newVertex, vertexes.getVertexByGuid(parentId)));
        getGeometryChangeObservable().setChanged(); // mark dirty, but don't sweep (notifyObservers) yet
        return newVertex;
    }
    
    public void updateWrapping(TmNeuron neuron, AnnotationModel annotationModel, TmWorkspace workspace) {
        if (this.neuron != neuron) {
            this.neuron = neuron;
            assert this.neuronId.equals(neuron.getId()); // Must use .equals() friggin java...
        }
        if (this.annotationModel != annotationModel) {
            this.annotationModel = annotationModel; // annotationModel gets reinstantiated on workspace reload
            // TODO: is more cleanup needed here? The vertex cache will get cleared below in vertexes.updateWrapping, assuming getGeoAnnotationMap has changed.
        }
        this.vertexes.updateWrapping(neuron.getGeoAnnotationMap(), workspace);
        this.workspace = workspace;
    }

    public boolean updateEdges() {
        boolean result = edges.updateGeometry();
        if (result) {
            geometryChangeObservable.setChanged();
        }
        return result;
    }
    
    @Override
    public String getName()
    {
        return neuron.getName();
    }

    @Override
    public void setName(String name)
    {
        neuron.setName(name);
        // TODO : name change observable?
    }

    @Override
    public Color getColor()
    {
        // Use just-in-time fetching of color, since NeuronStyle might not be
        // set yet at WorkspaceLoaded time.
        if (cachedColor != null)
            return cachedColor;
        NeuronStyle style = annotationModel.getNeuronStyle(neuron);
        if (style != null) {
            cachedColor = style.getColor();
            return cachedColor;
        }
        return defaultColor;
    }

    @Override
    public void setColor(Color color)
    {
        if (color == null)
            return;
        if (color.equals(cachedColor))
            return;
        // TODO: set color in actual wrapped Style
        cachedColor = color;
    }

    @Override
    public ObservableInterface getColorChangeObservable()
    {
        return colorChangeObservable;
    }

    @Override
    public Collection<NeuronVertex> getVertexes()
    {
        return vertexes;
    }

    @Override
    public Collection<NeuronEdge> getEdges()
    {
        return edges;
    }

    @Override
    public NeuronVertexAdditionObservable getVertexAddedObservable()
    {
        return membersAddedObservable;
    }

    @Override
    public NeuronVertexDeletionObservable getVertexesRemovedObservable()
    {
        return membersRemovedObservable;
    }

    @Override
    public ObservableInterface getGeometryChangeObservable()
    {
        return geometryChangeObservable;
    }

    @Override
    public ObservableInterface getVisibilityChangeObservable()
    {
        return visibilityChangeObservable;
    }

    @Override
    public boolean isVisible()
    {
        return bIsVisible;
        // return neuronStyle.isVisible();
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (bIsVisible == visible)
            return; // no change
        bIsVisible = visible;
        getVisibilityChangeObservable().setChanged();
    }

    // Hash based on database GUID, so distinct object with the same ID hash together
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(neuronId);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NeuronModelAdapter other = (NeuronModelAdapter) obj;
        if (!Objects.equals(this.neuronId, other.neuronId)) {
            return false;
        }
        return true;
    }

    TmNeuron getTmNeuron()
    {
        return neuron;
    }

    // TODO: - implement Edges correctly
    private static class EdgeList 
    implements Collection<NeuronEdge>
    // extends ArrayList<NeuronEdge> 
    {
        private final VertexList vertices;
        // Cache edge set for efficient updates
        private final Set<NeuronEdge> cachedEdges = new HashSet<>();
        
        public EdgeList(VertexList vertices) {
            this.vertices = vertices;
            updateGeometry();
        }
        
        public final boolean updateGeometry() {
            // Count the number of nodes with parents
            // TODO: This is a very brittle approach; the edge set could easily get stale.
            Set<NeuronEdge> freshEdges = new HashSet<>(); // All edges in the current model
            Set<NeuronEdge> newEdges = new HashSet<>(); // Edges we did not know about before
            Set<NeuronEdge> obsoleteEdges = new HashSet<>(); // Old edges no longer present
            for (NeuronVertex v : vertices) {
                NeuronVertexAdapter v1 = (NeuronVertexAdapter)v;
                TmGeoAnnotation child = v1.getTmGeoAnnotation();
                if (child == null)
                    continue;
                Long parentId = child.getParentId();
                TmGeoAnnotation parent = vertices.getAnnotationByGuid(parentId);
                if (parent == null)
                    continue;
                NeuronEdge edge = new NeuronEdgeAdapter(
                        vertices.getVertexByGuid(child.getId()), 
                        vertices.getVertexByGuid(parentId));
                freshEdges.add(edge);
                if (! cachedEdges.contains(edge))
                    newEdges.add(edge);
            }
            for (NeuronEdge edge : cachedEdges) {
                if (! freshEdges.contains(edge))
                    obsoleteEdges.add(edge);
            }
            if (newEdges.isEmpty() && obsoleteEdges.isEmpty())
                return false; // no change
            cachedEdges.removeAll(obsoleteEdges);
            cachedEdges.addAll(newEdges);
            return true;
        }

        @Override
        public int size()
        {
            return cachedEdges.size();
        }

        @Override
        public boolean isEmpty()
        {
            return cachedEdges.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return cachedEdges.contains(o);
        }

        @Override
        public Iterator<NeuronEdge> iterator()
        {
            return cachedEdges.iterator();
        }

        @Override
        public Object[] toArray()
        {
            return cachedEdges.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return cachedEdges.toArray(a);
        }

        @Override
        public boolean add(NeuronEdge e)
        {
            return cachedEdges.add(e);
        }

        @Override
        public boolean remove(Object o)
        {
            return cachedEdges.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            return cachedEdges.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends NeuronEdge> c)
        {
            return cachedEdges.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            return cachedEdges.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            return cachedEdges.retainAll(c);
        }

        @Override
        public void clear()
        {
            cachedEdges.clear();
        }

    }


    // Adapter to make a Map<Long, TmGeoAnnotation> look like a Collection<NeuronVertex>
    private static class VertexList implements Collection<NeuronVertex> 
    {
        private Map<Long, TmGeoAnnotation> vertices;
        private final Map<Long, NeuronVertex> cachedVertices = new HashMap<>();
        private TmWorkspace workspace;

        private VertexList(Map<Long, TmGeoAnnotation> vertices, TmWorkspace workspace)
        {
            this.vertices = vertices;
            this.workspace = workspace;
        }
        
        public boolean hasCachedVertex(Long vertexId) {
            return cachedVertices.containsKey(vertexId);
        }
        
        private void updateWrapping(Map<Long, TmGeoAnnotation> geoAnnotationMap, TmWorkspace workspace)
        {
            if (this.vertices != geoAnnotationMap) {
                this.vertices = geoAnnotationMap;
                cachedVertices.clear(); // just in case
            }
            if (this.workspace != workspace) {
                this.workspace = workspace;
            }
        }
        
        public boolean containsKey(Long neuronGuid) {
            return vertices.containsKey(neuronGuid);
        }
        
        @Override
        public int size()
        {
            return vertices.size();
        }

        @Override
        public boolean isEmpty()
        {
            return vertices.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Iterator<NeuronVertex> iterator()
        {
            final Iterator<TmGeoAnnotation> it = vertices.values().iterator();
            return new Iterator<NeuronVertex>() {

                @Override
                public boolean hasNext()
                {
                    return it.hasNext();
                }

                @Override
                public NeuronVertex next()
                {
                    TmGeoAnnotation vertex = it.next();
                    Long guid = vertex.getId();
                    return getVertexByGuid(guid);
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private TmGeoAnnotation getAnnotationByGuid(Long vertexId)
        {
            return vertices.get(vertexId);
        }

        private NeuronVertex getVertexByGuid(Long vertexId)
        {
            if (! cachedVertices.containsKey(vertexId)) {
                TmGeoAnnotation a = getAnnotationByGuid(vertexId);
                if (a == null)
                    return null;
                cachedVertices.put(vertexId, new NeuronVertexAdapter(a, workspace));
            }
            return cachedVertices.get(vertexId);
        }

    }

}
