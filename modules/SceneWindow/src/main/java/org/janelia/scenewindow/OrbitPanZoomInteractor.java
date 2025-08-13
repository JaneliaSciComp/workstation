
package org.janelia.scenewindow;

import java.awt.Color;
import org.janelia.geometry3d.AbstractCamera;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.janelia.model.domain.tiledMicroscope.TmOperation;
import org.janelia.workstation.controller.TmViewerManager;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vantage manages a View matrix
 * @author brunsc
 */
public class OrbitPanZoomInteractor
extends SceneInteractor
implements MouseListener, MouseMotionListener, MouseWheelListener, MouseInputListener
{
    private Point previousPoint = null;
    private Cursor openHandCursor;
    private Cursor grabHandCursor;
    private Cursor rotateCursor;
    private Cursor crosshairCursor;
    private Cursor currentCursor;
    private Component component;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public OrbitPanZoomInteractor(AbstractCamera camera, Component component) 
    {
        super(camera, component);
        createCursorImages();
        this.component = component;
        checkCursor(crosshairCursor);
    }

	protected void checkCursor(Cursor newCursor) {
		if (newCursor == null)
			return;
		currentCursor = newCursor;
		if (component == null)
			return;
		if (component.getCursor() == currentCursor)
			return;
		component.setCursor(currentCursor);
	}
	
    private Cursor loadLocalCursorImage(String fileName, int centerX, int centerY, String description) {
        URL url = OrbitPanZoomInteractor.class.getResource(fileName);
        if (url == null)
            return null;
        Image image = null;
        try {
            image = ImageIO.read(url);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (image == null)
            return null;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        ImageIcon icon = new ImageIcon(image);
        Dimension imageSize = new Dimension(icon.getIconWidth(), icon.getIconHeight());
		Dimension cursorSize = toolkit.getBestCursorSize(imageSize.width, imageSize.height);
		if (! cursorSize.equals(imageSize)) {
			int w = (int)cursorSize.width;
			int h = (int)cursorSize.height;
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics g = bi.createGraphics();
			g.setColor(new Color(0f, 0f, 0f, 0f));
			g.fillRect(0, 0, w, h);
			icon.paintIcon(null, g, 0, 0);
			image = bi;
		}

        return toolkit.createCustomCursor(image, new Point(centerX, centerY), description);
    }
    
    private void createCursorImages() {
        openHandCursor = loadLocalCursorImage("grab_opened.png", 8, 8, "openHandCursor");
        grabHandCursor = loadLocalCursorImage("grab_closed.png", 8, 8, "grabHandCursor");
        rotateCursor = loadLocalCursorImage("rotate_icon.png", 4, 4, "rotateCursor");
        crosshairCursor = loadLocalCursorImage("crosshair3.png", 8, 8, "crosshairCursor");
    }
    
    @Override
    public String getToolTipText() {
        return ""
                + "Middle-drag to rotate";
    } 

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            // double click to recenter
            if ( recenterOnMouse(event) )
                 notifyObservers();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        previousPoint = event.getPoint();
        // System.out.println("mouse pressed");
        if (event.isPopupTrigger()) {
            // Do not change cursor on popup
        }
        // else if ((event.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
        else if (isRotateMode(event)) {
			checkCursor(rotateCursor);
		}
        else if (isPanMode(event)) {
			checkCursor(grabHandCursor);
		}
        else {
            checkCursor(crosshairCursor);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        previousPoint = null;
        // System.out.println("mouse released");
        checkCursor(crosshairCursor);
    }

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}

    @Override
    public void mouseDragged(MouseEvent event) {
        if (event.isConsumed()) {
            // log.info("Skipping consumed Pan drag event");
            return; // So TracingInteractor can avoid world drag
        }
        // log.info("PanZoom dragging");
        boolean bChanged = false;
        if (previousPoint != null) {
            int dx = event.getPoint().x - previousPoint.x;
            int dy = event.getPoint().y - previousPoint.y;
            if ( (dx != 0) || (dy != 0) ) {
                // Left drag to pan
                if (isPanMode(event)) {
                    bChanged = panPixels(-dx, dy, 0);
                }
                // Middle drag to rotate
                else if (isRotateMode(event)) {
                    long startTime = System.currentTimeMillis();
                    if (camera.getVantage().isConstrainedToUpDirection())
                        bChanged = orbitPixels(dx, -dy, 6.0f);
                    else 
                        bChanged = rotatePixels(dx, -dy, 6.0f);
                    long endTime = System.currentTimeMillis();
                    TmViewerManager.getInstance().logOperation(TmOperation.Activity.ROTATE_SCREEN,
                            null, endTime-startTime);
                }
            }
        }
        if (bChanged) 
            notifyObservers();
        previousPoint = event.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        // System.out.println("Mouse wheel moved");
        if (zoomMouseWheel(event, 0.15f))
            notifyObservers();
    }
    
    private boolean isRotateMode(MouseEvent event) {
        // Middle drag to rotate
        if (SwingUtilities.isMiddleMouseButton(event))
            return true;
        // OR Shift left-drag to rotate
        if (SwingUtilities.isLeftMouseButton(event) && event.isShiftDown())
            return true;
        return false;
    }
    
    private boolean isPanMode(MouseEvent event) {
        // Shift-drag is rotate
        if (isRotateMode(event))
            return false;
        // Other left-drag is pan
        if (SwingUtilities.isLeftMouseButton(event))
            return true;
        return false;
    }

}
