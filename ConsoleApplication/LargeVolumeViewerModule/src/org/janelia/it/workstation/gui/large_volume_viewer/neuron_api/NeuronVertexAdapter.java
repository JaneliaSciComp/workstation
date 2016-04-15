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
import java.util.Objects;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronVertexAdapter implements NeuronVertex
{
    private final TmGeoAnnotation vertex;
    private final Long vertexId;
    private final Jama.Matrix voxToMicronMatrix;
    private final Jama.Matrix micronToVoxMatrix;

    public NeuronVertexAdapter(TmGeoAnnotation vertex, TmWorkspace workspace) {
        this.vertex = vertex;
        this.vertexId = vertex.getId();
        this.voxToMicronMatrix = workspace.getVoxToMicronMatrix();
        this.micronToVoxMatrix = workspace.getMicronToVoxMatrix();
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
        Jama.Matrix micLoc = voxToMicronMatrix.times(voxLoc);
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
        Jama.Matrix voxLoc = micronToVoxMatrix.times(micLoc);
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
        return vertex.getRadius().floatValue();
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

    TmGeoAnnotation getTmGeoAnnotation()
    {
        return vertex;
    }

}
