package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.workstation.ab2.event.AB2EventHandler;

public abstract class GLSelectable implements AB2EventHandler {

    protected boolean isSelectable=false;
    protected boolean isHoverable=false;
    protected boolean isDraggable=false;

    protected boolean isSelected=false;
    protected boolean isHovered=false;
    protected boolean isDragging=false;

    protected List<Integer> selectedIds=new ArrayList<>();
    protected List<Integer> hoveredIds=new ArrayList<>();
    protected List<Integer> draggingIds=new ArrayList<>();

    // SELECT

    public boolean isSelectable() { return isSelectable; }

    public void setSelectable(boolean isSelectable) { this.isSelectable=isSelectable; }

    public void setSelect() { isSelected=true; }

    public void setSelect(int pickId) { selectedIds.add(pickId); }

    public boolean isSelected() { return isSelected; }

    public void releaseSelect() { isSelected=false; }

    public void releaseSelect(int pickId) { selectedIds.remove(new Integer(pickId)); }

    public void releaseAllSelect() { isSelected=false; selectedIds.clear(); }

    // HOVER

    public boolean isHoverable() { return isHoverable; }

    public void setHoverable(boolean isHoverable) { this.isHoverable=isHoverable; }

    public void setHover() { isHovered=true; }

    public void setHover(int pickId) { hoveredIds.add(pickId); }

    public boolean isHovered() { return isHovered; }

    public void releaseHover() { isHovered=false; }

    public void releaseHover(int pickId) { hoveredIds.remove(new Integer(pickId)); }

    public void releaseAllHover() { isHovered=false; hoveredIds.clear(); }

    // DRAG

    public boolean isDraggable() { return isDraggable; }

    public void setDraggable(boolean isDraggable) { this.isDraggable=isDraggable; }

    public void setDrag() { isDragging=true; }

    public void setDrag(int pickId) { draggingIds.add(pickId); }

    public boolean isDragging() { return isDragging; }

    public void releaseDrag() { isDragging=false; }

    public void releaseDrag(int pickId) { draggingIds.remove(new Integer(pickId)); }

    public void releaseAllDrag() { isDragging=false; draggingIds.clear(); }

    public abstract boolean acceptsDropType(GLSelectable selectable);

}
