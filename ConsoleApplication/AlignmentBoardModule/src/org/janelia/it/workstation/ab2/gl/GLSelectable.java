package org.janelia.it.workstation.ab2.gl;

import org.janelia.it.workstation.ab2.event.AB2EventHandler;

public interface GLSelectable extends AB2EventHandler {

    public void setHover();

    public void releaseHover();

    public void setSelect();

    public void releaseSelect();

    public void setDrag();

    public void releaseDrag();

    public boolean isSelectable();

    public boolean acceptsDropType(GLSelectable selectable);

}
