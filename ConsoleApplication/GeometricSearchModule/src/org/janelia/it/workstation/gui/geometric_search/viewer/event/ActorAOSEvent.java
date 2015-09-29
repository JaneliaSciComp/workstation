package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;

/**
 * Created by murphys on 9/25/2015.
 */
public class ActorAOSEvent extends VoxelViewerEvent {

    public static final String ALL_TYPE="ALL_TYPE";
    public static final String OFF_TYPE="OFF_TYPE";
    public static final String SOLO_TYPE="SOLO_TYPE";

    String actorName;
    String aosType;
    boolean isSelected;

    public ActorAOSEvent(String actorName, String aosType, boolean isSelected) {
        this.actorName=actorName;
        this.aosType=aosType;
        this.isSelected=isSelected;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getAosType() {
        return aosType;
    }

    public void setAosType(String aosType) {
        this.aosType = aosType;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
