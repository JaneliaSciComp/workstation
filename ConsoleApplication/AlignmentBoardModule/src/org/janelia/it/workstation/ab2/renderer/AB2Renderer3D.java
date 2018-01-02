package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.media.opengl.GL4;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.OrthographicCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;
import org.janelia.geometry3d.camera.ConstRotation;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.controller.AB2UserContext;
import org.janelia.it.workstation.ab2.controller.AB2View3DMode;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseWheelEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB2Renderer3D extends AB2RendererD implements AB2Renderer3DControls {
    Logger logger = LoggerFactory.getLogger(AB2Renderer3D.class);

    public static double MAX_DRAG_INCREMENT_PERC=0.10;

    Point[] dragPoints;

    protected PerspectiveCamera camera3d;
    Matrix4 vp3d;
    AB2Controller controller;

    public ConstRotation getRotation() {
        return vantage.getRotation();
    }

    public Matrix4 getRotationAsTransform() {
        return new Matrix4(vantage.getRotationInGround().asTransform());
    }

    public float getFocusDistance3d() {
        return camera3d.getCameraFocusDistance();
    }

    public ConstVector3 getFocusPosition3d() {
        return vantage.getFocusPosition();
    }

    public AB2Renderer3D() {
        controller=AB2Controller.getController();
        setBackgroundColorBuffer();
        resetView();
        dragPoints=new Point[2];
        dragPoints[0]=new Point();
        dragPoints[1]=new Point();
    }

    @Override
    protected void resetCamera() {
        camera3d = new PerspectiveCamera(vantage, viewport);
    }

    public Matrix4 getVp3d() { return new Matrix4(vp3d); }

    @Override
    protected void resetVPMatrix() {
        Matrix4 projectionMatrix3d=new Matrix4(camera3d.getProjectionMatrix());
        Matrix4 viewMatrix3d=new Matrix4(camera3d.getViewMatrix());
        vp3d=viewMatrix3d.multiply(projectionMatrix3d);
    }

    public double glUnitsPerPixel() {
        return Math.abs( camera3d.getCameraFocusDistance() ) / DISTANCE_TO_SCREEN_IN_PIXELS;
    }

    public void rotatePixels(double dx, double dy, double dz) {

        //logger.info("rotatePixels() raw : dx="+dx+" dy="+dy+" dz="+dz);

          double maxDragDistance=(MAX_DRAG_INCREMENT_PERC*viewport.getHeightPixels());
//
//        double dxCheck=dx;
//        if (dxCheck<0.0) dxCheck=dxCheck*-1.0;
//
//        double dyCheck=dy;
//        if (dyCheck<0.0) dyCheck=dyCheck*-1.0;
//
//        double dzCheck=dz;
//        if (dzCheck<0.0) dzCheck=dzCheck*-1.0;
//
//        if (dxCheck>maxDragDistance*2.0) {
//            dx=0.0;
//        }
//
//        if (dyCheck>maxDragDistance*2.0) {
//            dy=0.0;
//        }
//
//        if (dzCheck>maxDragDistance*2.0) {
//            dz=0.0;
//        }

        // Rotate like a trackball
        double dragDistance = Math.sqrt(dy * dy + dx * dx + dz * dz);
        if (dragDistance <= 0.0)
            return;


        if (dragDistance>maxDragDistance) {
            return; // discard
        }

        Vector3 rotationAxis=new Vector3( (float)dy, (float)dx, (float)dz);
        rotationAxis=rotationAxis.normalize();

        double wD=viewport.getWidthPixels() * 1.0;
        double hD=viewport.getHeightPixels() * 1.0;

        double windowSize = Math.sqrt(wD*wD + hD*hD);

        // Drag across the entire window to rotate all the way around
        double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;

        Rotation rotation = new Rotation().setFromAxisAngle(rotationAxis, (float)rotationAngle);

        vantage.getRotationInGround().multiply(rotation.transpose());
    }

    public void translatePixels(double dx, double dy, double dz) {
        // trackball translate
        Vector3 translation=new Vector3((float)-dx, (float)dy, (float)-dz);
        translation=translation.multiplyScalar((float)glUnitsPerPixel());
        Rotation copyOfRotation = new Rotation(vantage.getRotationInGround());
        translation=copyOfRotation.multiply(translation);
        vantage.getFocusPosition().add(translation);
    }

    public void zoom(double zoomRatio) {
        if (zoomRatio <= 0.0) {
            return;
        }
        if (zoomRatio == 1.0) {
            return;
        }

        double cameraFocusDistance = (double)camera3d.getCameraFocusDistance();
        cameraFocusDistance /= zoomRatio;
        if ( cameraFocusDistance > MAX_CAMERA_FOCUS_DISTANCE ) {
            return;
        }
        if ( cameraFocusDistance < MIN_CAMERA_FOCUS_DISTANCE ) {
            return;
        }

        double sceneUnitsPerViewportHeight=cameraFocusDistance*Math.tan(0.5*camera3d.getFovRadians())*2.0;

        vantage.setSceneUnitsPerViewportHeight((float)sceneUnitsPerViewportHeight);
    }

    public void zoomPixels(Point newPoint, Point oldPoint) {
        // Are we dragging away from the center, or toward the center?
        double wD=viewport.getWidthPixels()*1.0;
        double hD=viewport.getHeightPixels()*1.0;
        double[] p0 = {oldPoint.x - wD/2.0,
                oldPoint.y - hD/2.0};
        double[] p1 = {newPoint.x - wD/2.0,
                newPoint.y - hD/2.0};
        double dC0 = Math.sqrt(p0[0] * p0[0] + p0[1] * p0[1]);
        double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
        double dC = dC1 - dC0; // positive means away
        double denom = Math.max(20.0, dC1);
        double zoomRatio = 1.0 + dC/denom;

        //double cameraFocusDistance=(double)camera.getCameraFocusDistance();

        //logger.info("cfd="+cameraFocusDistance+" setting zoom to="+zoomRatio);

        zoom(zoomRatio);
    }

    // Here we will assume 5 points to start with. We will remove any outlier
    // and take the average of the remaining pairs for start and end.
    private void getFilteredStartEnd(List<Point> list, int length) {
        double maxLength=0.0;
        int maxIndex=-1;
        int listSize=list.size();
        for (int i=0;i<length;i++) {
            Point p=list.get(listSize-i-1);
            double l=Math.sqrt(p.x*p.x+p.y*p.y);
            if (l>maxLength) {
                maxLength=l;
                maxIndex=i;
            }
        }
        double startX=0.0;
        double endX=0.0;
        double startY=0.0;
        double endY=0.0;
        int count=0;
        int halfWay=(length-1)/2;
        for (int i=0;i<length;i++) {
            if (i==maxIndex) {
                continue;
            }
            Point p=list.get(listSize-i-1);
            if (count<halfWay) {
                endX+=p.x;
                endY+=p.y;
            } else {
                startX+=p.x;
                startY+=p.y;
            }
            count++;
        }
        startX=startX/((double)halfWay);
        startY=startY/((double)halfWay);
        endX=endX/((double)halfWay);
        endY=endY/((double)halfWay);
        dragPoints[0].x=(int)startX;
        dragPoints[0].y=(int)startY;
        dragPoints[1].x=(int)endX;
        dragPoints[1].y=(int)endY;
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);
        AB2UserContext userContext=controller.getUserContext();
        if (event instanceof AB2MouseDraggedEvent) {
            MouseEvent mouseEvent=((AB2MouseDraggedEvent) event).getMouseEvent();
            List<Point> points=userContext.getPositionHistory();
            if (points.size()>4) {
                getFilteredStartEnd(points, 5);
                Point p0=dragPoints[0];
                Point p1=dragPoints[1];

                //logger.info("p0 x="+p0.x+" y="+p0.y+" p1 x="+p1.x+" y="+p1.y+" list="+ points.size());

                int xDiff=p1.x-p0.x;
                int yDiff=p1.y-p0.y;

                if (xDiff!=0 || yDiff!=0) {

                    Point dPos = new Point(xDiff, yDiff);

                    AB2View3DMode.InteractionMode mode = AB2View3DMode.InteractionMode.ROTATE; // default drag controller is ROTATE
                    if (mouseEvent.isMetaDown()) // command-drag to zoom
                        mode = AB2View3DMode.InteractionMode.ZOOM;
                    if (SwingUtilities.isMiddleMouseButton(mouseEvent)) // middle drag to translate
                        mode = AB2View3DMode.InteractionMode.TRANSLATE;
                    if (mouseEvent.isShiftDown()) // shift-drag to translate
                        mode = AB2View3DMode.InteractionMode.TRANSLATE;

                    if (mode == AB2View3DMode.InteractionMode.TRANSLATE) {
                        translatePixels(dPos.x, dPos.y, 0);
                    }
                    else if (mode == AB2View3DMode.InteractionMode.ROTATE) {
                        rotatePixels(dPos.x, dPos.y, 0);
                    }
                    else if (mode == AB2View3DMode.InteractionMode.ZOOM) {
                        zoomPixels(p1, p0);
                    }
                }
            }
        } else if (event instanceof AB2MouseWheelEvent) {
            int notches = ((AB2MouseWheelEvent) event).getMouseWheelEvent().getWheelRotation();
            double zoomRatio = Math.pow(2.0, notches/50.0);
            zoom(zoomRatio);
        }
    }

}
