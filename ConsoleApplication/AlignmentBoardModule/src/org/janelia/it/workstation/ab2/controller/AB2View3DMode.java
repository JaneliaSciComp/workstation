package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.ab2.event.AB2MouseWheelEvent;
import org.janelia.it.workstation.ab2.renderer.AB23DRenderer;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseReleasedEvent;

public class AB2View3DMode extends AB2ControllerMode {

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    protected AB23DRenderer renderer;
    protected Point previousMousePos;
    protected boolean bMouseIsDragging = false;


    public AB2View3DMode(AB2Controller controller, AB23DRenderer renderer) {
        super(controller);
        this.renderer=renderer;
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
        final GL4 gl=glAutoDrawable.getGL().getGL4();
        renderer.init(gl);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        final GL4 gl=glAutoDrawable.getGL().getGL4();
        renderer.dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        final GL4 gl=glAutoDrawable.getGL().getGL4();
        renderer.display(gl);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
        final GL4 gl=glAutoDrawable.getGL().getGL4();
        renderer.reshape(gl, i, i1, i2, i3);
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
        } else if (event instanceof AB2MouseWheelEvent) {
            int notches = ((AB2MouseWheelEvent) event).getMouseWheelEvent().getWheelRotation();
            double zoomRatio = Math.pow(2.0, notches/50.0);
            renderer.zoom(zoomRatio);
            controller.repaint();
        }
    }
}
