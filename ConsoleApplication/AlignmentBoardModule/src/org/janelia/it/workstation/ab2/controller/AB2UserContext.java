package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public class AB2UserContext {

    // todo: add support for multi-object select

    private List<Point> positionHistory=new ArrayList<>();
    private boolean mouseIsDragging = false;

    private Object dragObject;
    private Object hoverObject;
    private Object selectObject;

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

    public Object getDragObject() {
        return dragObject;
    }

    public void setDragObject(Object dragObject) {
        this.dragObject = dragObject;
    }

    public Object getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(Object hoverObject) {
        this.hoverObject = hoverObject;
    }

    public Object getSelectObject() {
        return selectObject;
    }

    public void setSelectObject(Object selectObject) {
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
