package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by murphys on 4/10/15.
 */
public class VoxelViewerGLPanel extends GLJPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewerGLPanel.class);
    protected static GLProfile profile = null;
    protected static GLCapabilities capabilities = null;

    public enum InteractionMode {
        ROTATE,
        TRANSLATE,
        ZOOM
    }

    protected VoxelViewerProperties properties;
    VoxelViewerModel model;
    VoxelViewerRenderer renderer;

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

    public VoxelViewerGLPanel(int width, int height, VoxelViewerModel model) {
        super(capabilities);
        this.model=model;
        popupMenu = new JPopupMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);


        // Context menu for resetting view
        JMenuItem resetViewItem = new JMenuItem("Reset view");
        resetViewItem.addActionListener(this);
        popupMenu.add(resetViewItem);
        renderer=new VoxelViewerRenderer(model);
        renderer.setProperties(properties);
        setPreferredSize( new Dimension( width, height ) );

        addGLEventListener(renderer);
    }

    public void setProperties(VoxelViewerProperties properties) {
        this.properties=properties;
        renderer.setProperties(properties);
    }

    public GL4ShaderProperties getProperties() {
        return properties;
    }

    @Override
    public Dimension getPreferredSize() {
        int width=1200;
        int height=800;
        if (properties!=null) {
            try {
                width=properties.getInteger(VoxelViewerProperties.GL_VIEWER_WIDTH_INT);
                height=properties.getInteger(VoxelViewerProperties.GL_VIEWER_HEIGHT_INT);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new Dimension(width, height);
    }
    
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
        renderer.setPixelDimensions(preferredSize.getWidth(), preferredSize.getHeight());
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

    public VoxelViewerModel getModel() {
        return model;
    }

    public VoxelViewerRenderer getRenderer() { return renderer; }

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
        resetView();
    }

    public void resetView() {
        renderer.resetView();
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
