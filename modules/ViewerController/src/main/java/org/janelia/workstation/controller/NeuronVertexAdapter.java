package org.janelia.workstation.controller;

import java.util.Objects;

import Jama.Matrix;
import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.util.MatrixUtilities;
import org.janelia.workstation.controller.model.TmModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronVertexAdapter implements NeuronVertex
{

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private final TmGeoAnnotation vertex;
    private final Long vertexId;
    private Jama.Matrix voxToMicronMatrix;
    private Jama.Matrix micronToVoxMatrix;

    public NeuronVertexAdapter(TmGeoAnnotation vertex) {
        this.vertex = vertex;
        this.vertexId = vertex.getId();
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
        Jama.Matrix micLoc = getVoxToMicronMatrix().times(voxLoc);
        return new float[] {
                (float) micLoc.get(0, 0),
                (float) micLoc.get(1, 0),
                (float) micLoc.get(2, 0)};
    }

    Jama.Matrix getVoxToMicronMatrix() {
        if (voxToMicronMatrix != null)
            return voxToMicronMatrix;
        updateVoxToMicronMatrices(TmModelManager.getInstance().getCurrentSample());
        return voxToMicronMatrix;
    }

    Jama.Matrix getMicronToVoxMatrix() {
        if (micronToVoxMatrix != null)
            return micronToVoxMatrix;
        updateVoxToMicronMatrices(TmModelManager.getInstance().getCurrentSample());
        return micronToVoxMatrix;
    }

    private void updateVoxToMicronMatrices(TmSample sample) {
        String serializedVoxToMicronMatrix = sample.getVoxToMicronMatrix();
        if (serializedVoxToMicronMatrix == null) {
            LOG.error("Found null voxToMicronMatrix");
            return;
        }
        voxToMicronMatrix = MatrixUtilities.deserializeMatrix(serializedVoxToMicronMatrix, "voxToMicronMatrix");

        String serializedMicronToVoxMatrix = sample.getMicronToVoxMatrix();
        if (serializedMicronToVoxMatrix == null) {
            LOG.error("Found null micronToVoxMatrix");
            return;
        }
        micronToVoxMatrix = MatrixUtilities.deserializeMatrix(serializedMicronToVoxMatrix, "micronToVoxMatrix");
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
        Jama.Matrix voxLoc = getMicronToVoxMatrix().times(micLoc);
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
