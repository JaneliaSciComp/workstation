package org.janelia.workstation.browser.gui.lasso;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

/**
 * Code copy and pasted from the ImageJA project. It's not possible to reuse
 * their code as-is because of dependencies on AWT.
 */
public class BufferedImageCanvas extends JPanel implements ImageCanvas, MouseListener, MouseMotionListener {

    protected ImagePlus imp;
    protected boolean imageUpdated;
    protected Rectangle srcRect;
    protected int imageWidth, imageHeight;
    protected int xMouse; // current cursor offscreen x location 
    protected int yMouse; // current cursor offscreen y location

    protected double magnification;

    protected int xMouseStart;
    protected int yMouseStart;
    protected int xSrcStart;
    protected int ySrcStart;
    protected int flags;

    private int mousePressedX, mousePressedY;
    private long mousePressedTime;

    private Image offScreenImage;
    private int offScreenWidth = 0;
    private int offScreenHeight = 0;
    private boolean mouseExited = true;
    private AtomicBoolean paintPending;
    private boolean painted;

    private static boolean controlDown, altDown, spaceDown, shiftDown;

    public BufferedImageCanvas(ImagePlus imp) {
        this.imp = imp;
        paintPending = new AtomicBoolean(false);
        int width = imp.getWidth();
        int height = imp.getHeight();
        imageWidth = width;
        imageHeight = height;
        srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
        setSize(imageWidth, imageHeight);
        magnification = 1.0;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(imageWidth, imageHeight);
    }

    /**
     * Converts a screen x-coordinate to an offscreen x-coordinate.
     */
    @Override
    public int offScreenX(int sx) {
        return srcRect.x + (int) (sx / magnification);
    }

    /**
     * Converts a screen y-coordinate to an offscreen y-coordinate.
     */
    @Override
    public int offScreenY(int sy) {
        return srcRect.y + (int) (sy / magnification);
    }

    /**
     * Converts a screen x-coordinate to a floating-point offscreen x-coordinate.
     */
    @Override
    public double offScreenXD(int sx) {
        return srcRect.x + sx / magnification;
    }

    /**
     * Converts a screen y-coordinate to a floating-point offscreen y-coordinate.
     */
    @Override
    public double offScreenYD(int sy) {
        return srcRect.y + sy / magnification;
    }

    /**
     * Converts an offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenX(int ox) {
        return (int) ((ox - srcRect.x) * magnification);
    }

    /**
     * Converts an offscreen y-coordinate to a screen y-coordinate.
     */
    @Override
    public int screenY(int oy) {
        return (int) ((oy - srcRect.y) * magnification);
    }

    /**
     * Converts a floating-point offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenXD(double ox) {
        return (int) ((ox - srcRect.x) * magnification);
    }

    /**
     * Converts a floating-point offscreen x-coordinate to a screen x-coordinate.
     */
    @Override
    public int screenYD(double oy) {
        return (int) ((oy - srcRect.y) * magnification);
    }

    @Override
    public Rectangle getSrcRect() {
        return srcRect;
    }

    /**
     * ImagePlus.updateAndDraw calls this method to force the paint() method to
     * update the image from the ImageProcessor.
     */
    @Override
    public void setImageUpdated() {
        imageUpdated = true;
    }

    public void setPaintPending(boolean state) {
        paintPending.set(state);
    }

    public boolean getPaintPending() {
        return paintPending.get();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        painted = true;
        Roi roi = imp.getRoi();
        if (roi != null) {
            if (imageWidth != 0) {
                paintDoubleBuffered(g);
                setPaintPending(false);
                return;
            }
        }
        if (imageUpdated) {
            imageUpdated = false;
            imp.updateImage();
        }
        Image img = imp.getImage();
        if (img != null) {
            g.drawImage(img, 0, 0, (int) (srcRect.width * magnification + 0.5), (int) (srcRect.height * magnification + 0.5),
                    srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, null);
        }
        if (roi != null) {
            drawRoi(roi, g);
        }
        setPaintPending(false);
    }

    // Use double buffer to reduce flicker when drawing complex ROIs.
    // Author: Erik Meijering
    void paintDoubleBuffered(Graphics g) {
        final int srcRectWidthMag = (int) (srcRect.width * magnification + 0.5);
        final int srcRectHeightMag = (int) (srcRect.height * magnification + 0.5);
        if (offScreenImage == null || offScreenWidth != srcRectWidthMag || offScreenHeight != srcRectHeightMag) {
            offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
            offScreenWidth = srcRectWidthMag;
            offScreenHeight = srcRectHeightMag;
        }
        Roi roi = imp.getRoi();
        if (imageUpdated) {
            imageUpdated = false;
            imp.updateImage();
        }
        Graphics offScreenGraphics = offScreenImage.getGraphics();
        Image img = imp.getImage();
        if (img != null) {
            offScreenGraphics.drawImage(img, 0, 0, srcRectWidthMag, srcRectHeightMag,
                    srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, null);
        }
        if (roi != null) {
            drawRoi(roi, offScreenGraphics);
        }
        g.drawImage(offScreenImage, 0, 0, null);
    }

    private void drawRoi(Roi roi, Graphics g) {
        roi.draw(g);
    }

    /**
     * Sets the cursor based on the current tool and cursor location.
     */
    public void setCursor(int sx, int sy, int ox, int oy) {
        xMouse = ox;
        yMouse = oy;
        mouseExited = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        xMouse = offScreenX(x);
        yMouse = offScreenY(y);
        flags = e.getModifiers();
        mousePressedX = mousePressedY = -1;
        if (flags == 0) {
            // workaround for Mac OS 9 bug
            flags = InputEvent.BUTTON1_MASK;
        }
        Roi roi = imp.getRoi();
        if (roi != null) {
            roi.handleMouseDrag(x, y, flags);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int sx = e.getX();
        int sy = e.getY();
        int ox = offScreenX(sx);
        int oy = offScreenY(sy);
        flags = e.getModifiers();
        setCursor(sx, sy, ox, oy);
        mousePressedX = mousePressedY = -1;
        Roi roi = imp.getRoi();
        int type = roi != null ? roi.getType() : -1;
        if (type > 0 && (type == Roi.POLYGON || type == Roi.POLYLINE || type == Roi.ANGLE || type == Roi.LINE)
                && roi.getState() == Roi.CONSTRUCTING) {
            roi.mouseMoved(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // No action
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        flags = e.getModifiers();
        int ox = offScreenX(x);
        int oy = offScreenY(y);
        xMouse = ox;
        yMouse = oy;
        mousePressedX = ox;
        mousePressedY = oy;
        mousePressedTime = System.currentTimeMillis();
        handleRoiMouseDown(e);
    }

    protected void handleRoiMouseDown(MouseEvent e) {
        int sx = e.getX();
        int sy = e.getY();
        int ox = offScreenX(sx);
        int oy = offScreenY(sy);
        Roi roi = imp.getRoi();
        int handle = roi != null ? roi.isHandle(sx, sy) : -1;
        setRoiModState(e, roi, handle);
        if (roi != null) {
            Rectangle r = roi.getBounds();
            int type = roi.getType();
            if ((type == Roi.POLYGON || type == Roi.POLYLINE || type == Roi.ANGLE)
                    && roi.getState() == roi.CONSTRUCTING) {
                return;
            }
        }
        imp.createNewRoi(sx, sy);
    }

    void setRoiModState(MouseEvent e, Roi roi, int handle) {
        if (roi == null || (handle >= 0 && roi.modState == Roi.NO_MODS)) {
            return;
        }
        if (roi.state == Roi.CONSTRUCTING) {
            return;
        }
        if (e.isShiftDown()) {
            roi.modState = Roi.ADD_TO_ROI;
        } else if (e.isAltDown()) {
            roi.modState = Roi.SUBTRACT_FROM_ROI;
        } else {
            roi.modState = Roi.NO_MODS;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        flags = e.getModifiers();
        flags &= ~InputEvent.BUTTON1_MASK; // make sure button 1 bit is not set
        flags &= ~InputEvent.BUTTON2_MASK; // make sure button 2 bit is not set
        flags &= ~InputEvent.BUTTON3_MASK; // make sure button 3 bit is not set
        Roi roi = imp.getRoi();
        if (roi != null) {
            Rectangle r = roi.getBounds();
            int type = roi.getType();
            if ((r.width == 0 || r.height == 0)
                    && !(type == Roi.POLYGON || type == Roi.POLYLINE || type == Roi.ANGLE || type == Roi.LINE)
                    && roi.getState() == Roi.CONSTRUCTING
                    && type != Roi.POINT) {
                imp.deleteRoi();
            } else {
                roi.handleMouseUp(e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseExited = true;
    }

}
