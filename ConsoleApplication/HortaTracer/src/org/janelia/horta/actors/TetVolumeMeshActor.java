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
    private List<List<Integer>> outerTetrahedra = new ArrayList<>();
    private List<Integer> centralTetrahedron = new ArrayList<>();
    protected int vboCentralTriangleAdjacencyIndices = 0;

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
    public void dispose(GL3 gl) {
        
        super.dispose(gl);
        //
        int[] vbos = {vboCentralTriangleAdjacencyIndices};
        gl.glDeleteBuffers(1, vbos, 0);
        vboCentralTriangleAdjacencyIndices = 0;
        super.dispose(gl);
    }
    
    @Override
    public void displayTriangleAdjacencies(GL3 gl) {
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());

        if (vboTriangleAdjacencyIndices == 0)
            initTriangleAdjacencyIndices(gl);
        
        // First pass: rear tetrahedra
        int passIndex = gl.glGetUniformLocation(material.getShaderProgramHandle(), "renderPass");
        gl.glUniform1i(passIndex, 3);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, triangleAdjacencyIndexCount, GL3.GL_UNSIGNED_INT, 0);
        
        // Second pass: central tetrahedron
        if (centralTetrahedron.size() > 0) {
            gl.glUniform1i(passIndex, 2);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboCentralTriangleAdjacencyIndices);
            gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, 6, GL3.GL_UNSIGNED_INT, 0);
        }
        
        // Third pass: front tetrahedra
        gl.glUniform1i(passIndex, 1);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, triangleAdjacencyIndexCount, GL3.GL_UNSIGNED_INT, 0);
        
        //
        vertexBufferObject.unbind(gl);
    }


    
    @Override
    protected void initTriangleAdjacencyIndices(GL3 gl) 
    {
        // Outer tetrahedra
        if ((vboTriangleAdjacencyIndices == 0) && (outerTetrahedra.size() > 0))
        {
            int faceCount = outerTetrahedra.size();
            triangleAdjacencyIndexCount = 6 * faceCount;
            IntBuffer indices = Buffers.newDirectIntBuffer(triangleAdjacencyIndexCount);
            for (List<Integer> tet : outerTetrahedra) {
                int a = tet.get(0); 
                int b = tet.get(1);
                int c = tet.get(2);
                int apex = tet.get(3); // apex
                indices.put(a);
                indices.put(apex);
                indices.put(b);
                indices.put(apex);
                indices.put(c);
                indices.put(apex);
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
        if ((vboCentralTriangleAdjacencyIndices == 0) && (centralTetrahedron.size() > 0))
        {
            IntBuffer indices = Buffers.newDirectIntBuffer(6);
            List<Integer> tet = centralTetrahedron;
            int a = tet.get(0); 
            int b = tet.get(1);
            int c = tet.get(2);
            int apex = tet.get(3); // apex
            indices.put(a);
            indices.put(apex);
            indices.put(b);
            indices.put(apex);
            indices.put(c);
            indices.put(apex);

            indices.flip();
            IntBuffer vbos = IntBuffer.allocate(1);
            vbos.rewind();
            gl.glGenBuffers(1, vbos);
            vboCentralTriangleAdjacencyIndices = vbos.get(0);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboCentralTriangleAdjacencyIndices);
            gl.glBufferData(
                    GL3.GL_ELEMENT_ARRAY_BUFFER,
                    indices.capacity() * Buffers.SIZEOF_INT,
                    indices,
                    GL3.GL_STATIC_DRAW);
            gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

}
