package org.janelia.horta.nodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelChildFactory extends ChildFactory< VertexSubset >
{
    private final NeuronModel neuron;
    private final Map<NeuronVertex, Collection<NeuronVertex>> vertexNeighbors = new HashMap<>();
    private final VertexSubset tips = new VertexSubset(
            new HashSet<NeuronVertex>(), 
            "Tips",
            vertexNeighbors, 1); // zero or one neighbor
    private final VertexSubset branchPoints = new VertexSubset(
            new HashSet<NeuronVertex>(), 
            "Branch Points",
            vertexNeighbors, 3); // more than two neighbors
    
    public NeuronModelChildFactory(NeuronModel neuron) {
        this.neuron = neuron;
        neuron.getVisibilityChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refresh(false);
            }
        });
    }

    private void insertDirectionalPair(NeuronVertex vertex1, NeuronVertex vertex2) {
        if (! vertexNeighbors.containsKey(vertex1))
            vertexNeighbors.put(vertex1, new HashSet<NeuronVertex>());
        vertexNeighbors.get(vertex1).add(vertex2);
    }
    
    private void insertEdge(NeuronEdge edge) {
        Iterator<NeuronVertex> it = edge.iterator();
        NeuronVertex vertex1 = it.next();
        NeuronVertex vertex2 = it.next();
        insertDirectionalPair(vertex1, vertex2);
        insertDirectionalPair(vertex2, vertex1);
    }
    
    private void refreshCaches() {
        // Precache all vertex neighbors
        // TODO: somehow manage updates efficiently, or at all...
        vertexNeighbors.clear();
        for ( NeuronEdge edge : neuron.getEdges() ) {
            insertEdge(edge);
        }
        tips.getVertices().clear();
        branchPoints.getVertices().clear();
        for ( NeuronVertex vertex : neuron.getVertexes()) {
            Collection<NeuronVertex> neighbors = vertexNeighbors.get(vertex);
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
