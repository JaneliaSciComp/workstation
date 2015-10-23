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

import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;

import java.util.*;

/**
 *
 * @author brunsc
 */
public class MeshGeometry 
implements Collection<Vertex>, ObservableInterface
{

    protected final List<Vertex> vertices = new ArrayList<>();
    
    // Vertex attributes: colors, normals
    // private final List<Color> colors;
    // private final List<Vector3> normals;
    //
    private final List<Face> faces = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();
    private final Set<Edge> edges = new LinkedHashSet<>(); // unique edges
    
    private final Collection[] collections = {vertices, edges, faces, triangles};
    
    private final Box3 boundingBox = new Box3();
    private boolean boundingBoxIsDirty = false;
    
    private final ComposableObservable observable = new ComposableObservable();

    public MeshGeometry() {
        addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                boundingBoxIsDirty = true;
            }
        });
    }
    
    /**
     * 
     * returns new vertex
    */
    public Vertex addVertex(float x, float y, float z) {
        return addVertex(new Vector3(x, y, z));
    }
    
    public Vertex addVertex(float[] xyz) {
        return addVertex(new Vector3(xyz));
    }
    
    public Vertex addVertex(ConstVector3 v) {
        Vertex result = new Vertex(v);
        if (add(result))
            return result;
        else
            return null;
    }
    
    public int addEdge(int ix1, int ix2) {
        return addEdge(new Edge(ix1, ix2));
    }
    
    public int addEdge(Edge edge) {
        if (edges.add(edge))
            setChanged();
        return edges.size() - 1;
    }
    
//    public int addFace(Integer[] indices) {
//        return addFace(new Face(indices));
//    }
    
    public int addFace(int[] indices) {
        return addFace(new Face(indices));
    }
    
    public int addFace(Face face) {
        List<Integer> v = face.getVertices();
        faces.add(face);
        // Triangles
        // Decompose face into triangles
        for (int i = 2; i < v.size(); ++i) {
            this.triangles.add(new Triangle(
                    v.get(0), v.get(i-1), v.get(i)));
        }
        // Edges
        for (int i = 1; i < v.size(); ++i)
            this.edges.add(new Edge(v.get(i-1), v.get(i)));
        // Close loop by connecting first vertex to last vertex
        if (v.size() > 1)
            this.edges.add(new Edge(v.get(0), v.get(v.size() - 1)));
        return faces.size() - 1;
    }
    
    /**
     * Removes all vertices, faces, edges, triangles, and reset bounding box.
     */
    @Override
    public void clear() {
        boolean changed = false;
        for (Collection c : collections) {
            if (c.size() > 0)
                changed = true;
            c.clear();
        }
        if (changed)
            setChanged();
        boundingBox.clear();
    }
    
    /**
     * Computes normal of a triangle consisting of three vertices.
     * @param vi1 - vertex 1
     * @param vi2 - vertex 2
     * @param vi3 - vertex 3
     * @return Vector3 object
     */
    public Vector3 computeTriangleNormal(int vi1, int vi2, int vi3) {
        Vector3 v1 = vertices.get(vi1).getPosition();
        Vector3 v2 = vertices.get(vi2).getPosition();
        Vector3 v3 = vertices.get(vi3).getPosition();
        Vector3 v21 = new Vector3(v2).sub(v1);
        Vector3 v23 = new Vector3(v2).sub(v3);
        return v23.cross(v21).normalize();
    }
    
    public void computeTriangleNormals() {
        for (Triangle f : triangles) {
            Vector3 normal = computeTriangleNormal(f.getVertexIndex(0), f.getVertexIndex(1), f.getVertexIndex(2));
            f.setNormal(normal);
            // System.out.println("triangle normal: "+normal);
        }
    }
    
    public Set<Edge> getEdges() {
        return edges;
    }

    // public List<Color> getColors() {
    //     return colors;
    // }

    public List<Triangle> getTriangles() {
        return triangles;
    }
    
    private List<Vertex> getVertices() {
        return vertices;
    }

    public void computeVertexNormals() {
        if (! hasTriangleNormals())
            computeTriangleNormals();
        // normals.clear();
        for (Vertex v : vertices) // initialize to zero vector
            v.setAttribute("normal", new Vector3(0,0,0));
        final int[] vx = new int[]{0, 1, 2}; // cache vertex indices for iteration
        for (Triangle f : triangles) { // sum face normal contributions
            // weight face normal by cosine of angle at each vertex
            // cache vertex positions for concise access
            Vector3[] fv = new Vector3[3];
            for (int i : vx)
                fv[i] = vertices.get(f.getVertexIndex(i)).getPosition();
            // compute lengths of triangle sides
            float[] sideLengths = new float[3];
            for (int i : vx) {
                // indices of other two sides
                int j = (i+1) % 3;
                int k = (i+2) % 3;
                sideLengths[i] = new Vector3(fv[j]).sub(fv[k]).length();
            }
            // compute angle at each triangle vertex
            for (int i : vx) {
                // indices of other two sides
                int j = (i+1) % 3;
                int k = (i+2) % 3;
                // cosine law
                float a = sideLengths[i];
                float b = sideLengths[j];
                float c = sideLengths[k];
                float angle = (float)Math.acos((b*b + c*c - a*a)/(2*b*c));
                // Store weighted normal
                Vector3 n = new Vector3(f.getNormal()).multiplyScalar(angle);
                // System.out.println(a+", "+b+", "+c+": "+angle+"; normal = "+n);
                Vector3 oldNormal = (Vector3) vertices.get(f.getVertexIndex(i)).getVectorAttribute("normal");
                oldNormal.add(n);
                // normals.get(f.getVertexIndex(i)).add(n);
            }
        }
        // normalize all vertex normals to unit length
        for (Vertex v : vertices) {
            Vector3 n = (Vector3)v.getVectorAttribute("normal");
            // System.out.println(v.getPosition() + ": normal: " + n);
            if (n.lengthSquared() > 0) n.normalize();
        }
        // for (Vector3 n : normals) 
        //     if (n.lengthSquared() > 0) n.normalize();
    }

    // public List<Vector3> getNormals() {
    //     return normals;
    // }
    
//    public boolean hasFaceColors() {
//        if (faces.size() < 1) return false;
//        Face face = faces.iterator().next();
//        return face.getColor() != null;
//    }

    public boolean hasTriangleNormals() {
        if (triangles.size() < 1) return false;
        Triangle face = triangles.iterator().next();
        return face.getNormal() != null;
    }

    public boolean hasVertexNormals() {
        return vertices.size() >= 1 && vertices.get(0).hasAttribute("normal");
    }
    
    // public boolean hasVertexColors() {
    //     return colors.size() > 0;
    // }

    /**
     * Creates a now MeshGeometry, with the order of face vertices reversed.
     * This is useful for transforming the original Utah teapot to 
     * CCW convention.
     *
     */
//    public MeshGeometry reverseFaces() {
//        MeshGeometry result = new MeshGeometry();
//        for (Vertex v : vertices)
//            result.getVertices().add(new Vertex(v.getPosition()));
//        for (Face f : faces) {
//            List<Integer> ix = f.getVertices();
//            ArrayList rev = new ArrayList(ix);
//            Collections.reverse(rev);
//            result.addFace(new Face(rev));
//        }
//        result.notifyObservers();
//        return result;
//    }
    
    // Reverse vertex order of faces and triangles that do not match their 
    // vertices normal directions
//    public void correctFaceOrders() {
//        if (! hasVertexNormals())
//            return;
//        for (Face f : faces) {
//            Vector3 vnorm = new Vector3(0,0,0); // normal based on vertex normals
//            for (int vi : f.getVertices())
//                vnorm.add((Vector3)vertices.get(vi).getVectorAttribute("normal"));
//            Vector3 fnorm = new Vector3(0,0,0); // normal based on ordered face vertices
//            // sum contibutions from triangles that make up this face
//            for (int fvi = 2; fvi < f.getVertices().size(); ++fvi) {
//                fnorm.add(computeTriangleNormal(
//                        f.getVertices().get(0),
//                        f.getVertices().get(fvi-1),
//                        f.getVertices().get(fvi)));
//            }
//            if (fnorm.dot(vnorm) < 0) // normal directions disagree
//                Collections.reverse(f.getVertices());
//        }
//    }
    
    // Reverse vertex order of faces and triangles that do not match their 
    // vertices normal directions
//    public void correctTriangleOrders() {
//        if (! hasVertexNormals())
//            return;
//        for (Triangle t : triangles) {
//            Vector3 vnorm = new Vector3(0,0,0); // normal based on vertex normals
//            int[] a = t.asArray();
//            for (int vi : a)
//                vnorm.add((Vector3)vertices.get(vi).getVectorAttribute("normal"));
//            Vector3 fnorm = computeTriangleNormal(a[0], a[1], a[2]);
//            if (fnorm.dot(vnorm) < 0) { // normal directions disagree, so flip
//                int[] rev = {a[2], a[1], a[0]};
//                for (int i = 0; i < 3; ++i)
//                    a[i] = rev[i];
//            }
//        }
//    }

    public Box3 getBoundingBox() {
        if (boundingBoxIsDirty) {
            boundingBox.clear();
            for (Vertex vtx : getVertices()) {
                boundingBox.include(vtx.getPosition());
            }
            boundingBoxIsDirty = false;
        }
        return boundingBox;
    }
    
    public Vertex getVertex(int index) {
        return getVertices().get(index);
    }
    
    public int getVertexCount() {
        return getVertices().size();
    }

    @Override
    public int size() {
        return vertices.size();
    }

    @Override
    public boolean isEmpty() {
        for (Collection c : collections)
            if (! c.isEmpty()) return false;
        return true;
    }

    @Override
    public boolean contains(Object o) {
        for (Collection c : collections)
            if (c.contains(o)) return true;
        return false;
    }

    @Override
    public Iterator<Vertex> iterator() {
        return vertices.iterator();
    }

    @Override
    public Object[] toArray() {
        return vertices.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return vertices.toArray(a);
    }

    @Override
    public boolean add(Vertex e) {
        boolean result = vertices.add(e);
        if (result)
            setChanged();
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = vertices.remove(o);
        // Next check non-vertex items
        for (Collection coll : collections) {
            if (coll == vertices) continue;
            if (coll.remove(o)) result = true;
        }        
        if (result)
            observable.setChanged();
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        boolean result = false;
        for (Collection coll : collections) {
            if (coll.containsAll(c)) result = true;
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends Vertex> c) {
        boolean result = vertices.addAll(c);
        if (result)
            observable.setChanged();
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // First do vertices, and maybe change bounding box
        boolean result = vertices.removeAll(c);
        if (result)
            observable.setChanged();
        // Next check non-vertex items
        for (Collection coll : collections) {
            if (coll == vertices) continue;
            if (coll.removeAll(c)) result = true;
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // First do vertices, and maybe change bounding box
        boolean result = vertices.retainAll(c);
        if (result)
            observable.setChanged();
        // Next check non-vertex items
        for (Collection coll : collections) {
            if (coll == vertices) continue;
            if (coll.retainAll(c)) result = true;
        }
        return result;
    }

    @Override
    public void setChanged() {
        observable.setChanged();
    }

    @Override
    public void notifyObservers() {
        observable.notifyObservers();
    }

    @Override
    public void addObserver(Observer observer) {
        observable.addObserver(observer);
    }
    
    @Override
    public void deleteObserver(Observer observer) {
        observable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers() {
        observable.deleteObservers();
    }
    
}
