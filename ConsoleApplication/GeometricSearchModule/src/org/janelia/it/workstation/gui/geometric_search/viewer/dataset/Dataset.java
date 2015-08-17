package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public abstract class Dataset {

    private String name;

    public void setName(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    List<Renderable> renderableList=new ArrayList<>();

    public abstract boolean createRenderables();

    public List<Renderable> getRenderableList() {
        return renderableList;
    }

}
