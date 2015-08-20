package org.janelia.it.workstation.gui.geometric_search.viewer.renderable;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.AddRenderableEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.ClearAllRenderablesEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/20/2015.
 */
public class RenderableModel implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RenderableModel.class);

    List<Renderable> renderableList = new ArrayList<>();

    public void addRenderable(Renderable renderable) {
        renderableList.add(renderable);
        EventManager.sendEvent(this, new AddRenderableEvent(renderable));
    }

    public void clear() {
        renderableList.clear();
        EventManager.sendEvent(this, new ClearAllRenderablesEvent());
    }

    @Override
    public void processEvent(VoxelViewerEvent event) {

    }


}
