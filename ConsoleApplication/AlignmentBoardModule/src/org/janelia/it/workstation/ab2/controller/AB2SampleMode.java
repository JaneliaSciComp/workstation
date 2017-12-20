package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLRegion;

public class AB2SampleMode extends AB2ControllerMode {

    public AB2SampleMode(AB2Controller controller) {
        super(controller);
    }

    @Override
    public GLRegion getRegionAtPosition(Point point) {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void processEvent(AB2Event event) {

    }

    @Override
    public void init(GLAutoDrawable drawable) {

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void modeDisplay(GLAutoDrawable drawable) {

    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }

}
