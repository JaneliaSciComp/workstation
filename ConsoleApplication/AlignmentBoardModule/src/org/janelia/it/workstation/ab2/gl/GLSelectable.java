package org.janelia.it.workstation.ab2.gl;

import org.janelia.it.workstation.ab2.event.AB2EventHandler;

public interface GLSelectable extends AB2EventHandler {

    public void setHover(int hoveringActorId);

    public void releaseHover();

    public void setSelect();

    public void releaseSelect();

    public void setDrag();

    public void releaseDrag();

}
