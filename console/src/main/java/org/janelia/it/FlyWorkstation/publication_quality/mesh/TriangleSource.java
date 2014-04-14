package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.List;

/**
 * Created by fosterl on 4/11/14.
 */
public interface TriangleSource {
    /**
     * Gets the full list of vertices.
     *
     * @return all vertices produced to this point.
     */
    List<VertexInfoBean> getVertices();

    /**
     * Gets the full list of triangles, which have references to the vertices above.
     *
     * @return all triangles produced to this point.
     */
    List<Triangle> getTriangleList();

}
