package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.workstation.ab2.event.AB2EventHandler;

/*

This base class for both GLAbstractActor and GLRegion attempts to flexibly support a very wide
range of manipulation schemes and states for both simple and complex actors and regions.

While a region is understood to be selectable/hoverable/draggable as a singleton, actors
may have a very large number of distinctly selectable/hoverable/draggable elements, which are
distinguished by their different pickIds.

If a GLSelectable is a GLRegion, it has no actorId, nor any pickIds. If simply has the property
of being either selectable/hoverable/draggable.

On the other hand, if a GLSelectable is a GLAbstractActor, it is guaranteed to have at least an
actorId, and possibly many other alternate pickIds.

The method isSelectable() for a GLAbstractActor is equivalent to isSelectable(actorId), likewise
for isHoverable() and isDraggable().

Also, the method setSelectable(), for a GLAbstractActor, is equivalent to setSelectable(actorId), and
again the same for setHoverable() and setDraggable().

*/

public abstract class GLSelectable implements AB2EventHandler {

    protected boolean isSelectable=false;
    protected boolean isHoverable=false;
    protected boolean isDraggable=false;

    protected boolean isSelected=false;
    protected boolean isHovered=false;
    protected boolean isDragging=false;

    protected List<Integer> selectableIds=new ArrayList<>();
    protected List<Integer> selectedIds=new ArrayList<>();

    protected List<Integer> hoverableIds=new ArrayList<>();
    protected Integer hoverId=0;

    protected List<Integer> draggableIds=new ArrayList<>();
    protected List<Integer> draggingIds=new ArrayList<>();

    // SELECT

    public boolean isSelectable() { return isSelectable; }

    public boolean isSelectable(int pickId) { return isSelectable; }

    public void setSelectable(boolean isSelectable) { this.isSelectable=isSelectable; }

    public void setSelect() { isSelected=true; }

    public void setSelect(int pickId) { selectedIds.add(pickId); }

    public boolean isSelected() { return isSelected; }

    public void releaseSelect() { isSelected=false; }

    public void releaseSelect(int pickId) { selectedIds.remove(new Integer(pickId)); }

    public void releaseAllSelect() { isSelected=false; selectedIds.clear(); }

    public List<Integer> getSelectedIds() { return selectedIds; }

    // HOVER

    public boolean isHoverable() { return isHoverable; }

    public void setHoverable(boolean isHoverable) { this.isHoverable=isHoverable; }

    public void setHover() { isHovered=true; }

    public void setHover(int pickId) { hoverId=pickId; }

    public boolean isHovered() { return isHovered; }

    public void releaseHover() { isHovered=false; }

    public void releaseHoverId() { hoverId=0; }

    public void releaseAllHover() { isHovered=false; hoverId=0; }

    public Integer getHoverId() { return hoverId; }

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

    public List<Integer> getDraggingIds() { return draggingIds; }

}
