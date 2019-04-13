package org.janelia.workstation.gui.large_volume_viewer.neuron_api;

import java.util.Objects;

import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronVertexAdapter implements NeuronVertex
{
    private final TmGeoAnnotation vertex;
    private final Long vertexId;
    private final NeuronSetAdapter workspace;

    public NeuronVertexAdapter(TmGeoAnnotation vertex, NeuronSetAdapter workspace) {
        this.vertex = vertex;
        this.vertexId = vertex.getId();
        this.workspace = workspace;
    }
    
    @Override
    public float[] getLocation()
    {
        // Convert from image voxel coordinates to Cartesian micrometers
        // TmGeoAnnotation is in voxel coordinates
        Jama.Matrix voxLoc = new Jama.Matrix(new double[][] {
            {vertex.getX(), }, 
            {vertex.getY(), }, 
            {vertex.getZ(), },
            {1.0, },
        });
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix micLoc = workspace.getVoxToMicronMatrix().times(voxLoc);
        return new float[] {
                (float) micLoc.get(0, 0),
                (float) micLoc.get(1, 0),
                (float) micLoc.get(2, 0)};
    }

    @Override
    public void setLocation(float x, float y, float z)
    {
        // Convert from Cartesian micrometers to image voxel coordinates
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix micLoc = new Jama.Matrix(new double[][] {
            {x, }, 
            {y, }, 
            {z, },
            {1.0, },
        });
        // TmGeoAnnotation XYZ is in voxel coordinates
        Jama.Matrix voxLoc = workspace.getMicronToVoxMatrix().times(micLoc);
        vertex.setX(new Double(voxLoc.get(0,0)));
        vertex.setY(new Double(voxLoc.get(1,0)));
        vertex.setZ(new Double(voxLoc.get(2,0)));
        // TODO - signalling?
    }

    @Override
    public boolean hasRadius()
    {
        if (vertex == null) 
            return false;
        if (vertex.getRadius() == null) 
            return false;
        return true;
    }

    @Override
    public float getRadius()
    {
        return hasRadius() ? vertex.getRadius().floatValue() : DefaultNeuron.radius;
    }

    @Override
    public void setRadius(float radius)
    {
        vertex.setRadius(new Double(radius));
        // TODO - signalling?
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.vertexId);
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
        final NeuronVertexAdapter other = (NeuronVertexAdapter) obj;
        if (!Objects.equals(this.vertexId, other.vertexId)) {
            return false;
        }
        return true;
    }

    public TmGeoAnnotation getTmGeoAnnotation()
    {
        return vertex;
    }

    @Override
    public String toString() {
        return "NeuronVertex[neuronId=" + vertex.getNeuronId() + ", id=" + vertex.getId() + "]";
    }

}
