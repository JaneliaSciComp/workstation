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

package org.janelia.horta;

import Jama.Matrix;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.geometry3d.VolumeTextureMesh;

/**
 * Specialized Mesh for mouse brain tile rendering
 * @author Christopher Bruns
 */
public class BrainTileMesh extends MeshGeometry
implements VolumeTextureMesh
{
    private final BrickInfo brickInfo;
    private Matrix4 transformWorldToTexCoord;
    
    public BrainTileMesh(BrickInfo brickInfo) {
        this.brickInfo = brickInfo;
        // Enumerate 8 corners of tile
        // NOTE: These are actual corners, not axis-aligned bounding box
        int cornerCount = 0;
        for ( ConstVector3 corner : brickInfo.getCornerLocations() ) {
            // spatial coordinates
            Vertex v = addVertex(corner);
            
            // texture coordinates
            // X texture coordinates alternate
            float tx = (float)cornerCount % 2;
            float ty = (float)(cornerCount/2) % 2;
            float tz = (float)(cornerCount/4) % 2;
            v.setAttribute("texCoord", new Vector3(tx, ty, tz));
            
            cornerCount += 1;
        }

        addFace(new int[] {0, 2, 3, 1}); // rear
        addFace(new int[] {4, 5, 7, 6}); // front
        addFace(new int[] {1, 3, 7, 5}); // right
        addFace(new int[] {0, 4, 6, 2}); // left
        addFace(new int[] {2, 6, 7, 3}); // top
        addFace(new int[] {0, 1, 5, 4}); // bottom
        notifyObservers();
    }
    
    @Override
    public Matrix4 getTransformWorldToTexCoord() {
        if (transformWorldToTexCoord == null) {
            Matrix m = brickInfo.getTexCoord_X_stageUm();
            transformWorldToTexCoord = new Matrix4(m);
            // System.out.println("transformWorldToTexCoord = "+transformWorldToTexCoord);
        }
        return transformWorldToTexCoord;
    }

    @Override
    public float getMinResolution() {
        // convert nanometers to micrometers
        return (float)brickInfo.getResolutionMicrometers();
    }
    
}
