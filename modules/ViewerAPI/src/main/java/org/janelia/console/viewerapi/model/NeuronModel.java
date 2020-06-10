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
extends Hideable, NonInteractable, UserToggleRadius
{
    String getName();
    void setName(String name);
    
    Color getColor();
    void setColor(Color color);

    // used to limit impact for neuron vbo checking
    boolean getReviewMode();
    void setReviewMode(boolean reviewMode);

    Long getNeuronId();
    
    String getOwnerKey();
    void setOwnerKey(String ownerKey);
    
    // Signals when the color of this neuron is toggled on or off
    ObservableInterface getColorChangeObservable();
    
    Collection<NeuronVertex> getVertexes();
    Collection<NeuronEdge> getEdges();

    // returns vertices that have been reviewed
    Collection<NeuronVertex> getReviewedVertices();
    boolean isReviewedVertex (NeuronVertex vertex);
    void addReviewedVertices(Collection<NeuronVertex> vertexList);
    void removeReviewedVertices(Collection<NeuronVertex> vertexList);
    void clearVertices (Collection<NeuronVertex> vertexList);
    
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
