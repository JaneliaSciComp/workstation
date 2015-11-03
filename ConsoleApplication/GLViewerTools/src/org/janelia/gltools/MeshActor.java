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
package org.janelia.gltools;

import org.janelia.gltools.material.Material;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Triangle;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Matrix4;
import com.jogamp.common.nio.Buffers;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ConstVector;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Edge;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.MeshFloatVbo.VertexAttribute;

/**
 * MeshActor class is analogous to Three.js Mesh class,
 * and to VTK Actor class.
 * 
 * @author brunsc
 */
public class MeshActor extends BasicGL3Actor
{
    protected final MeshGeometry geometry;
    private final Material material;
    
    private MeshFloatVbo vertexBufferObject = null;
    
    // Vbo bookkeeping
    private int triangleIndexCount = 0;
    private int particleIndexCount = 0;
    private int edgeIndexCount = 0;
    private int triangleAdjacencyIndexCount = 0;
    // Intermediate list of actual vbo vertex index for all triangles
    private final List<VertexIndex> triangleVertices = new ArrayList<>();
    private final List<VertexIndex> edgeVertices = new ArrayList<>();
    private int vboTriangleIndices = 0;
    private int vboTriangleAdjacencyIndices = 0;
    private int vboParticleIndices = 0;
    private int vboEdgeIndices = 0;

    private boolean useNormals = false;

    // TODO use this
    private boolean geometryIsDirty = true;
    private boolean particleIndicesAreDirty = true;
    private boolean edgeIndicesAreDirty = true;
    
    public MeshActor(MeshGeometry geometry, Material material, CompositeObject3d parent) {
        super(parent);
        this.geometry = geometry;
        this.material = material;
        
        // All materials use positions, right?
        useNormals = material.usesNormals();
        if (useNormals && ! material.hasPerFaceAttributes()) {
            if (! geometry.hasVertexNormals())
                geometry.computeVertexNormals();
        }
        
        // Keep track of when geometry changes
        geometry.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                geometryIsDirty = true;
            }
        });
        
        initializeAttributes();
    }

    private void initializeAttributes() {
        if (vertexBufferObject != null) {
            // System.out.println("Vbo already allocated");
            return;
        }
        if (geometry.size() < 1) {
            // System.out.println("Geometry has no points");
            return;
        }
        // Finalize vertex attributes
        List<VertexAttribute> attributes = new ArrayList<VertexAttribute>();
        Vertex vertex = geometry.getVertex(0);
        // TODO - interleave Vector3 and Float attributes to achieve 4-alignment
        for (String attribute : vertex.getVectorAttributeNames()) {
            if (attribute.equals("normal")) continue; // handle normals specially, below
            ConstVector v = vertex.getVectorAttribute(attribute);
            attributes.add(new VertexAttribute(attribute, v.size()));
        }
        
        if (useNormals)
            attributes.add(new VertexAttribute("normal", 3)); // either per face or per position

        for (String attribute : vertex.getFloatAttributeNames())
            attributes.add(new VertexAttribute(attribute, 1));
        
        vertexBufferObject = new MeshFloatVbo(attributes);
        
        populateVbos();
    }
    
    public MeshGeometry getGeometry() {
        return geometry;
    }

    public Material getMaterial() {
        return material;
    }

    private void populateVbos() {
        // Compute normals
        if (useNormals) {
            if (material.hasPerFaceAttributes()) {
                if (! geometry.hasTriangleNormals())
                    geometry.computeTriangleNormals();
            }
            else {
                if (! geometry.hasVertexNormals())
                    geometry.computeVertexNormals();
            }
        }
        //
        // Use duplicated vertices if there are per-face normals or per-face colors
        int vertexCount = 0;
        if (material.hasPerFaceAttributes())
            vertexCount = geometry.getTriangles().size() * 3;
        else
            vertexCount = geometry.getVertexCount();
        
        if (vertexCount < 1)
            return;
        int faceCount = geometry.getTriangles().size();
        // if (faceCount < 1)
        //     return;
        
        triangleIndexCount = 3 * faceCount;
        int vboVertexIndex = 0;
        triangleVertices.clear();
        if (material.hasPerFaceAttributes()) {
            // Make a separate copy of each vertex, for each face it is on.
            for(Triangle f : geometry.getTriangles()) {
                Vector3 triangleNormal = f.getNormal();
                for(int i : f.asArray()) {
                    Vertex v = geometry.getVertex(i);
                    triangleVertices.add(new VertexIndex(i, vboVertexIndex));
                    vboVertexIndex += 1;
                    
                    // vertexBufferObject.append(v.getPosition().toArray());
                    
                    for (String attName : v.getVectorAttributeNames()) {
                        if ( attName.equals("normal") ) {
                            continue;
                        }
                        vertexBufferObject.append(v.getVectorAttribute(attName).toNewArray());
                    }
                    if (useNormals)
                        vertexBufferObject.append(triangleNormal.toArray()); // use TRIANGLE normal in flat mode
                    for (String attName : v.getFloatAttributeNames()) {
                        vertexBufferObject.append(v.getFloatAttribute(attName));
                    }
                }
            }
        }
        else {
            for(Triangle f : geometry.getTriangles()) {
                for(int i : f.asArray()) {
                    triangleVertices.add(new VertexIndex(i, i));
                }
            }
            // In the absence of per face attributes, we can use each vertex just once
            for(int i = 0; i < geometry.getVertexCount(); ++i) {
                Vertex v = geometry.getVertex(i);
                for (String attName : v.getVectorAttributeNames()) {
                    if (attName.equals("normal") && !useNormals)
                        continue;
                    vertexBufferObject.append(v.getVectorAttribute(attName).toNewArray());
                }
                for (String attName : v.getFloatAttributeNames())
                    vertexBufferObject.append(v.getFloatAttribute(attName));
                // vertexBufferObject.append(v.getPosition().toArray());
                // System.out.println(v.getPosition());
                // if (useNormals)
                //     vertexBufferObject.append(v.getVector3Attribute("normal").toArray());
            }
       }
        
        // Edges/lines
        edgeIndexCount = 2 * geometry.getEdges().size();
        if (edgeVertices.size() > 0)
            edgeIndicesAreDirty = true;
        edgeVertices.clear();
        for (Edge edge : geometry.getEdges()) {
            edgeIndicesAreDirty = true;
            for (int i : edge.asArray())
                edgeVertices.add(new VertexIndex(i, i));
        }
        
        particleIndicesAreDirty = true;
        geometryIsDirty = false;
    }
    
    protected void refreshVbos() {
        // TODO - make this work with neuron cursor
        if (! geometryIsDirty)
            return;
        initializeAttributes();
        if (vertexBufferObject == null)
            return;
        // System.out.println("Refreshing VBOs");
        vertexBufferObject.clear();
        populateVbos();
    }
    
    @Override
    public void init(GL3 gl) 
    {
        if (vertexBufferObject == null)
            initializeAttributes();
        if (vertexBufferObject == null)
            return; // nothing to see here
        super.init(gl);
        material.init(gl);
        vertexBufferObject.init(gl);
        initTriangleIndices(gl);
    }
    
    private void initTriangleIndices(GL3 gl) {
        if (vboTriangleIndices > 0)
            return; // already initialized
        int faceCount = geometry.getTriangles().size();
        if (faceCount < 1) {
            return;
        }
        IntBuffer indices = Buffers.newDirectIntBuffer(triangleVertices.size());
        for (VertexIndex vix : triangleVertices)
            indices.put(vix.vboIndex);
        indices.flip();

        IntBuffer vbos = IntBuffer.allocate(1);
        vbos.rewind();
        gl.glGenBuffers(1, vbos);
        vboTriangleIndices = vbos.get(0);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
        gl.glBufferData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                indices.capacity() * Buffers.SIZEOF_INT,
                indices,
                GL3.GL_STATIC_DRAW);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    private void initEdgeIndices(GL3 gl)
    {
        if (vboEdgeIndices > 0)
            return; // already initialized
        
        IntBuffer vbos = IntBuffer.allocate(1);
        vbos.rewind();
        gl.glGenBuffers(1, vbos);
        vboEdgeIndices = vbos.get(0);
        
        refreshEdgeIndices(gl);
    }
    
    private void refreshEdgeIndices(GL3 gl)
    {        
        int edgeCount = geometry.getEdges().size();
        if (edgeCount < 1) {
            return;
        }
        IntBuffer indices = Buffers.newDirectIntBuffer(edgeVertices.size());
        for (VertexIndex vix : edgeVertices)
            indices.put(vix.vboIndex);
        indices.flip();
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);
        gl.glBufferData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                indices.capacity() * Buffers.SIZEOF_INT,
                indices,
                GL3.GL_STATIC_DRAW);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        edgeIndicesAreDirty = false;
    }
    
    private void initParticleIndices(GL3 gl)
    {
        if (vboParticleIndices > 0)
            return; // already initialized
        
        IntBuffer vbos = IntBuffer.allocate(1);
        vbos.rewind();
        gl.glGenBuffers(1, vbos);
        vboParticleIndices = vbos.get(0);        
        
        refreshParticleIndices(gl);
    }

    
    private void refreshParticleIndices(GL3 gl) {
        particleIndexCount = geometry.getVertexCount();
        if (particleIndexCount < 1)
            return;
        IntBuffer indices = Buffers.newDirectIntBuffer(particleIndexCount);
        for (int i = 0; i < particleIndexCount; ++i)
            indices.put(i);
        indices.flip();
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboParticleIndices);
        gl.glBufferData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                indices.capacity() * Buffers.SIZEOF_INT,
                indices,
                GL3.GL_STATIC_DRAW);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        particleIndicesAreDirty = false;
    }
    
    private void initTriangleAdjacencyIndices(GL3 gl) 
    {
        if (vboTriangleAdjacencyIndices > 0)
            return; // already initialized
        int faceCount = geometry.getTriangles().size();
        if (faceCount < 1) {
            return;
        }
        triangleAdjacencyIndexCount = 6 * faceCount;
        IntBuffer indices = Buffers.newDirectIntBuffer(triangleAdjacencyIndexCount);
        
        // TODO
        AdjacencyEdgeSet edges = new AdjacencyEdgeSet();
        // First pass populates AdjacencyEdgeSet data structure
        Iterator<VertexIndex> i = triangleVertices.iterator();
        while (i.hasNext()) {
            VertexIndex a = i.next();
            VertexIndex b = i.next();
            VertexIndex c = i.next();
            edges.addTriangle(a, b, c);
        }
        // Second pass populates triangle adjacency index buffer
        i = triangleVertices.iterator(); // reset iterator
        while (i.hasNext()) {
            VertexIndex a = i.next();
            VertexIndex b = i.next();
            VertexIndex c = i.next();
            VertexIndex ab = edges.getOtherVertex(a, b, c);
            VertexIndex bc = edges.getOtherVertex(b, c, a);
            VertexIndex ca = edges.getOtherVertex(c, a, b);
            // TODO
            indices.put(a.vboIndex);
            indices.put(ab.vboIndex);
            indices.put(b.vboIndex);
            indices.put(bc.vboIndex);
            indices.put(c.vboIndex);
            indices.put(ca.vboIndex);
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

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) 
    {
        super.display(gl, camera, parentModelViewMatrix); // display child objects
        
        if (geometry.size() < 1)
            return; // nothing to display
        
        if (geometryIsDirty) {
            refreshVbos();
        }
        
        material.display(gl, this, camera, parentModelViewMatrix);
    }

    public void displayFaces(GL3 gl) {
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());

        if (vboTriangleIndices == 0)
            initTriangleIndices(gl);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES, triangleIndexCount, GL3.GL_UNSIGNED_INT, 0);
        //
        vertexBufferObject.unbind(gl);
    }
    
    public void displayTriangleAdjacencies(GL3 gl) {
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());

        if (vboTriangleAdjacencyIndices == 0)
            initTriangleAdjacencyIndices(gl);
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboTriangleAdjacencyIndices);
        gl.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, triangleAdjacencyIndexCount, GL3.GL_UNSIGNED_INT, 0);
        //
        vertexBufferObject.unbind(gl);
    }

    public void displayEdges(GL3 gl) {
        if (vertexBufferObject == null)
            init(gl);
        if (vertexBufferObject == null)
            return;
        
        if (vboEdgeIndices == 0)
            initEdgeIndices(gl);
        
        if (edgeIndicesAreDirty)
            refreshEdgeIndices(gl);
        
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());
        
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);
        gl.glDrawElements(GL3.GL_LINES, edgeIndexCount, GL3.GL_UNSIGNED_INT, 0);
        
        vertexBufferObject.unbind(gl);
   }

    public void displayParticles(GL3 gl) {
        if (vertexBufferObject == null)
            init(gl);
        if (vertexBufferObject == null)
            return;
        
        if (vboParticleIndices == 0)
            initParticleIndices(gl);
        
        if (particleIndicesAreDirty)
            refreshParticleIndices(gl);
        
        vertexBufferObject.bind(gl, material.getShaderProgramHandle());
        
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboParticleIndices);
        gl.glDrawElements(GL3.GL_POINTS, particleIndexCount, GL3.GL_UNSIGNED_INT, 0);
        
        vertexBufferObject.unbind(gl);
   }

    @Override
    public void dispose(GL3 gl) {
        //
        int[] vbos = {vboTriangleIndices, vboTriangleAdjacencyIndices, vboParticleIndices, vboEdgeIndices};
        gl.glDeleteBuffers(4, vbos, 0);
        vboTriangleIndices = 0;
        vboTriangleAdjacencyIndices = 0;
        vboParticleIndices = 0;
        vboEdgeIndices = 0;
        
        if (vertexBufferObject != null)
            vertexBufferObject.dispose(gl);
        
        material.dispose(gl);
        
        super.dispose(gl);
    }

    private static class VertexIndex {
        // These two indices might differ, in the case of 
        int meshIndex; // Vertex index in original MeshGeometry
        int vboIndex; // Vertex index in local vertex buffer object

        VertexIndex(int meshIndex, int vboIndex) {
            this.meshIndex = meshIndex;
            this.vboIndex = vboIndex;
        }
    }
    
    /**
     * Utility class for helping to compute triangle adjacency
     */
    private static class AdjacencyEdge {
        Key key;
        VertexIndex leftVertex = null; // other triangle vertex with lower-upper-left vertex order
        VertexIndex rightVertex = null; // other triangle vertex with upper-lower-right vertex order
        
        AdjacencyEdge(VertexIndex v1, VertexIndex v2, VertexIndex v3) {
            key = new Key(v1.meshIndex, v2.meshIndex);
            if (v1.meshIndex < v2.meshIndex)
                leftVertex = v3;
            else
                rightVertex = v3;                
        }
        
        VertexIndex getOtherVertex(VertexIndex a, VertexIndex b, VertexIndex c) {
            if (a.meshIndex < b.meshIndex)
                return rightVertex != null ? rightVertex : leftVertex;
            else
                return leftVertex != null ? leftVertex : rightVertex;
        }
        
        void insertEdge(VertexIndex v1, VertexIndex v2, VertexIndex v3) {
            if (v1.meshIndex < v2.meshIndex) {
                leftVertex = v3;
            } else {
                rightVertex = v3;                
            }            
        }

        static class Key {
            int smaller;
            int larger;
            
            Key(int v1, int v2) {
                if (v1 < v2) {
                    smaller = v1;
                    larger = v2;
                }
                else {
                    smaller = v2;
                    larger = v1;
                }
            }

            @Override
            public int hashCode() {
                int hash = 7;
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Key other = (Key) obj;
                if (this.smaller != other.smaller) {
                    return false;
                }
                if (this.larger != other.larger) {
                    return false;
                }
                return true;
            }
        }
    }
    
    private static class AdjacencyEdgeSet {
        Map<AdjacencyEdge.Key, AdjacencyEdge> edges = new HashMap<AdjacencyEdge.Key, AdjacencyEdge>();
        
        void addEdge(VertexIndex a, VertexIndex b, VertexIndex c) {
            AdjacencyEdge.Key k = new AdjacencyEdge.Key(a.meshIndex, b.meshIndex);
            AdjacencyEdge e;
            if (edges.containsKey(k))
                e = edges.get(k);
            else {
                e = new AdjacencyEdge(a, b, c);
                edges.put(k, e);
            }
            e.insertEdge(a, b, c);
        }
        
        void addTriangle(VertexIndex a, VertexIndex b, VertexIndex c) {
            addEdge(a, b, c);
            addEdge(b, c, a);
            addEdge(c, a, b);
        }
        
        VertexIndex getOtherVertex(VertexIndex a, VertexIndex b, VertexIndex c) {
            AdjacencyEdge.Key k = new AdjacencyEdge.Key(a.meshIndex, b.meshIndex);
            return edges.get(k).getOtherVertex(a, b, c);
        }

    }

}
