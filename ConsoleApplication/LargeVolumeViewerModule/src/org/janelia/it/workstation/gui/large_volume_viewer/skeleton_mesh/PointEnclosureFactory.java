/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;

/**
 *
 * @author fosterl
 */
public class PointEnclosureFactory implements TriangleSource  {
    private List<VertexInfoBean> vtxInfoBeans;
    private List<Triangle> triangles;
    
    public PointEnclosureFactory(int numSides, double radius) {
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

    private void setup(int numSides, double radius) {
        // Make a lot of polygons: as many as requested number of sides.
        double hypot = radius;
        double angularIteration = Math.PI / 4.0 / numSides;
        List<Matrix> points = new ArrayList<>();
        // Growing forward, to midline.
        for (int i = 0; i < numSides - 1; i++) {
            double y = Math.sin(angularIteration * (numSides - i)) * hypot;
            double z = Math.cos(angularIteration * (numSides - i)) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createTransform(hypot, y, z);
            addResult(polygon, transform, points);
        }
        // Growing beyond midline.
        for (int i = 0; i < numSides - 1; i++) {
            double y = Math.sin(angularIteration * i) * hypot;
            double z = Math.cos(angularIteration * i) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createTransform(hypot, y, z);            
            addResult(polygon, transform, points);
        }
        
        vtxInfoBeans = new ArrayList<>();
        // Traverse all the vertices.
        int vtxBufOffset = 0;
        for ( Matrix point: points ) {
            VertexInfoBean bean = new VertexInfoBean();
            
            VertexInfoKey key = new VertexInfoKey();
            key.setPosition(new double[] { point.get(0, 0), point.get(1, 0), point.get(2, 0) });
            bean.setKey(key);
            bean.setVtxBufOffset(vtxBufOffset ++);
            
            vtxInfoBeans.add( bean );
        }
        
        // Traverse vertex beans, to make triangles.
        triangles = new ArrayList<>();
    }

    protected void addResult(double[][] polygon, Matrix transform, List<Matrix> rings) {
        Matrix pm = new Matrix(polygon);
        Matrix result = pm.times(transform);
        rings.add(result);
    }

    protected Matrix createTransform(double hypot, double y, double z) {
        Matrix transform = Matrix.identity(4, 4);
        transform.set(1, 3, hypot - y);
        transform.set(2, 3, z);
        return transform;
    }
}
