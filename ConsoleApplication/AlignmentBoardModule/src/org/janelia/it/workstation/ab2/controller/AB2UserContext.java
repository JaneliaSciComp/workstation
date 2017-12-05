package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public class AB2UserContext {
    private Point previousMousePos;
    private boolean mouseIsDragging = false;
    private AB2Renderer currentDragRenderer;
    private Map<String,Object> contextMap=new HashMap<>();

    public AB2UserContext() {}

    public AB2UserContext(Point previousMousePos, boolean mouseIsDragging, Map<String, Object> contextMap) {
        this.previousMousePos=previousMousePos;
        this.mouseIsDragging=mouseIsDragging;
        if (contextMap!=null) {
            this.contextMap=contextMap;
        }
    }

    public Point getPreviousMousePos() { return previousMousePos; }

    public void setPreviousMousePos(Point point) { previousMousePos=point; }

    public boolean isMouseIsDragging() { return mouseIsDragging; }

    public void setMouseIsDragging(boolean dragging) { mouseIsDragging=dragging; }

    public Map<String, Object> getContextMap() { return contextMap; }

    public void setContextMap(Map<String, Object> contextMap) { this.contextMap=contextMap; }

    public void setCurrentDragRenderer(AB2Renderer renderer) { this.currentDragRenderer=renderer; }

    public AB2Renderer getCurrentDragRenderer() {
        return currentDragRenderer;
    }

    public void clear() {
        previousMousePos=null;
        mouseIsDragging=false;
        currentDragRenderer=null;
        contextMap=new HashMap<>();
    }

}
