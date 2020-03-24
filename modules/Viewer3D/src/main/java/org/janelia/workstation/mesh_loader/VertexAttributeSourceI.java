/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.workstation.mesh_loader;

import java.util.List;
import java.util.Map;

/**
 * Implementations will provide mesh vertices, and related attributes.
 * Created by fosterl on 4/18/14.
 */
public interface VertexAttributeSourceI extends VertexExporterI {
    List<TriangleSource> execute() throws Exception;
    Map<Long, RenderBuffersBean> getRenderIdToBuffers();
    void close();
}
