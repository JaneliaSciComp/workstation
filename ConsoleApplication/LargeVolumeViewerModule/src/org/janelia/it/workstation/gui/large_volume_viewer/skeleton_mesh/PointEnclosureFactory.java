/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;

/**
 * Creates triangles and their constituent vertices, to wrap a point
 * with a sphere.
 * 
 * @author fosterl
 */
public class PointEnclosureFactory implements TriangleSource  {
    private final List<VertexInfoBean> vtxInfoBeans;
    private final List<Triangle> triangles;
    
    private final List<Matrix> prototypePoints;
    
    private final Map<Integer,VertexInfoBean> offsetToVertex;
    private int numSides;
    
    private int vtxBufOffset = 0;
    
    public PointEnclosureFactory(int numSides, double radius) {
        this.numSides = numSides;
        vtxInfoBeans = new ArrayList<>();
        triangles = new ArrayList<>();
        prototypePoints = new ArrayList<>();
        offsetToVertex = new HashMap<>();
        setup(numSides, radius);
    }

    @Override
    public List<VertexInfoBean> getVertices() {
        return vtxInfoBeans;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    public void addEnclosure(double[] pointCoords, float[] color) {
        // Making a new bean for every point.
        int enclosureBaseIndex = vtxBufOffset;
        for (Matrix point: prototypePoints) {
            beanFromPoint(point, pointCoords, color);
        }
        createTriangles(numSides, enclosureBaseIndex);
    }
    
    private void setup(int numSides, double radius) {
        // Make a lot of polygons: as many as requested number of sides.
        double hypot = radius;
        final int ringCount = (int) (double) (numSides / 2.0);
        double angleOffset = Math.PI / 20.0;
        double angularIteration =
                ((Math.PI / 2.0) - 2 * angleOffset) / ringCount;
        double angle = angleOffset;
        // Growing forward, to midline.
        for (int i = 0; i < ringCount; i++) {
            double y = Math.sin(angle) * hypot;
            double z = -Math.cos(angle) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createPrototypeTransform(hypot, y, z);
            addResult(polygon, transform, prototypePoints);
            
            angle += angularIteration;
        }
        
        angle -= angularIteration; // Push back in other direction.
        
        // Growing beyond midline.        
        for (int i = 0; i < ringCount; i++) {
            double y = Math.sin(angle) * hypot;
            double z = Math.cos(angle) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createPrototypeTransform(hypot, y, z);            
            addResult(polygon, transform, prototypePoints);
            angle -= angularIteration;
        }
        vtxBufOffset = 0; // Re-using this counter.
    }

    /**
     * Given a point matrix, create a new vertex bean.
     * 
     * @param point triple telling position of point.
     */
    private VertexInfoBean beanFromPoint(Matrix point, double[] pointCoords, float[] color) {
        VertexInfoBean bean = new VertexInfoBean();
        
        VertexInfoKey key = new VertexInfoKey();
        // Move the coords to be relative to the incoming point coords.
        Matrix transform = Matrix.identity(4, 4);
        transform.set(0, 3, pointCoords[0]);
        transform.set(1, 3, pointCoords[1]);
        transform.set(2, 3, pointCoords[2]);
        point = transform.times(point);
        
        key.setPosition(new double[] { point.get(0, 0), point.get(1, 0), point.get(2, 0) });
        bean.setKey(key);
        bean.setAttribute( VertexInfoBean.KnownAttributes.b_color.toString(), color, 3 );
        offsetToVertex.put( vtxBufOffset, bean);
        bean.setVtxBufOffset(vtxBufOffset ++);
        
        vtxInfoBeans.add( bean );
        return bean;
    }

    private void addResult(double[][] polygon, Matrix transform, List<Matrix> points) {        
        for (double[] polygonPoint: polygon) {
            Matrix pm = new Matrix(4, 1);
            for ( int i = 0; i < polygonPoint.length; i++ ) {
                pm.set(i, 0, polygonPoint[i]);
            }
            pm.set(3, 0, 1.0); //Ensures transforms have something to work with.
            Matrix result = transform.times(pm);
            points.add(result);
        }
    }

    private void createTriangles(int numSides, int offset) {
        // Now create triangles.
        for (int i = 0; i < prototypePoints.size(); i++) {
            Triangle triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get(offset + i));
            triangle.addVertex(offsetToVertex.get(offset + i + 1));
            triangle.addVertex(offsetToVertex.get(offset + i + numSides));
//            triangle[0] = offset + i;
//            triangle[1] = offset + i + 1;
//            triangle[2] = offset + i + numSides;
            triangles.add(triangle);

            triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get(offset + i + 1));
            triangle.addVertex(offsetToVertex.get(offset + i + numSides + 1));
            triangle.addVertex(offsetToVertex.get(offset + i + numSides));
//            triangle[0] = offset + i + 1;
//            triangle[1] = offset + i + numSides + 1;
//            triangle[2] = offset + i + numSides;
            triangles.add(triangle);
        }

        // Include end caps.
        // Winding to point close end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get(offset + 0));
            triangle.addVertex(offsetToVertex.get(offset + (numSides - i - 1)));
            triangle.addVertex(offsetToVertex.get(offset + (numSides - i - 2)));
//            triangle[0] = offset + 0;
//            triangle[1] = offset + (numSides - i - 1);
//            triangle[2] = offset + (numSides - i - 2);
            triangles.add(triangle);
        }

        // Winding to point far end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            int initialVertex = prototypePoints.size() - numSides;
            triangle.addVertex(offsetToVertex.get(offset + initialVertex));
            triangle.addVertex(offsetToVertex.get(offset + initialVertex + i + 2));
            triangle.addVertex(offsetToVertex.get(offset + initialVertex + i + 1));
//            triangle[0] = offset + initialVertex;
//            triangle[1] = offset + initialVertex + i + 2;
//            triangle[2] = offset + initialVertex + i + 1;
            triangles.add(triangle);
        }
    }

    private Matrix createPrototypeTransform(double hypot, double y, double z) {
        Matrix transform = Matrix.identity(4, 4);
        transform.set(1, 3, hypot - y);
        transform.set(2, 3, z);
        return transform;
    }
    
    @SuppressWarnings("unused")
    private void dumpPointMatrix(Matrix point) {
        point.print(10, 4);
    }
    
}
