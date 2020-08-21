package org.janelia.workstation.gui.large_volume_viewer.neuron_api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronEdgeAdapter implements NeuronEdge
{
    private final Collection<NeuronVertex> vertices = new HashSet<>();

    NeuronEdgeAdapter(NeuronVertex vertex1, NeuronVertex vertex2)
    {
        vertices.add(vertex1);
        vertices.add(vertex2);
    }

    @Override
    public Iterator<NeuronVertex> iterator()
    {
        return vertices.iterator();
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.vertices);
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
        final NeuronEdgeAdapter other = (NeuronEdgeAdapter) obj;
        if (!Objects.equals(this.vertices, other.vertices)) {
            return false;
        }
        return true;
    }

    @Override
    public int getSize() {
        return vertices.size();
    }
}
