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
import java.util.Objects;
import java.util.Set;

import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.BasicNeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.BasicNeuronVertexDeletionObservable;
import org.janelia.console.viewerapi.model.BasicNeuronVertexUpdateObservable;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObservable;
import org.janelia.console.viewerapi.model.NeuronVertexUpdateObservable;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Jama.Matrix;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelAdapter implements NeuronModel
{

    private TmNeuronMetadata neuron;
    private final Long neuronId;
    private final VertexList vertexes;
    private final EdgeList edges;
    private final ObservableInterface colorChangeObservable = new ComposableObservable();
    private final ObservableInterface geometryChangeObservable = new ComposableObservable();
    private final ObservableInterface visibilityChangeObservable = new ComposableObservable();
    private final NeuronVertexCreationObservable membersAddedObservable = 
            new BasicNeuronVertexCreationObservable();
    private final NeuronVertexUpdateObservable vertexUpdatedObservable =
            new BasicNeuronVertexUpdateObservable();
    private final NeuronVertexDeletionObservable membersRemovedObservable = 
            new BasicNeuronVertexDeletionObservable();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // private NeuronStyle neuronStyle;
    // TODO: Stop using locally cached color and visibility, in favor of proper syncing with underlying Style
    // private AnnotationModel annotationModel;
    private boolean nonInteractable;
    private boolean visible;
    private boolean userToggleRadius;
    private boolean underReview = false;
    private String ownerKey;
    private Color defaultColor = Color.GRAY;
    private Color cachedColor = null;
    // private TmWorkspace workspace;
    // private TmSample sample;
    private NeuronSetAdapter neuronSet;
    private HashSet<NeuronVertex> reviewedNodes = new HashSet<>();

    public NeuronModelAdapter(TmNeuronMetadata neuron, NeuronSetAdapter workspace)
            // AnnotationModel annotationModel, TmWorkspace workspace, TmSample sample)
    {
        assert neuron != null;
        this.neuron = neuron;
        this.neuronId = neuron.getId();
        this.neuronSet = workspace;
        nonInteractable = false; // TODO:
        visible = neuronSet.annotationModel.getNeuronVisibility(neuron);
        ownerKey = neuron.getOwnerKey();
        vertexes = new VertexList(neuron.getGeoAnnotationMap(), neuronSet);
        edges = new EdgeList(vertexes);
        // this.workspace = workspace;
        // this.sample = sample;
    }
    
    public void loadNewVertices (TmNeuronMetadata neuron) {
        vertexes.loadNewVertices(neuron.getGeoAnnotationMap());
        updateEdges();
    }

    protected boolean hasCachedVertex(Long vertexId) {
        return vertexes.hasCachedVertex(vertexId);
    }
        
    public NeuronVertex getVertexForAnnotation(TmGeoAnnotation annotation) {
        if (annotation == null)
            return null;
        return vertexes.getVertexByGuid(annotation.getId());
    }
    
    @Override
    public NeuronVertex getVertexByGuid(Long guid) {
        return vertexes.getVertexByGuid(guid);
    }
    
    // Special method for adding annotation anchors from the Horta side
    @Override
    public NeuronVertex appendVertex(NeuronVertex parent, float[] micronXyz, float radius) 
    {
        if ((parent != null) && (! (parent instanceof NeuronVertexAdapter)))
            return null; // TODO: error?
        
        // Convert micron coordinates to voxel coordinates
        Matrix m2v = neuronSet.getMicronToVoxMatrix();
        // Matrix m2v = MatrixUtilities.deserializeMatrix(sample.getMicronToVoxMatrix(), "micronToVoxMatrix");
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
            // no parent? create root annotation.
            TmGeoAnnotation ann;
            if (parent == null) {
                ann = neuronSet.annotationModel.addRootAnnotation(neuron, voxelXyz);
            }
            else {
                NeuronVertexAdapter p = (NeuronVertexAdapter)parent;
                TmGeoAnnotation parentAnnotation = p.getTmGeoAnnotation();
                ann = neuronSet.annotationModel.addChildAnnotation(parentAnnotation, voxelXyz);
                ActivityLogHelper.getInstance().logExternallyAddAnchor(neuronSet.workspace.getSampleRef().getTargetId(), neuronSet.workspace.getId(), ann, micronXyz);
            }
            ann.setRadius(new Double(radius));
            if (ann != null) {
                NeuronVertex vertex = vertexes.getVertexByGuid(ann.getId()); // new NeuronVertexAdapter(ann, workspace);
                result = vertex;
            }
        } catch (Exception ex) {
        }
        return result;
    }
    
    public void mergeNeuronData (TmNeuronMetadata newNeuron) {
        vertexes.updateWrapping (newNeuron.getGeoAnnotationMap());
        edges.updateGeometry();
    }
    
    @Override
    public boolean transferNeurite(NeuronVertex anchor) {
        if (! (anchor instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter nva = (NeuronVertexAdapter)anchor;
        try {
            neuronSet.annotationModel.moveNeurite(nva.getTmGeoAnnotation(), neuron);
            return true;
        } catch (Exception ex) {
            return false;
        }        
    }

    @Override
    public boolean splitNeurite(NeuronVertex anchor1, NeuronVertex anchor2) {
        if (! (anchor1 instanceof NeuronVertexAdapter))
            return false;
        if (! (anchor2 instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter nva1 = (NeuronVertexAdapter)anchor1;
        NeuronVertexAdapter nva2 = (NeuronVertexAdapter)anchor2;
        
        Long id1 = nva1.getTmGeoAnnotation().getId();
        Long id2 = nva2.getTmGeoAnnotation().getId();
        Long parentId1 = nva1.getTmGeoAnnotation().getParentId();
        Long parentId2 = nva2.getTmGeoAnnotation().getParentId();
        // Which anchor is the parent, which the child?
        // Set newRootId to the child in the relationship.
        Long newRootId = null;
        if (parentId1.equals(id2)) {
            newRootId = id1;
        }
        else if (parentId2.equals(id1)) {
            newRootId = id2;
        }
        
        if (newRootId == null)
            return false; // anchors not connected, or anchor's id is null
        
        try {
            neuronSet.annotationModel.splitNeurite(neuronId, newRootId);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean mergeNeurite(NeuronVertex source, NeuronVertex target) {
        if (! (source instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter sourceNVA = (NeuronVertexAdapter)source;
        if (! (target instanceof NeuronVertexAdapter))
            return false;
        NeuronVertexAdapter targetNVA = (NeuronVertexAdapter)target;
        
        TmGeoAnnotation sourceAnn = sourceNVA.getTmGeoAnnotation();
        TmGeoAnnotation targetAnn = targetNVA.getTmGeoAnnotation();
        
        Long sourceID = sourceAnn.getId();
        Long targetID = targetAnn.getId();
        
        // Borrow logic from AnnotationManager::canMergeNeurite()::520
        if (sourceID.equals(targetID))
            return false; // cannot merge with itself
        if (neuronSet.annotationModel.getNeuriteRootAnnotation(sourceAnn).getId().equals(
                neuronSet.annotationModel.getNeuriteRootAnnotation(targetAnn).getId())) {
            return false;
        }
        
        try {
            neuronSet.annotationModel.mergeNeurite(sourceAnn.getNeuronId(), sourceID, targetAnn.getNeuronId(), targetID);
        } 
        catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean updateNeuronRadius(TmNeuronMetadata neuron, float radius) {
        try {
            neuronSet.annotationModel.updateNeuronRadius(neuron.getId(), radius);
            return true;
        }
        catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    
    @Override
    public boolean updateVertexRadius(NeuronVertex vertex, float micronRadius) {
        try {
            if (! (vertex instanceof NeuronVertexAdapter) )
                return false;
            NeuronVertexAdapter nva = (NeuronVertexAdapter)vertex;
            TmGeoAnnotation annotation = nva.getTmGeoAnnotation();
            neuronSet.annotationModel.updateAnnotationRadius(annotation.getNeuronId(), annotation.getId(), micronRadius);
            return true;
        }
        catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    @Override
    public boolean moveVertex(NeuronVertex vertex, float[] micronXyz) {
        try {
            if (! (vertex instanceof NeuronVertexAdapter) )
                return false;
            NeuronVertexAdapter nva = (NeuronVertexAdapter)vertex;
            
            // Be Careful! I'm calling setLocation() here, so I don't have
            // recompute the coordinate transform.
            // But moveAnnotation() might someday notice that the 
            // destination coordinates are already there
            nva.setLocation(
                    micronXyz[0],
                    micronXyz[1],
                    micronXyz[2]);
            TmGeoAnnotation annotation = nva.getTmGeoAnnotation();
            Vec3 destination = new Vec3(
                    annotation.getX(),
                    annotation.getY(),
                    annotation.getZ());
            neuronSet.annotationModel.moveAnnotation(annotation.getNeuronId(), annotation.getId(), destination);
            return true;
        }
        catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    @Override
    public boolean deleteVertex(NeuronVertex doomedVertex) {
        try {
            if (! (doomedVertex instanceof NeuronVertexAdapter) )
                return false;
            NeuronVertexAdapter nva = (NeuronVertexAdapter)doomedVertex;
            TmGeoAnnotation annotation = nva.getTmGeoAnnotation();

            // Borrow some defensive logic from AnnotationManager.java::405
            if (annotation == null)
                return false; // no such anchor
            if (annotation.isRoot() && annotation.getChildIds().size() > 0)
                return false; // non-empty root cannot be deleted
            if (annotation.getChildIds().size() > 1)
                return false; // non-terminal cannot be deleted
            
            neuronSet.annotationModel.deleteLink(annotation);
            ActivityLogHelper.getInstance().logExternallyDeleteLink(neuronSet.workspace.getSampleRef().getTargetId(), neuronSet.workspace.getId(), annotation);
            return true;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    
    NeuronVertex addVertex(TmGeoAnnotation annotation)
    {
        Long vertexId = annotation.getId();
        
        // Les reported this assert triggering recently
        // assert(vertexes.containsKey(vertexId));
        if (! vertexes.containsKey(vertexId)) {
            logger.error("Could not find anchor with guid "+vertexId+" in NeuronModelAdapter");
            return null;
        }
        
        Long parentId = annotation.getParentId();
        NeuronVertex newVertex = vertexes.getVertexByTmGeoAnnotation(annotation);
        if (newVertex == null) {
            logger.error("Could not find anchor with guid "+vertexId);
            return null;
        }
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
    
    public void updateWrapping(TmNeuronMetadata neuron) {
        if (this.neuron == neuron)
            return;
        assert neuron != null;
        this.neuron = neuron;
        this.vertexes.updateWrapping(neuron.getGeoAnnotationMap());
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
        NeuronStyle style = neuronSet.annotationModel.getNeuronStyle(neuron);
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
        cachedColor = color;

        // Set color in actual wrapped Style
        boolean vis = true;
        Color deepColor = null;
        NeuronStyle style = neuronSet.annotationModel.getNeuronStyle(neuron);
        if (style != null) {
            vis = style.isVisible() ;
            deepColor = style.getColor();
        }
        else {
            vis = isVisible();
        }

        // Avoid multiple style setting calls
        if (! color.equals(deepColor)) {
            try {
                neuronSet.annotationModel.setNeuronStyle(neuron, new NeuronStyle(color, vis, isNonInteractable()));
            } catch (Exception ex) {
                logger.error("Error setting neuron style", ex);
            }
        }

        getColorChangeObservable().setChanged();
    }

    @Override
    public boolean getReviewMode() {
        return underReview;
    }

    @Override
    public void setReviewMode(boolean reviewMode) {
        underReview = reviewMode;
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

    public Collection<NeuronVertex> getCachedVertexes()
    {
        return vertexes.getCachedVertices();
    }

    public void clearCachedVertices()
    {
        vertexes.clearCachedVertices();
    }
    
    @Override
    public Collection<NeuronEdge> getEdges()
    {
        return edges;
    }

    @Override
    public Collection<NeuronVertex> getReviewedVertices() {
        return reviewedNodes;
    }

    @Override
    public boolean isReviewedVertex(NeuronVertex vertex) {
        if (reviewedNodes.contains(vertex))
            return true;
        return false;
    }

    @Override
    public void addReviewedVertices(Collection<NeuronVertex> vertexList) {
        reviewedNodes.addAll(vertexList);
    }

    @Override
    public void clearVertices(Collection<NeuronVertex> vertexList) {
        reviewedNodes.removeAll(vertexList);
    }

    @Override
    public NeuronVertexCreationObservable getVertexCreatedObservable()
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
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {       
        if (visible == this.visible) {
            // we're up to date
            return;
        }
        
        try {
            // change local first so it prevents reexecution on the callback 
            this.visible = visible;
            neuronSet.changeNeuronVisibility(neuron, visible);
        } catch (Exception ex) {
            logger.error("Error setting neuron style", ex);
        }

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

    public TmNeuronMetadata getTmNeuronMetadata()
    {
        return neuron;
    }

    @Override
    public NeuronVertexUpdateObservable getVertexUpdatedObservable() {
        return vertexUpdatedObservable;
    }

    @Override
    public boolean isNonInteractable() {
        return nonInteractable;
    }

    @Override
    public void setNonInteractable(boolean nonInteractable) {
        this.nonInteractable = nonInteractable;
    }
    
    @Override
    public boolean isUserToggleRadius() {
        return userToggleRadius;
    }

    @Override
    public void setUserToggleRadius(boolean userToggleRadius) {
        this.userToggleRadius = userToggleRadius;
    }
    
    /**
     * @return the ownerKey
     */
    @Override
    public String getOwnerKey() {
        return ownerKey;
    }

    /**
     * @param ownerKey the ownerKey to set
     */
    @Override
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    @Override
    public Long getNeuronId() {
        return neuronId;
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
        private final NeuronSetAdapter neuronSet;
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private VertexList(Map<Long, TmGeoAnnotation> vertices, NeuronSetAdapter neuronSet)
        {
            this.vertices = vertices;
            this.neuronSet = neuronSet;
        }
        
        public void loadNewVertices(Map<Long, TmGeoAnnotation> vertices) {
            this.vertices = vertices;
            clearCachedVertices();
        }
        
        public boolean hasCachedVertex(Long vertexId) {
            return cachedVertices.containsKey(vertexId);
        }
        
        private void updateWrapping(Map<Long, TmGeoAnnotation> geoAnnotationMap)
        {
            if (this.vertices != geoAnnotationMap) {
                this.vertices = geoAnnotationMap;
                cachedVertices.clear(); // just in case
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
            if (! (o instanceof NeuronVertexAdapter))
                return false;
            NeuronVertexAdapter vertex = (NeuronVertexAdapter)o;
            Long guid = vertex.getTmGeoAnnotation().getId();
            NeuronVertex vertexAgain = getVertexByGuid(guid);
            if (vertexAgain == null)
                return false;
            return true;
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
                if (a == null) {
                    logger.error("anchor not found in geoAnnotationMap");
                    return null;
                }
                cachedVertices.put(vertexId, new NeuronVertexAdapter(a, neuronSet));
            }
            return cachedVertices.get(vertexId);
        }
        
        private NeuronVertex getVertexByTmGeoAnnotation(TmGeoAnnotation a)
        {
            if (a == null) {
                logger.error("attempt to retrieve vertex for null TmGeoAnnotation");
                return null;
            }
            Long vertexId = a.getId();
            
            // sanity check
            TmGeoAnnotation fromGAMap = getAnnotationByGuid(vertexId);
            if (fromGAMap == null) {
                logger.error("anchor not found in neuron geoAnnotationMap");
            }
            
            if (! cachedVertices.containsKey(vertexId)) {
                cachedVertices.put(vertexId, new NeuronVertexAdapter(a, neuronSet));
            }
            return cachedVertices.get(vertexId);
        }

        public List<NeuronVertex> getCachedVertices() {
            return new ArrayList<>(cachedVertices.values());
        }
        
        public void clearCachedVertices() {
            cachedVertices.clear();
        }
    }

}
