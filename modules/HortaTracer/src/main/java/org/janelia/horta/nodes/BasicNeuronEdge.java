package org.janelia.horta.nodes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronEdge implements NeuronEdge
{
    private final Set<NeuronVertex> vertices = new HashSet<>();

    public BasicNeuronEdge(NeuronVertex v1, NeuronVertex v2)
    {
        vertices.add(v1);
        vertices.add(v2);
    }

    @Override
    public Iterator<NeuronVertex> iterator()
    {
        return vertices.iterator();
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.vertices);
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
        final BasicNeuronEdge other = (BasicNeuronEdge) obj;
        if (!Objects.equals(this.vertices, other.vertices)) {
            return false;
        }
        return true;
    }

}
