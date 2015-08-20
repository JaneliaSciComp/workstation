package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;

/**
 * Created by murphys on 8/18/2015.
 */
public interface VoxelViewerEventListener {

    void processEvent(VoxelViewerEvent event);

}
