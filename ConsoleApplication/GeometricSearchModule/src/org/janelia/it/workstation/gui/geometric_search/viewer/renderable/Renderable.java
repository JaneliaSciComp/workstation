package org.janelia.it.workstation.gui.geometric_search.viewer.renderable;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by murphys on 8/7/2015.
 */
public class Renderable {

    protected String name;
    
    protected boolean visible=true;

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

}
