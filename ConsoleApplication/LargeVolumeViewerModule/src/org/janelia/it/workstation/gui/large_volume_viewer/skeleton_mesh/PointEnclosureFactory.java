/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Integer, VertexInfoBean> offsetToVertex;
    
    private PointPrototypeHelper prototypeHelper;
    private final VertexNumberGenerator vertexNumberGenerator;
    
    private int numSides;
    
    public PointEnclosureFactory(int numSides, double radius, VertexNumberGenerator vertexNumberGenerator) {
        vtxInfoBeans = new ArrayList<>();
        triangles = new ArrayList<>();
        offsetToVertex = new HashMap<>();
        this.vertexNumberGenerator = vertexNumberGenerator;

        setCharacteristics(numSides, radius);
    }

    /**
     * If transitioning to a different number-of-sides or radius, invoke this.
     * It will also be called at construction.
     * 
     * @param numSides how many sides in the "lateral rings" making the sphere?
     * @param radius how wide is the largest lateral ring?
     */
    public final void setCharacteristics(int numSides, double radius) {
        this.numSides = numSides;
        prototypeHelper = new PointPrototypeHelper(numSides, radius);
    }

    @Override
    public List<VertexInfoBean> getVertices() {
        return vtxInfoBeans;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    public void addEnclosure(double[] pointCoords, float[] color, float[] id) {
        // Making a new bean for every point.
        int enclosureBaseIndex = vertexNumberGenerator.getCurrentVertex();
        for (Matrix point: prototypeHelper.getPrototypePoints()) {
            beanFromPoint(point, pointCoords, color, id);
        }
        createTriangles(numSides, enclosureBaseIndex);
    }
    
    /**
     * Given a point matrix, create a new vertex bean.
     * 
     * @param point triple telling position of point.
     */
    private VertexInfoBean beanFromPoint(Matrix point, double[] pointCoords, float[] color, float[] id) {
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
        bean.setAttribute( NeuronTraceVtxAttribMgr.ID_VTX_ATTRIB, id, 3 );
        int beanVertexNumber = vertexNumberGenerator.allocateVertexNumber();
        offsetToVertex.put( beanVertexNumber, bean );
        bean.setVtxBufOffset(beanVertexNumber);
        
        vtxInfoBeans.add( bean );
        return bean;
    }

    private void createTriangles(int numSides, int vtxOffset) {
        List<Matrix> prototypePoints = prototypeHelper.getPrototypePoints();

        // Now create triangles.
        for (int i = 0; i < numSides - 1; i++) {
            for (int j = 0; j < numSides; j++) {
                int ringOffset = i * numSides + vtxOffset + j;
                // Need to link the triangle ring back to start-of-ring.
                int triangleStartVertex = ringOffset + 1;
                if (triangleStartVertex % numSides == 0) {
                    triangleStartVertex -= numSides;
                }

                Triangle triangle = new Triangle();
                triangle.addVertex(offsetToVertex.get(ringOffset));
                triangle.addVertex(offsetToVertex.get(ringOffset + numSides));
                triangle.addVertex(offsetToVertex.get(triangleStartVertex));
                triangles.add(triangle);

                triangle = new Triangle();
                int endOfTri2 = ringOffset + numSides + 1;
                if (endOfTri2 % numSides == 0) {
                    endOfTri2 -= numSides;
                }
                triangle.addVertex(offsetToVertex.get(triangleStartVertex));
                triangle.addVertex(offsetToVertex.get(ringOffset + numSides));
                triangle.addVertex(offsetToVertex.get(endOfTri2));
                triangles.add(triangle);
            }
        }

        // Include end caps.
        // Winding to point close end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            triangle.addVertex(offsetToVertex.get(vtxOffset + 0));
            triangle.addVertex(offsetToVertex.get(vtxOffset + (numSides - i - 2)));
            triangle.addVertex(offsetToVertex.get(vtxOffset + (numSides - i - 1)));
            triangles.add(triangle);
        }

        // Winding to point far end away from sphere.
        for (int i = 0; i < (numSides - 2); i++) {
            Triangle triangle = new Triangle();
            int initialVertex = prototypePoints.size() - numSides;
            triangle.addVertex(offsetToVertex.get(vtxOffset + initialVertex));
            triangle.addVertex(offsetToVertex.get(vtxOffset + initialVertex + i + 2));
            triangle.addVertex(offsetToVertex.get(vtxOffset + initialVertex + i + 1));
            triangles.add(triangle);
        }
    }

}
