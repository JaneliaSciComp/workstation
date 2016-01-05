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

import org.janelia.horta.actors.NeuriteActor;
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
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.SpheresActor;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.janelia.horta.nodes.BasicSwcVertex;
import org.openide.awt.StatusDisplayer;

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
    
    private NeuriteAnchor cachedHoverLocation = null; // TODO: - refactor as SELECTED vertex
    private TracingMode tracingMode = TracingMode.NAVIGATING;

    // private final NeuriteModel hoverModel = new NeuriteModel(); // TODO - deprecate in favor of below
    // For GUI feedback on existing model, contains zero or one vertex.
    private final NeuronModel highlightHoverModel = new BasicNeuronModel("Hover highlight");
    private NeuronVertex previousHoverVertex = null;
    
    private final NeuronModel neuronCursorModel = new BasicNeuronModel("Neuron tracing cursor"); // TODO: end point of auto tracing
    
    // For Tracing
    private final NeuronModel persistedParentModel = new BasicNeuronModel("Selected parent vertex"); // TODO: begin point of auto tracing
    
    private final NeuriteModel provisionalModel = new NeuriteModel();
    private final NeuriteModel previousHoverModel = new NeuriteModel();
    private final NeuriteModel persistedModel = new NeuriteModel();  
    
    RadiusEstimator radiusEstimator = 
            new TwoDimensionalRadiusEstimator();
            // new ConstantRadiusEstimator(5.0f);
    private StatusDisplayer.Message previousHoverMessage;
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // System.out.println("KeyTyped");
        // System.out.println(keyEvent.getKeyCode()+", "+KeyEvent.VK_ESCAPE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // System.out.println("KeyPressed");
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // System.out.println("KeyReleased");
        if(keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            // System.out.println("ESCAPE");
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
    
    public List<GL3Actor> createActors() {
        List<GL3Actor> result = new ArrayList<>();

        // Create actors in the order that they should be rendered;
        NeuriteActor nextActor = new NeuriteActor(null, persistedModel);
        result.add(nextActor);
        result.add(new NeuriteActor(null, provisionalModel));
        result.add(new NeuriteActor(null, previousHoverModel));
        // result.add(new NeuriteActor(null, hoverModel));
        SpheresActor highlightActor = new SpheresActor(highlightHoverModel);
        highlightActor.setMinPixelRadius(5.0f);
        result.add(highlightActor);
        
        // Colors 
        ((NeuriteActor)result.get(0)).setColor(Color.WHITE);
        ((NeuriteActor)result.get(1)).setColor(new Color(0.6f, 0.6f, 0.1f));
        ((NeuriteActor)result.get(2)).setColor(new Color(0.2f, 0.6f, 0.1f));
        ((SpheresActor)result.get(3)).setColor(new Color(1.0f, 1.0f, 0.1f));
        
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
        /*
        menu.add(new AbstractAction("Begin tracing at this location") {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setTracingModeOn(anchor);
            }
        }); 
                */
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
            // persistProvisionalAnchors();
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        // System.out.println("mouse exited");
        
        // keep showing hover highlight cursor, if dragging, even when mouse exits
        int buttonsDownMask = MouseEvent.BUTTON1_DOWN_MASK 
                | MouseEvent.BUTTON2_DOWN_MASK 
                | MouseEvent.BUTTON3_DOWN_MASK;
        if ( (event.getModifiersEx() & buttonsDownMask) != 0 )
            return;
        
        // Stop displaying hover highlight when cursor exits the viewport
        if (clearHighlightHoverVertex()) {
            highlightHoverModel.getMembersRemovedObservable().notifyObservers(); // repaint
        }

    }

    // GUI feedback for hovering existing vertex under cursor
    // returns true if a previously unhighlighted vertex is highlighted
    private boolean highlightHoverVertex(NeuronVertex vertex) 
    {
        if (vertex == null) return false;
        NeuronVertexIndex vix = volumeProjection.getVertexIndex();
        NeuronModel neuron = vix.neuronForVertex(vertex);
        float loc[] = vertex.getLocation();
        
        boolean doShowStatusMessage = true;
        if (doShowStatusMessage) {
            String message = "";
            if (neuron != null) {
                message += neuron.getName() + ": ";
            }
            message += " XYZ = [" + loc[0] + ", " + loc[1] + ", " + loc[2] + "]";
            message += "; Vertex Object ID = " + System.identityHashCode(vertex);
            if (message.length() > 0)
                previousHoverMessage = StatusDisplayer.getDefault().setStatusText(message, 2);            
        }
        
        boolean doShowVertexActor = true;
        if (doShowVertexActor) {
            // Remove any previous vertex
            highlightHoverModel.getVertexes().clear();
            highlightHoverModel.getEdges().clear();

            // Create a modified vertex to represent the enlarged, highlighted actor
            BasicSwcVertex highlightVertex = new BasicSwcVertex(loc[0], loc[1], loc[2]); // same center location as real vertex
            // Set highlight actor radius X% larger than true vertex radius, and at least 2 pixels larger
            float startRadius = 1.0f;
            if (vertex.hasRadius())
                startRadius = vertex.getRadius();
            float highlightRadius = startRadius * 1.50f;
            // TODO: and at least 2 pixels bigger - need camera info?
            highlightVertex.setRadius(highlightRadius);
            // blend neuron color with pale yellow highlight color
            float highlightColor[] = {1.0f, 1.0f, 0.8f, 0.5f}; // pale yellow and transparent
            float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
            if (neuron != null) {
                neuronColor = neuron.getColor().getColorComponents(neuronColor);
            }
            float highlightBlend = 0.75f;
            Color blendedColor = new Color(
                    neuronColor[0] - highlightBlend * (neuronColor[0] - highlightColor[0]),
                    neuronColor[1] - highlightBlend * (neuronColor[1] - highlightColor[1]),
                    neuronColor[2] - highlightBlend * (neuronColor[2] - highlightColor[2]),
                    highlightColor[3] // always use the same alpha transparency value
                    );
            highlightHoverModel.setVisible(true);
            highlightHoverModel.setColor(blendedColor);
            // highlightHoverModel.setColor(Color.MAGENTA); // for debugging
            
            highlightHoverModel.getVertexes().add(highlightVertex);
            highlightHoverModel.getMembersAddedObservable().setChanged();
            
            highlightHoverModel.getMembersAddedObservable().notifyObservers();     
            highlightHoverModel.getColorChangeObservable().notifyObservers();
        }
        
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearHighlightHoverVertex() 
    {
        if (highlightHoverModel.getVertexes().isEmpty()) {
            return false;
        }
        highlightHoverModel.getVertexes().clear();
        highlightHoverModel.getEdges().clear();
        highlightHoverModel.getMembersRemovedObservable().setChanged();
        cachedHoverLocation = null;
        previousHoverPoint = null;
        return true;
    }
    
    @Override
    public void mouseMoved(MouseEvent event) 
    {
        // TODO: update old provisional tracing behavior
        moveHoverCursor(event.getPoint());
    }

    private NeuriteAnchor anchorForScreenPoint(Point screenPoint) {
        double intensity = volumeProjection.getIntensity(screenPoint);
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
        if (screenPoint == previousHoverPoint)
            return; // no change from last time
        previousHoverPoint = screenPoint;
        
        // Find nearby brightest point
        // screenPoint = optimizePosition(screenPoint); // TODO: disabling optimization for now
        
        // Highlight annotation vertex
        Point hoverPoint = screenPoint;
        if (volumeProjection.isNeuronModelAt(hoverPoint)) { // found an existing annotation model under the cursor
            Vector3 xyz = volumeProjection.worldXyzForScreenXy(hoverPoint);
            NeuronVertexIndex vix = volumeProjection.getVertexIndex();
            NeuronVertex nearestVertex = vix.getNearest(xyz);
            if (nearestVertex == null) { // TODO - this should not happen?
                if (clearHighlightHoverVertex()) {
                    highlightHoverModel.getMembersRemovedObservable().notifyObservers(); // repaint
                }
            }
            else {
                highlightHoverVertex(nearestVertex);
            }
            // StatusDisplayer.getDefault().setStatusText("Neuron!", 2);
        }
        else {
            // Stop displaying highlight when cursor moves off neuron
            if (clearHighlightHoverVertex()) {
                highlightHoverModel.getMembersRemovedObservable().notifyObservers(); // repaint
            }
            // Clear previous vertex message, if necessary
            if (previousHoverMessage != null) {
                previousHoverMessage.clear(2);
                previousHoverMessage = null;
            }
        }
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
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point optimizeY(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, 0, -1, max);
        Point p2 = searchOptimizeBrightness(p, 0, 1, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point searchOptimizeBrightness(Point point, int dx, int dy, int max_step) {
        double i_orig = volumeProjection.getIntensity(point);
        double best_i = i_orig;
        int best_t = 0;
        double max_drop = 10 + 0.05 * i_orig;
        for (int t = 1; t <= max_step; ++t) {
            double i_test = volumeProjection.getIntensity(new Point(
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

}
