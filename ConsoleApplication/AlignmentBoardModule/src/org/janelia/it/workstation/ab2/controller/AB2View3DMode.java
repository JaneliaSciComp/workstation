package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2Basic3DRenderer;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseReleasedEvent;
import org.janelia.it.workstation.ab2.renderer.AB2SimpleCubeRenderer;

public class AB2View3DMode extends AB2ControllerMode {

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    protected AB23DRenderer renderer;
    protected Point previousMousePos;
    protected boolean bMouseIsDragging = false;


    public AB2View3DMode(AB2Controller controller) {
        super(controller);
        renderer=new AB2SimpleCubeRenderer();
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
    public void init(GLAutoDrawable glAutoDrawable) {
        renderer.init(glAutoDrawable);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        renderer.dispose(glAutoDrawable);
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        renderer.display(glAutoDrawable);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
        renderer.reshape(glAutoDrawable, i, i1, i2, i3);
    }

    @Override
    public void processEvent(AB2Event event) {
        if (event instanceof AB2MouseReleasedEvent) {
            if (bMouseIsDragging) {
                bMouseIsDragging=false;
            }
        } else if (event instanceof AB2MouseDraggedEvent) {
            MouseEvent mouseEvent=((AB2MouseDraggedEvent) event).getMouseEvent();
            Point p1 = mouseEvent.getPoint();
            if (! bMouseIsDragging) {
                bMouseIsDragging = true;
                previousMousePos = p1;
                return;
            }

            Point p0 = previousMousePos;
            Point dPos = new Point(p1.x-p0.x, p1.y-p0.y);

            InteractionMode mode = InteractionMode.ROTATE; // default drag controller is ROTATE
            if (mouseEvent.isMetaDown()) // command-drag to zoom
                mode = InteractionMode.ZOOM;
            if (SwingUtilities.isMiddleMouseButton(mouseEvent)) // middle drag to translate
                mode = InteractionMode.TRANSLATE;
            if (mouseEvent.isShiftDown()) // shift-drag to translate
                mode = InteractionMode.TRANSLATE;

            if (mode == InteractionMode.TRANSLATE) {
                renderer.translatePixels(dPos.x, dPos.y, 0);
                controller.repaint();
            }
            else if (mode == InteractionMode.ROTATE) {
                renderer.rotatePixels(dPos.x, dPos.y, 0);
                controller.repaint();
            }
            else if (mode == InteractionMode.ZOOM) {
                renderer.zoomPixels(p1, p0);
                controller.repaint();
            }

            previousMousePos = p1;
        }
    }
}
