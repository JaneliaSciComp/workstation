
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
