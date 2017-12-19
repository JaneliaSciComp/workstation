package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.workstation.ab2.event.AB2MouseBeginDragEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseWheelEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseReleasedEvent;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2View3DMode extends AB2ControllerMode {

    Logger logger= LoggerFactory.getLogger(AB2View3DMode.class);

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    public AB2View3DMode(AB2Controller controller) {
        super(controller);
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
        super.processEvent(event);
    }

}
