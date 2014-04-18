package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.util.List;
import java.util.Map;

/**
 * Implement this to create a manager of mesh vertices.
 * Created by fosterl on 4/18/14.
 */
public interface VertexAttributeManagerI {
    List<TriangleSource> execute() throws Exception;
    Map<Long,RenderBuffersBean> getRenderIdToBuffers();
    void close();
}
