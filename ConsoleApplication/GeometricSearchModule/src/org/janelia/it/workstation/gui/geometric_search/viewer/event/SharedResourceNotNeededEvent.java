package org.janelia.it.workstation.gui.geometric_search.viewer.event;

/**
 * Created by murphys on 9/8/2015.
 */
public class SharedResourceNotNeededEvent extends VoxelViewerEvent {

    String resourceName;

    public SharedResourceNotNeededEvent(String name) {
        this.resourceName=name;
    }

    public String getResourceName() {
        return resourceName;
    }
}
