package org.janelia.workstation.mesh_loader;

import java.io.File;

/**
 * Created by fosterl on 5/5/14.
 */
public interface VertexExporterI {
    void exportVertices(File outputLocation, String filenamePrefix) throws Exception;
}
