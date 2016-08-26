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

package org.janelia.horta.actors;

import com.jogamp.common.nio.Buffers;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.Material;

/**
 *
 * @author brunsc
 */
class TetVolumeMeshActor extends MeshActor 
{
    private final List<List<Integer>> outerTetrahedra = new ArrayList<>();
    private final List<Integer> centralTetrahedron = new ArrayList<>();
    // First render pass uses parent class vboTriangleAdjacenyIndices
    // protected int vboCentralTriangleAdjacencyIndices = 0; // For second render pass, just the central tetrahedron.
    // protected int vboReversedTriangleAdjacencyIndices = 0; // For the third render pass, front tetrahedra.

    public TetVolumeMeshActor(MeshGeometry geometry, Material material, CompositeObject3d parent) {
        super(geometry, material, parent);
    }
    
    public void addOuterTetrahedron(int a, int b, int c, int apex) {
        List<Integer> tet = new ArrayList<>();
        tet.add(a);
        tet.add(b);
        tet.add(c);
        tet.add(apex);
        outerTetrahedra.add(tet);
    }
    
    public void setCentralTetrahedron(int a, int b, int c, int apex) {
        List<Integer> tet = centralTetrahedron;
        tet.clear();
        tet.add(a);
        tet.add(b);
        tet.add(c);
        tet.add(apex);     
    }
    
    @Override
    public void displayTriangleAdjacencies(GL3 gl) 
    {
        final boolean doBlend = true;
        if (doBlend) {
            gl.glEnable(GL3.GL_BLEND);
            gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
            gl.glBlendEquation(GL3.GL_MAX);
        }
        
        final boolean doCull = true;
        if (doCull) {
            // Display Front faces only.
            gl.glEnable(GL3.GL_CULL_FACE);
            gl.glCullFace(GL3.GL_BACK);
            // (secretly the actual initial mesh geometry is the back faces of the tetrahedra though...)
            // (but semantically, we are showing front faces of the rendered volume)
        }
        
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());

        if (vboTriangleAdjacencyIndices == 0)
            initTriangleAdjacencyIndices(gl);
        
        // All three passes now in one index buffer
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, triangleAdjacencyIndexCount, GL3.GL_UNSIGNED_INT, 0);

        vertexBufferObject.unbind(gl);
    }


    
    @Override
    protected void initTriangleAdjacencyIndices(GL3 gl) 
    {
        // Outer tetrahedra
        if ((vboTriangleAdjacencyIndices == 0) && (outerTetrahedra.size() > 0))
        {
            // Conceptually there are three render passes, to get the five tetrahedra comprising a block to render
            // in painter's algorithm order:
            //  1) all tetrahedra BEHIND the central tetrahedron
            //  2) the central tetrahedron
            //  3) all tetrahedra IN FRONT of the central tetrahedron
            // We rely on on the geometry shader to reject tetrahedra that do not qualify for passes 1 & 3.
            // We send two copies of the central tetrahedron in the middle, so the shader will always accept one of them.
            int tetCount = 2 * outerTetrahedra.size(); // front and back versions of each tetrahedron
            if (centralTetrahedron.size() > 0)
                tetCount += 2;
            triangleAdjacencyIndexCount = 6 * tetCount;
            IntBuffer indices = Buffers.newDirectIntBuffer(triangleAdjacencyIndexCount); // for first render pass
            
            // For first render pass, ordinary tetrahedra
            for (List<Integer> tet : outerTetrahedra) {
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Forward
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(b); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(c); // abuse elements 3&5 to encode front-ness
            }

            // For second render pass, include two copies of central tetrahedron
            if (centralTetrahedron.size() > 0)
            {
                List<Integer> tet = centralTetrahedron;
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Central Tetrahedron gets both forward and reverse forms stored,
                // so it will always get drawn exactly once, after culling.
                // 1) forward version
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(b); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                // 2) reverse version
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(b); // abuse elements 3&5 to encode front-ness          
            }
            
            // For third render pass, tetrahedra with partially flipped base triangle
            for (List<Integer> tet : outerTetrahedra) {
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                // Inverted
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(c); // abuse elements 3&5 to encode front-ness
                indices.put(c);
                indices.put(b); // abuse elements 3&5 to encode front-ness
            }

            indices.flip();
            IntBuffer vbos = IntBuffer.allocate(1);
            vbos.rewind();
            gl.glGenBuffers(1, vbos);
            vboTriangleAdjacencyIndices = vbos.get(0);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
            gl.glBufferData(
                    GL3.GL_ELEMENT_ARRAY_BUFFER,
                    indices.capacity() * Buffers.SIZEOF_INT,
                    indices,
                    GL3.GL_STATIC_DRAW);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

}
