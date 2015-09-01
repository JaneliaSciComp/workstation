package org.janelia.it.workstation.gui.geometric_search.viewer.event;

/**
 * Created by murphys on 8/28/2015.
 */
public class ActorSetVisibleEvent extends VoxelViewerEvent {

    String name;
    boolean isVisible;

    public ActorSetVisibleEvent(String name, boolean isVisible) {
        this.name=name;
        this.isVisible=isVisible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
}
