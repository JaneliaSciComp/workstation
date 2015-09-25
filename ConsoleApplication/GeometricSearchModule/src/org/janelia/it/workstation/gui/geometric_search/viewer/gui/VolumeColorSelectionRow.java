package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Created by murphys on 9/9/2015.
 */
public class VolumeColorSelectionRow extends ColorSelectionRow {

    Knob brightnessKnob;
    Knob transparencyKnob;

    SyncedCallback brightnessCallback;
    SyncedCallback transparencyCallback;

    public VolumeColorSelectionRow(String name, ScrollableColorRowPanel parentRowPanel) {
        super(name, parentRowPanel);

        brightnessKnob=new Knob("B", 0.0, 3.0, 1.45, true);
        brightnessKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (brightnessCallback!=null) {
                    brightnessCallback.performAction(new Float(brightnessKnob.getValue()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        transparencyKnob=new Knob("T", 0.0, 1.0, 0.1, true);
        transparencyKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (transparencyCallback!=null) {
                    transparencyCallback.performAction(new Float(transparencyKnob.getValue()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        add(brightnessKnob);
        add(transparencyKnob);

    }

    public void setBrightnessCallback(SyncedCallback callback) {
        this.brightnessCallback=callback;
        if (brightnessCallback!=null) brightnessCallback.performAction(new Float(brightnessKnob.getValue()));
    }

    public void setTransparencyCallback(SyncedCallback callback) {
        this.transparencyCallback=callback;
        if (transparencyCallback!=null) transparencyCallback.performAction(new Float(transparencyKnob.getValue()));
    }

}
