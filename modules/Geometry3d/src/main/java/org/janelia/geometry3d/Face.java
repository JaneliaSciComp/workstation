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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Mesh face, part of a MeshGeometry, consisting of a series of
 * vertex indices, representing a closed loop.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Face {
    private Color color = null;
    private Vector3 normal = null;
    private final List<Integer> vertices;

    public Face(List<Integer> vertices) {
        this.vertices = vertices;
    }
    
    public Face(Integer[] vertices) {
        this.vertices = Arrays.asList(vertices);
    }

    public Face(int[] vertices) {
        Integer[] newArray = new Integer[vertices.length];
        for (int i = 0; i < vertices.length; ++i)
            newArray[i] = vertices[i];
        this.vertices = Arrays.asList(newArray);
    }
    
    public Color getColor() {
        return color;
    }

    public Vector3 getNormal() {
        return normal;
    }
    
    public List<Integer> getVertices() {
        return vertices;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setNormal(Vector3 normal) {
        this.normal = normal;
    }

}
