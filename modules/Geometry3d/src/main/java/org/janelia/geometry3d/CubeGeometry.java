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
package org.janelia.geometry3d;

import java.util.Arrays;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class CubeGeometry extends MeshGeometry {
    public CubeGeometry(Vector3 center, Vector3 size) 
    {
        // Eight vertices form the corner positions of the cube
        // precompute coordinates
        float x1 = center.getX() - size.getX()/2;
        float x2 = center.getX() + size.getX()/2;
        float y1 = center.getY() - size.getY()/2;
        float y2 = center.getY() + size.getY()/2;
        float z1 = center.getZ() - size.getZ()/2;
        float z2 = center.getZ() + size.getZ()/2;
        // Vertices
        addVertex(x1, y1, z2);
        addVertex(x2, y1, z2);
        addVertex(x2, y2, z2);
        addVertex(x1, y2, z2);
        addVertex(x1, y2, z1);
        addVertex(x2, y2, z1);
        addVertex(x2, y1, z1);
        addVertex(x1, y1, z1);
        // Faces
        addFace(new Face(Arrays.asList(new Integer[] {
            0, 1, 2, 3}))); // front
        addFace(new Face(Arrays.asList(new Integer[] {
            0, 3, 4, 7}))); // left
        addFace(new Face(Arrays.asList(new Integer[] {
            2, 5, 4, 3}))); // top
        addFace(new Face(Arrays.asList(new Integer[] {
            1, 6, 5, 2}))); // right
        addFace(new Face(Arrays.asList(new Integer[] {
            0, 7, 6, 1}))); // bottom
        addFace(new Face(Arrays.asList(new Integer[] {
            4, 5, 6, 7}))); // rear
        notifyObservers();
    }
}
