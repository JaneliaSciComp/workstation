package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.Renderable;

/**
 * Created by murphys on 8/20/2015.
 */
public class RenderableAddedEvent extends VoxelViewerEvent {

    Renderable renderable;

    public RenderableAddedEvent(Renderable renderable) {
        this.renderable = renderable;
    }

    public Renderable getRenderable() {
        return renderable;
    }

    public void setRenderable(Renderable renderable) {
        this.renderable = renderable;
    }

}
