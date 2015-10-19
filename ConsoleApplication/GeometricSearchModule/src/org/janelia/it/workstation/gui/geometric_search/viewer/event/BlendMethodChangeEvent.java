package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import java.awt.*;

/**
 * Created by murphys on 9/23/2015.
 */
public class BlendMethodChangeEvent extends VoxelViewerEvent {

    int blendMethod;

    public BlendMethodChangeEvent(int blendMethod) {
        this.blendMethod=blendMethod;
    }

    public int getBlendMethod() {
        return blendMethod;
    }

    public void setBlendMethod(int blendMethod) {
        this.blendMethod=blendMethod;
    }

}
