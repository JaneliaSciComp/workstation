package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronEdge;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronChildFactory extends ChildFactory< VertexSubset >
{
    private final TmNeuronMetadata neuron;
    private final Map<TmGeoAnnotation, Collection<TmGeoAnnotation>> vertexNeighbors = new HashMap<>();
    private final VertexSubset tips = new VertexSubset(
            new HashSet<TmGeoAnnotation>(),
            "Tips",
            vertexNeighbors, 1); // zero or one neighbor
    private final VertexSubset branchPoints = new VertexSubset(
            new HashSet<TmGeoAnnotation>(),
            "Branch Points",
            vertexNeighbors, 3); // more than two neighbors
    
    public NeuronChildFactory(TmNeuronMetadata neuron) {
        this.neuron = neuron;
       /* neuron.getVisibilityChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refresh(false);
            }
        });*/
    }

    private void insertDirectionalPair(TmGeoAnnotation vertex1, TmGeoAnnotation vertex2) {
        if (! vertexNeighbors.containsKey(vertex1))
            vertexNeighbors.put(vertex1, new HashSet<TmGeoAnnotation>());
        vertexNeighbors.get(vertex1).add(vertex2);
    }
    
    private void insertEdge(TmNeuronEdge edge) {
        TmGeoAnnotation vertex1 = edge.getParentVertex();
        TmGeoAnnotation vertex2 = edge.getChildVertex();
        insertDirectionalPair(vertex1, vertex2);
        insertDirectionalPair(vertex2, vertex1);
    }
    
    private void refreshCaches() {
        // Precache all vertex neighbors
        // TODO: somehow manage updates efficiently, or at all...
        vertexNeighbors.clear();
        for ( TmNeuronEdge edge : neuron.getEdges() ) {
            insertEdge(edge);
        }
        tips.getVertices().clear();
        branchPoints.getVertices().clear();
        for ( TmGeoAnnotation vertex : neuron.getGeoAnnotationMap().values()) {
            Collection<TmGeoAnnotation> neighbors = vertexNeighbors.get(vertex);
            int neighborCount = 0;
            if (neighbors != null)
                neighborCount = neighbors.size();
            if (neighborCount < 2)
                tips.getVertices().add(vertex);
            else if (neighborCount > 2)
                branchPoints.getVertices().add(vertex);
        }        
    }
    
    @Override
    protected boolean createKeys(List< VertexSubset > toPopulate)
    {
        refreshCaches();
        if (tips.getVertices().size() > 0)
            toPopulate.add(tips);
        if (branchPoints.getVertices().size() > 0)
            toPopulate.add(branchPoints);
        return true;
    }

    @Override
    protected Node createNodeForKey(VertexSubset key) {
        return new VertexSubsetNode(key);
    }
}
