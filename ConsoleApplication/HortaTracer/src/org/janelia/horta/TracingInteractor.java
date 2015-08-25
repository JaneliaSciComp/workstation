/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.horta;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import org.janelia.geometry3d.Vector3;

/**
 * Adapted from C:\Users\brunsc\Documents\Fiji_Plugins\Auto_Trace\Semi_Trace.java
 * @author Christopher Bruns
 */
public class TracingInteractor extends MouseAdapter
        implements MouseListener, MouseMotionListener, KeyListener
{

    private final VolumeProjection volumeProjection;
    // private NeuriteAnchor sourceAnchor; // origin of series of provisional anchors
    // private final Stack<NeuriteAnchor> provisionalModel = new Stack<>();
    // private final List<NeuriteAnchor> persistedModel = new ArrayList<>();
    private final int max_tol = 5; // pixels
    
    private NeuriteAnchor cachedHoverLocation = null;
    private TracingMode tracingMode = TracingMode.NAVIGATING;

    private final NeuriteModel hoverModel = new NeuriteModel();
    private final NeuriteModel provisionalModel = new NeuriteModel();
    private final NeuriteModel previousHoverModel = new NeuriteModel();
    private final NeuriteModel persistedModel = new NeuriteModel();  
    
    RadiusEstimator radiusEstimator = 
            new TwoDimensionalRadiusEstimator();
            // new ConstantRadiusEstimator(5.0f);
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
        System.out.println("KeyTyped");
        System.out.println(keyEvent.getKeyCode()+", "+KeyEvent.VK_ESCAPE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("KeyPressed");
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        System.out.println("KeyReleased");
        if(keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            System.out.println("ESCAPE");
            setTracingModeOff();
        }
    }

    public boolean setTracingModeOn() {
        if (cachedHoverLocation == null)
            return false; // cannot start tracing without an anchor
        return setTracingModeOn(cachedHoverLocation);
    }
    
    public boolean setTracingModeOn(NeuriteAnchor sourceAnchor) {
        if (TracingMode.TRACING == this.tracingMode)
            return false; // no change
        this.tracingMode = TracingMode.TRACING;
        previousHoverModel.clear();
        previousHoverModel.add(sourceAnchor);
        previousHoverModel.notifyObservers();
        return true;
    }
    
    public void setTracingModeOff() {
        if (TracingMode.TRACING != this.tracingMode)
            return; // no change
        this.tracingMode = TracingMode.NAVIGATING;
        provisionalModel.clear();
        provisionalModel.setChanged();
        previousHoverModel.clear();
        previousHoverModel.setChanged();
        provisionalModel.notifyObservers();
        previousHoverModel.notifyObservers();
    }
    
    public TracingMode getTracingMode() {return tracingMode;}
    
    static public enum TracingMode {TRACING, NAVIGATING};

    public TracingInteractor(
            VolumeProjection volumeProjection) 
    {
        this.volumeProjection = volumeProjection;
        connectMouseToComponent();
    }

    public NeuriteAnchor getHoverLocation() {
        return cachedHoverLocation;
    }
    
    private void connectMouseToComponent() {
        volumeProjection.getMouseableComponent().addMouseListener(this);
        volumeProjection.getMouseableComponent().addMouseMotionListener(this);
        volumeProjection.getMouseableComponent().addKeyListener(this);
    }
    
    public List<NeuriteActor> createActors() {
        List<NeuriteActor> result = new ArrayList<>();

        // Create actors in the order that they should be rendered;
        NeuriteActor nextActor = new NeuriteActor(null, persistedModel);
        result.add(nextActor);
        result.add(new NeuriteActor(null, provisionalModel));
        result.add(new NeuriteActor(null, previousHoverModel));
        result.add(new NeuriteActor(null, hoverModel));
        
        // Colors 
        result.get(0).setColor(Color.WHITE);
        result.get(1).setColor(new Color(0.6f, 0.6f, 0.1f));
        result.get(2).setColor(new Color(0.2f, 0.6f, 0.1f));
        result.get(3).setColor(new Color(1.0f, 1.0f, 0.1f));
        
        return result;
    }
    
    /**
     * 
     * @param menu - existing menu to add tracing options to
     */
    public void exportMenuItems(JPopupMenu menu, NeuriteAnchor anchor) {
        if (tracingMode == TracingMode.TRACING)
            exportEnabledMenuItems(menu, anchor);
        else
            exportDisabledMenuItems(menu, anchor);
    }

    private void exportEnabledMenuItems(JPopupMenu menu, NeuriteAnchor anchor) 
    {
        menu.add(new AbstractAction("Exit tracing mode [ESC]") {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setTracingModeOff();
            }
        });
    }

    private void exportDisabledMenuItems(JPopupMenu menu, final NeuriteAnchor anchor) {
        menu.add(new AbstractAction("Begin tracing at this location") {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setTracingModeOn(anchor);
            }
        }); 
    }    

    private boolean extendProvisionalAnchors(NeuriteAnchor anchor) {
        NeuriteAnchor sourceAnchor = previousHoverModel.getLast();
        if (sourceAnchor == null) {
            return false; // nothing to extend
        }
        boolean bChanged = false; // not whether anything changes...
        
        // Build up series of provisional points
        float sourceD2 = sourceAnchor.distanceSquared(anchor.getLocationUm());

        // remove anchors farther from source than cursor
        NeuriteAnchor w = null;
        if (provisionalModel.size() > 0) {
            w = provisionalModel.getLast();
        }

        while ((w != null) && (sourceAnchor.distanceSquared(w.getLocationUm()) > sourceD2)) {
            // remove anchors farther tnan cursor from sourceAnchor
            bChanged = true;
            provisionalModel.pollLast();
            if (provisionalModel.size() > 0) {
                w = provisionalModel.getLast();
            } else {
                w = null;
            }
        }

        if (w == null) {
            w = sourceAnchor;
        }

        // maybe append a new provisional anchor
        double min_d = 3 + anchor.getRadiusUm() + w.getRadiusUm();
        double min_d2 = min_d * min_d;
        float localD2 = w.distanceSquared(anchor.getLocationUm());
        // IJ.log("d2b = "+sourceD2);
        if (localD2 > min_d2) {
            bChanged = true;
            provisionalModel.add(anchor);
        }

        return bChanged;
    }

    // Click to commit current set of anchors
    @Override
    public void mouseClicked(MouseEvent event) {
        // System.out.println("Mouse clicked in tracer");
        
        if ( (event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1) )
        {
            // Shift-clicking causes entry into tracing mode
            if (event.isShiftDown()) {
                // setTracingModeOn();
            }

            // Regular click has no tracing effect in navigating mode
            if (tracingMode == TracingMode.NAVIGATING)
                return;

            // In tracing mode, clicking causes provisional anchors to be persisted.
            persistProvisionalAnchors();
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        // System.out.println("mouse exited");
        if (hoverModel.isEmpty()) {
            return;
        }
        int buttonsDownMask = MouseEvent.BUTTON1_DOWN_MASK 
                | MouseEvent.BUTTON2_DOWN_MASK 
                | MouseEvent.BUTTON3_DOWN_MASK;
        if ( (event.getModifiersEx() & buttonsDownMask) != 0 )
            return; // keep showing cursor, if dragging, even when mouse exits
        hoverModel.clear();
        cachedHoverLocation = null;
        hoverModel.notifyObservers();
        previousHoverPoint = null;
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        moveHoverCursor(event.getPoint());
        if (hoverModel.isEmpty())
            return;
        if (getTracingMode() != TracingMode.TRACING)
            return;
        if (extendProvisionalAnchors(hoverModel.getLast()))
            provisionalModel.notifyObservers();
    }

    private NeuriteAnchor anchorForScreenPoint(Point screenPoint) {
        int intensity = volumeProjection.getIntensity(screenPoint);
        if (intensity <= 0) return null;
        float radius = radiusEstimator.estimateRadius(screenPoint, volumeProjection);
        if (radius <= 0) return null;
        Vector3 xyz = volumeProjection.worldXyzForScreenXy(screenPoint);
        NeuriteAnchor anchor = new NeuriteAnchor(
                xyz,
                intensity,
                radius);
        return anchor;
    }
    
    // Show provisional Anchor radius and position for current mouse location
    private Point previousHoverPoint = null;
    public void moveHoverCursor(Point screenPoint) {
        // Find nearby brightest point
        screenPoint = optimizePosition(screenPoint);
        
        if (screenPoint == previousHoverPoint)
            return; // no change from last time
        previousHoverPoint = screenPoint;
        
        hoverModel.clear();
        cachedHoverLocation = null;

        NeuriteAnchor anchor = anchorForScreenPoint(screenPoint);
        if (anchor != null) {            
            hoverModel.add(anchor);
            cachedHoverLocation = anchor;
        }
        
        hoverModel.notifyObservers();
    }

    private Point optimizePosition(Point screenPoint) {
        // TODO - this method is pretty crude; but maybe good enough?
        screenPoint = optimizeX(screenPoint, max_tol);
        screenPoint = optimizeY(screenPoint, max_tol);
        // Refine to local maximum
        Point prevPoint = new Point(screenPoint.x, screenPoint.y);
        int stepCount = 0;
        do {
            stepCount += 1;
            if (stepCount > 5) 
                break;
            screenPoint = optimizeX(screenPoint, 2);
            screenPoint = optimizeY(screenPoint, 2);
        } while (! prevPoint.equals(screenPoint));
        return screenPoint;
    }
    
    // Find a nearby brighter pixel
    private Point optimizeX(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, -1, 0, max);
        Point p2 = searchOptimizeBrightness(p, 1, 0, max);
        int intensity1 = volumeProjection.getIntensity(p1);
        int intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point optimizeY(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, 0, -1, max);
        Point p2 = searchOptimizeBrightness(p, 0, 1, max);
        int intensity1 = volumeProjection.getIntensity(p1);
        int intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point searchOptimizeBrightness(Point point, int dx, int dy, int max_step) {
        int i_orig = volumeProjection.getIntensity(point);
        int best_i = i_orig;
        int best_t = 0;
        double max_drop = 10 + 0.05 * i_orig;
        for (int t = 1; t <= max_step; ++t) {
            int i_test = volumeProjection.getIntensity(new Point(
                    point.x + t * dx, 
                    point.y + t * dy));
            if (i_test > best_i) {
                best_i = i_test;
                best_t = t;
            } else if (i_test < (best_i - max_drop)) {
                break; // Don't cross valleys
            }
        }
        if (best_t == 0) {
            return point;
        } else {
            return new Point(point.x + best_t * dx, point.y + best_t * dy);
        }
    }
    
    protected boolean persistProvisionalAnchors() {
        if (provisionalModel.isEmpty() && (hoverModel.isEmpty()))
            return false;
        persistedModel.addAll(provisionalModel);
        provisionalModel.clear();
        NeuriteAnchor latest = hoverModel.getLast();
        if (latest != null) {
            persistedModel.add(latest);
            // Set up new source anchor
            previousHoverModel.clear();
            previousHoverModel.add(latest);
            hoverModel.clear();
            previousHoverPoint = null;
        }
        provisionalModel.notifyObservers();
        persistedModel.notifyObservers();
        previousHoverModel.notifyObservers();
        return true;
    }
}
