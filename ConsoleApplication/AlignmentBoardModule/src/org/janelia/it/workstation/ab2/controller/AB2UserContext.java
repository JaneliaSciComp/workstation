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

    private GLSelectable dragObject;
    private GLSelectable hoverObject;
    private GLSelectable selectObject;

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

    public GLSelectable getDragObject() {
        return dragObject;
    }

    public void setDragObject(GLSelectable dragObject) {
        this.dragObject = dragObject;
    }

    public GLSelectable getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(GLSelectable hoverObject) {
        this.hoverObject = hoverObject;
    }

    public GLSelectable getSelectObject() {
        return selectObject;
    }

    public void setSelectObject(GLSelectable selectObject) {
        this.selectObject = selectObject;
    }

    public void clearDrag() {
        positionHistory.clear();
        mouseIsDragging=false;
        dragObject=null;
    }

    public void clearAll() {
        positionHistory.clear();
        mouseIsDragging=false;
        contextMap=new HashMap<>();
        dragObject=null;
        hoverObject=null;
        selectObject=null;
    }

}
