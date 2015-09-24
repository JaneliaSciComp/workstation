package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Created by murphys on 9/9/2015.
 */
public class MeshColorSelectionRow extends ColorSelectionRow {

    Knob edgefalloffKnob;
    Knob intensityKnob;
    Knob ambientKnob;

    SyncedCallback edgefalloffCallback;
    SyncedCallback intensityCallback;
    SyncedCallback ambientCallback;

    public MeshColorSelectionRow(String name, ScrollableColorRowPanel parentRowPanel) {
        super(name, parentRowPanel);

        edgefalloffKnob=new Knob("E", 0.0, 10.0, 2.0, true);
        edgefalloffKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (edgefalloffCallback!=null) {
                    edgefalloffCallback.performAction(new Float(edgefalloffKnob.getValue()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        intensityKnob=new Knob("I", 0.0, 1.0, 0.5, true);
        intensityKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (intensityCallback!=null) {
                    intensityCallback.performAction(new Float(intensityKnob.getValue()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        ambientKnob=new Knob("A", 0.0, 1.0, 0.1, true);
        ambientKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (ambientCallback!=null) {
                    ambientCallback.performAction(new Float(ambientKnob.getValue()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        add(edgefalloffKnob);
        add(intensityKnob);
        add(ambientKnob);

    }

    public void setEdgefalloffCallback(SyncedCallback callback) {
        this.edgefalloffCallback=callback;
        if (edgefalloffCallback!=null) edgefalloffCallback.performAction(new Float(edgefalloffKnob.getValue()));
    }

    public void setIntensityCallback(SyncedCallback callback) {
        this.intensityCallback=callback;
        if (intensityCallback!=null) intensityCallback.performAction(new Float(intensityKnob.getValue()));
    }

    public void setAmbientCallback(SyncedCallback callback) {
        this.ambientCallback=callback;
        if (ambientCallback!=null) ambientCallback.performAction(new Float(ambientKnob.getValue()));
    }

}
