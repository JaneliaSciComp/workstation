package org.janelia.it.workstation.ab2;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2GLPanel extends GLJPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(AB2GLPanel.class);
    protected static GLProfile profile = null;
    protected static GLCapabilities capabilities = null;

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    AB2Renderer renderer;
    AB2Controller controller;

    protected Point previousMousePos;
    protected boolean bMouseIsDragging = false;


    static {
        try {
            profile = GLProfile.get(GLProfile.GL4);
            capabilities = new GLCapabilities(profile);
        } catch ( Throwable th ) {
            profile = null;
            capabilities = null;
            logger.warn("JOGL is unavailable. No 3D images will be shown.");
        }
    }

    public JPopupMenu popupMenu;

    public AB2GLPanel(int width, int height, AB2Renderer renderer, AB2Controller controller) {
        super(capabilities);
        this.renderer=renderer;
        this.controller=controller;
        popupMenu = new JPopupMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setPreferredSize( new Dimension( width, height ) );
        addGLEventListener(renderer);
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getPreferredSize();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (bMouseIsDragging) {
            bMouseIsDragging=false;
        }
        maybeShowPopup(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    /** External addition to this conveniently-central popup menu. */
    public void addMenuAction( Action action ) {
        popupMenu.add(action);
    }

    public void releaseMenuActions() {
        popupMenu.removeAll();
    }

    public void refresh() {
        repaint();
    }

    public boolean refreshIfPending() {
        if (model.getGLModel().hasPendingEvents()) {
            refresh();
            return true;
        }
        return false;
    }

    public void refreshRendering()
    {
    }

    public void clear() {
        renderer.clear();
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        resetView();
    }

    public void resetView() {
        repaint();
    }

    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        renderer.setResetFirstRedraw(resetFirstRedraw);
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
            renderer.translatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ROTATE) {
            renderer.rotatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ZOOM) {
            renderer.zoomPixels(p1, p0);
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
            renderer.centerOnPixel(event.getPoint());
            repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        int notches = event.getWheelRotation();
        double zoomRatio = Math.pow(2.0, notches/50.0);
        renderer.zoom(zoomRatio);
        // Java does not seem to coalesce mouse wheel events,
        // giving the appearance of sluggishness.  So call repaint(),
        // not display().
        repaint();
    }

    protected void maybeShowPopup(MouseEvent event)
    {
        if (event.isPopupTrigger()) {
            popupMenu.show(event.getComponent(),
                    event.getX(), event.getY());
        }
    }
}
