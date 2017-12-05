package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public class AB2UserContext {
    private List<Point> positionHistory=new ArrayList<>();
    private boolean mouseIsDragging = false;
    private AB2Renderer currentDragRenderer;
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

    public void setCurrentDragRenderer(AB2Renderer renderer) { this.currentDragRenderer=renderer; }

    public AB2Renderer getCurrentDragRenderer() {
        return currentDragRenderer;
    }

    public void clear() {
        positionHistory.clear();
        mouseIsDragging=false;
        currentDragRenderer=null;
        contextMap=new HashMap<>();
    }

}
