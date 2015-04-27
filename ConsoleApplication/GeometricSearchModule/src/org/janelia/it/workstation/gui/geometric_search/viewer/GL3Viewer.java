package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.geometric_search.gl.GL3ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.gl.GL3SimpleActor;
import org.janelia.it.workstation.gui.opengl.GLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by murphys on 4/10/15.
 */
public class GL3Viewer extends GLJPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(GL3Viewer.class);

    protected static GLProfile profile = null;
    protected static GLCapabilities capabilities = null;


    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    GL3Model model;
    GL3Renderer renderer;

    protected Point previousMousePos;
    protected boolean bMouseIsDragging = false;


    static {
        try {
            profile = GLProfile.get(GLProfile.GL3);
            capabilities = new GLCapabilities(profile);
        } catch ( Throwable th ) {
            profile = null;
            capabilities = null;
            logger.warn("JOGL is unavailable. No 3D images will be shown.");
        }
    }

    public JPopupMenu popupMenu;

    public GL3Viewer() {
        super(capabilities);
        popupMenu = new JPopupMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        setPreferredSize( new Dimension( 400, 400 ) );

        // Context menu for resetting view
        JMenuItem resetViewItem = new JMenuItem("Reset view");
        resetViewItem.addActionListener(this);
        popupMenu.add(resetViewItem);
        model=new GL3Model();
        renderer=new GL3Renderer(model);
        addGLEventListener(renderer);
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public GL3Model getModel() {
        return model;
    }

    public GL3Renderer getRenderer() { return renderer; }

    /** External addition to this conveniently-central popup menu. */
    public void addMenuAction( Action action ) {
        popupMenu.add(action);
    }

    public void releaseMenuActions() {
        popupMenu.removeAll();
    }

    public void refresh() {
        model.setModelUpdate();
    }

    public void refreshRendering() {
        model.setRenderUpdate();
    }

    public void clear() {
        renderer.clear();
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // System.out.println("reset view");
        resetView();
    }

    public void resetView() {
        renderer.resetView();
        repaint();
    }

    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        renderer.setResetFirstRedraw(resetFirstRedraw);
    }

    /**
     * Add any actor to this Mip as desired.
     */
    public void addShaderAction(GL3ShaderActionSequence shaderAction) {
        addActorToRenderer(shaderAction);
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

    /** Special synchronized method, for adding actors. Supports multi-threaded brick-add. */
    private void addActorToRenderer(GL3ShaderActionSequence shaderAction) {
        synchronized ( this ) {
            renderer.addShaderAction(shaderAction);
            renderer.resetView();
        }
    }

}
