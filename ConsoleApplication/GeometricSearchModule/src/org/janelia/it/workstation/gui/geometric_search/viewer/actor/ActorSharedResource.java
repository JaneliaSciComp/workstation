package org.janelia.it.workstation.gui.geometric_search.viewer.actor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 9/4/2015.
 */
public abstract class ActorSharedResource {

    String name;
    List<Actor> sharedActorList=new ArrayList<>();

    protected ActorSharedResource(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Actor> getSharedActorList() {
        return sharedActorList;
    }

    public void setSharedActorList(List<Actor> sharedActorList) {
        this.sharedActorList = sharedActorList;
    }

    public abstract void load();

}
