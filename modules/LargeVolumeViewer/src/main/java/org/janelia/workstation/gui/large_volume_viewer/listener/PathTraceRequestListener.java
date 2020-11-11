package org.janelia.workstation.gui.large_volume_viewer.listener;

import org.janelia.workstation.gui.large_volume_viewer.tracing.PathTraceToParentRequest;

/**
 * Implement this to hear about requests to trace the path.
 * @author fosterl
 */
public interface PathTraceRequestListener {
    void pathTrace(PathTraceToParentRequest request);
}
