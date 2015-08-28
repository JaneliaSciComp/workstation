package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import java.awt.*;

/**
 * Created by murphys on 8/28/2015.
 */
public class RowSelectedEvent extends VoxelViewerEvent {

    Component component;

    public RowSelectedEvent(Component component) {
        this.component=component;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
}
