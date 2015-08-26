package org.janelia.it.workstation.gui.geometric_search.viewer.renderable;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by murphys on 8/7/2015.
 */
public abstract class Renderable {

    protected Actor actor;

    protected String name;
    
    protected boolean visible=true;

    protected Vector4 preferredColor=new Vector4(0.2f, 0.2f, 0.2f, 0.2f);

    public Vector4 getPreferredColor() {
        return preferredColor;
    }

    public void setPreferredColor(Vector4 preferredColor) {
        this.preferredColor = preferredColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }
    
    public void setVisible(boolean visible) {
        this.visible=visible;
    }
    
    public boolean isVisibile() {
        return visible;
    }

    Map<String, Object> parameterMap=new HashMap<>();

    public abstract Actor createAndSetActor();

    public abstract void disposeActor();

}
