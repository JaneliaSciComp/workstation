package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.ab2.gl.GLSelectable;

public class AB2UserContext {

    // todo: add support for multi-object select

    private List<Point> positionHistory=new ArrayList<>();
    private boolean mouseIsDragging = false;

    private List<GLSelectable> dragObjects=new ArrayList<>();
    private GLSelectable hoverObject;
    private List<GLSelectable> selectObjects=new ArrayList<>();

    private Map<String,Object> contextMap=new HashMap<>();

    public AB2UserContext() {}

    public AB2UserContext(Point position, boolean mouseIsDragging, Map<String, Object> contextMap) {
        if (positionHistory!=null) {
            this.positionHistory.add(position);
        }
        this.mouseIsDragging=mouseIsDragging;
        if (contextMap!=null) {
            this.contextMap=contextMap;
        }
    }

    public List<Point> getPositionHistory() { return positionHistory; }

    public void setPositionHistory(List<Point> points) { positionHistory=points; }

    public boolean isMouseIsDragging() { return mouseIsDragging; }

    public void setMouseIsDragging(boolean dragging) { mouseIsDragging=dragging; }

    public Map<String, Object> getContextMap() { return contextMap; }

    public void setContextMap(Map<String, Object> contextMap) { this.contextMap=contextMap; }

    public List<GLSelectable> getDragObjects() {
        return dragObjects;
    }

    public void addDragObject(GLSelectable dragObject) {
        dragObjects.add(dragObject);
    }

    public void addDragObjects(List<GLSelectable> dragObjects) { this.dragObjects.addAll(dragObjects); }

    public void clearDragObjects() { dragObjects.clear(); }

    public void removeDragObject(Object o) { dragObjects.remove(o); }

    public GLSelectable getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(GLSelectable hoverObject) {
        this.hoverObject = hoverObject;
    }

    public List<GLSelectable> getSelectObjects() {
        return selectObjects;
    }

    public void addSelectObject(GLSelectable selectObject) {
        selectObjects.add(selectObject);
    }

    public void addSelectObjects(List<GLSelectable> selectObjects) { selectObjects.addAll(selectObjects); }

    public void clearSelectObjects() { selectObjects.clear(); }

    public void removeSelectObject(Object o) { selectObjects.remove(o); }

    public void clearDrag() {
        positionHistory.clear();
        mouseIsDragging=false;
        dragObjects.clear();
    }

    public void clearAll() {
        positionHistory.clear();
        mouseIsDragging=false;
        contextMap=new HashMap<>();
        dragObjects.clear();
        hoverObject=null;
        selectObjects.clear();
    }

}
