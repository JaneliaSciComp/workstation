package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * This essentially tracks which vertices are in a given triangle.  It is necessary to keep track of triangles
 * so that the vertices, themselves can have including-triangle-averaged normal vectors calculated for them.
 *
 * Created by fosterl on 4/3/14.
 */
public class Triangle {
    private List<VertexInfoBean> vertices = new ArrayList<VertexInfoBean>();
    private VertexFactory.NormalDirection normalVector;
    public void addVertex( VertexInfoBean bean ) {
        vertices.add( bean );
    }

    public VertexFactory.NormalDirection getNormalVector() {
        return normalVector;
    }

    public void setNormalVector(VertexFactory.NormalDirection normalVector) {
        this.normalVector = normalVector;
    }
    public List<VertexInfoBean> getVertices() { return vertices; }
}
