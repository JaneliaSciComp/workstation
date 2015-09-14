package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import java.awt.*;

/**
 * Created by murphys on 9/4/2015.
 */
public class BackgroundColorChangeEvent extends VoxelViewerEvent {

    Color backgroundColor;

    public BackgroundColorChangeEvent(Color backgroundColor) {
        this.backgroundColor=backgroundColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
