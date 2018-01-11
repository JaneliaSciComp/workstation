package org.janelia.it.workstation.ab2.gl;

import org.janelia.it.workstation.ab2.event.AB2EventHandler;

public interface GLSelectable extends AB2EventHandler {

    public boolean isHoverable();
    public void setHoverable(boolean hoverable);
    public void setHover();
    public void releaseHover();
    public boolean isHovered();

    public boolean isSelectable();
    public void setSelectable(boolean selectable);
    public void setSelect();
    public void releaseSelect();
    public boolean isSelected();

    public boolean isDraggable();
    public void setDraggable(boolean draggable);
    public void setDrag();
    public void releaseDrag();
    public boolean isDragging();

    public boolean acceptsDropType(GLSelectable selectable);

}
