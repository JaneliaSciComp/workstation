package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.RenderableAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.Renderable;

/**
 * Created by murphys on 8/20/2015.
 */
public class RenderablePanel extends ScrollableRowPanel implements VoxelViewerEventListener {

    @Override
    public void processEvent(VoxelViewerEvent event) {
        if (event instanceof RenderableAddedEvent) {
            RenderableAddedEvent renderableAddedEvent=(RenderableAddedEvent)event;
            Renderable renderable=renderableAddedEvent.getRenderable();
            addEntry(renderable.getName());
        }
    }
}
