package org.janelia.it.workstation.gui.viewer3d.vendor_demo;

import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/20/13
 * Time: 5:01 PM
 *
 * This class shows the concept of a 3D viewer.
 */
public class Renderer3D extends GLJPanel // in case lightweight widget is required
        implements MouseListener, MouseMotionListener,
        MouseWheelListener {

    private boolean bMouseIsDragging;
    private Point previousMousePos;

    private int widthInPixels = 622;
    private int heightInPixels = 600;

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Test MipWidget for Masking");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Illustrate 3D Concept");

                frame.getContentPane().add(label);
                frame.setSize(new Dimension(600, 622));

                try {

                    // Simplest frame setup.
                    frame.getContentPane().add( new Renderer3D() );

                    //Display the window.
                    frame.pack();
                    frame.setSize(frame.getContentPane().getPreferredSize());
                    frame.setVisible(true);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        Point p1 = event.getPoint();
        if (! bMouseIsDragging) {
            bMouseIsDragging = true;
            previousMousePos = p1;
            return;
        }

        Point p0 = previousMousePos;
        Point dPos = new Point(p1.x-p0.x, p1.y-p0.y);

        InteractionMode mode = InteractionMode.ROTATE; // default drag mode is ROTATE
        if (event.isMetaDown()) // command-drag to zoom
            mode = InteractionMode.ZOOM;
        if (SwingUtilities.isMiddleMouseButton(event)) // middle drag to translate
            mode = InteractionMode.TRANSLATE;
        if (event.isShiftDown()) // shift-drag to translate
            mode = InteractionMode.TRANSLATE;

        if (mode == InteractionMode.TRANSLATE) {
            translatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ROTATE) {
            rotatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ZOOM) {
            zoomPixels(p1, p0);
            repaint();
        }

        previousMousePos = p1;
    }

    @Override
    public void mouseMoved(MouseEvent event) {}

    @Override
    public void mouseClicked(MouseEvent event) {
        bMouseIsDragging = false;
        // Double click to center
        if (event.getClickCount() == 2) {
            centerOnPixel(event.getPoint());
            repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        int notches = event.getWheelRotation();
        double zoomRatio = Math.pow(2.0, notches/50.0);
        zoom(zoomRatio);
        // Java does not seem to coalesce mouse wheel events,
        // giving the appearance of sluggishness.  So call repaint(),
        // not display().
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height)
    {
        // Keep roughly the same view as before by zooming
//        double zoomRatio = 1.0;
//        if (heightInPixels > 0) {
//            zoomRatio = (double)height/heightInPixels;
//        }
//        this.widthInPixels = width;
//        this.heightInPixels = height;
//
//        // System.out.println("reshape() called: x = "+x+", y = "+y+", width = "+width+", height = "+height);
//        final GL2 gl = gLDrawable.getGL().getGL2();
//
//        if (height <= 0) // avoid a divide by zero error!
//        {
//            height = 1;
//        }
//        if (zoomRatio != 1.0  &&  volumeModel.getCamera3d().getFocus().getY() != DEFAULT_CAMERA_FOCUS_DISTANCE )
//            zoom(zoomRatio);
//        updateProjection(gl);
//        gl.glMatrixMode(GL2.GL_MODELVIEW);
//        gl.glLoadIdentity();
//
//        double previousFocusDistance = volumeModel.getCamera3d().getFocus().getZ();
//        if ( previousFocusDistance == DEFAULT_CAMERA_FOCUS_DISTANCE ) {
//            BoundingBox3d boundingBox = getBoundingBox();
//            resetCameraFocus( boundingBox );
//        }
    }

    public void updateProjection(GL2 gl) {
//        gl.glViewport(0, 0, (int)widthInPixels, (int)heightInPixels);
//        double verticalApertureInDegrees = 180.0/Math.PI * 2.0 * Math.abs(
//                Math.atan2(heightInPixels/2.0, distanceToScreenInPixels));
//        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
//        gl.glMatrixMode(GL2.GL_PROJECTION);
//        gl.glLoadIdentity();
//        final float h = (float) widthInPixels / (float) heightInPixels;
//        double cameraFocusDistance = Math.abs(volumeModel.getCamera3d().getFocus().getZ());
//        glu.gluPerspective(verticalApertureInDegrees,
//                h,
//                0.5 * cameraFocusDistance,
//                2.0 * cameraFocusDistance);
//        gl.glPopAttrib();
    }

    public void zoom(double zoomRatio) {
        if (zoomRatio <= 0.0)
            return;
        if (zoomRatio == 1.0)
            return;
//        double cameraFocusDistance = volumeModel.getCamera3d().getFocus().getZ();
//        cameraFocusDistance /= zoomRatio;
//        volumeModel.getCamera3d().setFocus( 0.0, 0.0, cameraFocusDistance );
    }

    public void zoomPixels(Point newPoint, Point oldPoint) {
        // Are we dragging away from the center, or toward the center?
        double[] p0 = {oldPoint.x - widthInPixels/2.0,
                oldPoint.y - heightInPixels/2.0};
        double[] p1 = {newPoint.x - widthInPixels/2.0,
                newPoint.y - heightInPixels/2.0};
        double dC0 = Math.sqrt(p0[0]*p0[0]+p0[1]*p0[1]);
        double dC1 = Math.sqrt(p1[0]*p1[0]+p1[1]*p1[1]);
        double dC = dC1 - dC0; // positive means away
        double denom = Math.max(20.0, dC1);
        double zoomRatio = 1.0 + dC/denom;
        zoom(zoomRatio);
    }

    public void rotatePixels(double dx, double dy, double dz) {
        // Rotate like a trackball
        double dragDistance = Math.sqrt(dy*dy + dx*dx + dz*dz);
        if (dragDistance <= 0.0)
            return;
        UnitVec3 rotationAxis = new UnitVec3(dy, -dx, dz);
        double windowSize = Math.sqrt(
                widthInPixels*widthInPixels
                        + heightInPixels*heightInPixels);
        // Drag across the entire window to rotate all the way around
        double rotationAngle = 2.0 * Math.PI * dragDistance/windowSize;
        // System.out.println(rotationAxis.toString() + rotationAngle);
        Rotation3d rotation = new Rotation3d().setFromAngleAboutUnitVector(
                rotationAngle, rotationAxis);
        // System.out.println(rotation);
//        getVolumeModel().getCamera3d().setRotation( getVolumeModel().getCamera3d().getRotation().times( rotation.transpose() ) );
        // System.out.println(R_ground_camera);
    }

    public void translatePixels(double dx, double dy, double dz) {
        // trackball translate
//        Vec3 t = new Vec3(-dx, -dy, -dz).times(glUnitsPerPixel());
//        focusInGround.plusEquals(getVolumeModel().getCamera3d().getRotation().times(t));
    }

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    public void centerOnPixel(Point p) {
        // System.out.println("center");
        double dx =  p.x - widthInPixels/2.0;
        double dy = heightInPixels/2.0 - p.y;
        translatePixels(-dx, dy, 0.0);
    }

}
