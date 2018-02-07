package org.janelia.it.workstation.browser.gui.lasso;

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
 * Code copy and pasted from the ImageJA project. It's not possible to reuse their code as-is because of dependencies on AWT.
 */
public class BufferedImageCanvas extends JPanel implements ImageCanvas, MouseListener, MouseMotionListener  {

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

    private Roi currentRoi;
    private int mousePressedX, mousePressedY;
    private long mousePressedTime;

    private Image offScreenImage;
    private int offScreenWidth = 0;
    private int offScreenHeight = 0;
    private boolean mouseExited = true;
    private boolean customRoi;
    private AtomicBoolean paintPending;
    private boolean scaleToFit;
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

    public Dimension getPreferredSize() {
        return new Dimension(imageWidth, imageHeight);
    }
    
    /**Converts a screen x-coordinate to an offscreen x-coordinate.*/
    public int offScreenX(int sx) {
        return srcRect.x + (int)(sx/magnification);
    }
        
    /**Converts a screen y-coordinate to an offscreen y-coordinate.*/
    public int offScreenY(int sy) {
        return srcRect.y + (int)(sy/magnification);
    }
    
    /**Converts a screen x-coordinate to a floating-point offscreen x-coordinate.*/
    public double offScreenXD(int sx) {
        return srcRect.x + sx/magnification;
    }
        
    /**Converts a screen y-coordinate to a floating-point offscreen y-coordinate.*/
    public double offScreenYD(int sy) {
        return srcRect.y + sy/magnification;
    }

    /**Converts an offscreen x-coordinate to a screen x-coordinate.*/
    public int screenX(int ox) {
        return  (int)((ox-srcRect.x)*magnification);
    }
    
    /**Converts an offscreen y-coordinate to a screen y-coordinate.*/
    public int screenY(int oy) {
        return  (int)((oy-srcRect.y)*magnification);
    }

    /**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
    public int screenXD(double ox) {
        return  (int)((ox-srcRect.x)*magnification);
    }
    
    /**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
    public int screenYD(double oy) {
        return  (int)((oy-srcRect.y)*magnification);
    }
        
    public Rectangle getSrcRect() {
        return srcRect;
    }

    /** ImagePlus.updateAndDraw calls this method to force the paint()
        method to update the image from the ImageProcessor. */
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
        
//        if (IJ.debugMode) IJ.log("ImageCanvas.paint: "+imp);
                
        painted = true;
        Roi roi = imp.getRoi();
        if (roi!=null) {
//            if (roi!=null)
//                roi.updatePaste();
            if (imageWidth!=0) {
                paintDoubleBuffered(g);
                setPaintPending(false);
                return;
            }
        }
//        try {
            if (imageUpdated) {
                imageUpdated = false;
                imp.updateImage();
            }
//            setInterpolation(g, Prefs.interpolateScaledImages);
            Image img = imp.getImage();
            if (img!=null) {
                g.drawImage(img, 0, 0, (int)(srcRect.width*magnification+0.5), (int)(srcRect.height*magnification+0.5),
                        srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
            }
//            if (overlay!=null)
//                drawOverlay(overlay, g);
//            if (showAllOverlay!=null)
//                drawOverlay(showAllOverlay, g);
            if (roi!=null) drawRoi(roi, g);
//            if (srcRect.width<imageWidth || srcRect.height<imageHeight)
//                drawZoomIndicator(g);
//            if (IJ.debugMode) showFrameRate(g);
//        }
//        catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
        setPaintPending(false);
    }

    // Use double buffer to reduce flicker when drawing complex ROIs.
    // Author: Erik Meijering
    void paintDoubleBuffered(Graphics g) {
        final int srcRectWidthMag = (int)(srcRect.width*magnification+0.5);
        final int srcRectHeightMag = (int)(srcRect.height*magnification+0.5);
        if (offScreenImage==null || offScreenWidth!=srcRectWidthMag || offScreenHeight!=srcRectHeightMag) {
            offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
            offScreenWidth = srcRectWidthMag;
            offScreenHeight = srcRectHeightMag;
        }
        Roi roi = imp.getRoi();
//        try {
            if (imageUpdated) {
                imageUpdated = false;
                imp.updateImage();
            }
            Graphics offScreenGraphics = offScreenImage.getGraphics();
//            setInterpolation(offScreenGraphics, Prefs.interpolateScaledImages);
            Image img = imp.getImage();
            if (img!=null)
                offScreenGraphics.drawImage(img, 0, 0, srcRectWidthMag, srcRectHeightMag,
                    srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
//            if (overlay!=null)
//                drawOverlay(overlay, offScreenGraphics);
//            if (showAllOverlay!=null)
//                drawOverlay(showAllOverlay, offScreenGraphics);
            if (roi!=null)
                drawRoi(roi, offScreenGraphics);
//            if (srcRect.width<imageWidth || srcRect.height<imageHeight)
//                drawZoomIndicator(offScreenGraphics);
//            if (IJ.debugMode) showFrameRate(offScreenGraphics);
            g.drawImage(offScreenImage, 0, 0, null);
//        }
//        catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
    }

    private void drawRoi(Roi roi, Graphics g) {
//        if (roi==currentRoi) {
//            Color lineColor = roi.getStrokeColor();
//            Color fillColor = roi.getFillColor();
//            float lineWidth = roi.getStrokeWidth();
//            roi.setStrokeColor(null);
//            roi.setFillColor(null);
//            boolean strokeSet = roi.getStroke()!=null;
//            if (strokeSet)
//                roi.setStrokeWidth(1);
//            roi.draw(g);
//            roi.setStrokeColor(lineColor);
//            if (strokeSet)
//                roi.setStrokeWidth(lineWidth);
//            roi.setFillColor(fillColor);
//            currentRoi = null;
//        } else
            roi.draw(g);
    }
    
    /** Sets the cursor based on the current tool and cursor location. */
    public void setCursor(int sx, int sy, int ox, int oy) {
        xMouse = ox;
        yMouse = oy;
        mouseExited = false;
        Roi roi = imp.getRoi();
//        ImageWindow win = imp.getWindow();
//        overOverlayLabel = false;
//        if (win==null)
//            return;
//        if (IJ.spaceBarDown()) {
//            setCursor(handCursor);
//            return;
//        }
//        int id = Toolbar.getToolId();
//        switch (id) {
//            case Toolbar.MAGNIFIER:
//                setCursor(moveCursor);
//                break;
//            case Toolbar.HAND:
//                setCursor(handCursor);
//                break;
//            default:  //selection tool
//                PlugInTool tool = Toolbar.getPlugInTool();
//                boolean arrowTool = roi!=null && (roi instanceof Arrow) && tool!=null && "Arrow Tool".equals(tool.getToolName());
//                if ((id>=Toolbar.CUSTOM1) && !arrowTool) {
//                    if (Prefs.usePointerCursor)
//                        setCursor(defaultCursor);
//                    else
//                        setCursor(crosshairCursor);
//                } else if (roi!=null && roi.getState()!=roi.CONSTRUCTING && roi.isHandle(sx, sy)>=0) {
//                    setCursor(handCursor);
//                } else if ((overlay!=null||showAllOverlay!=null) && overOverlayLabel(sx,sy,ox,oy) && (roi==null||roi.getState()!=roi.CONSTRUCTING)) {
//                    overOverlayLabel = true;
//                    setCursor(handCursor);
//                } else if (Prefs.usePointerCursor || (roi!=null && roi.getState()!=roi.CONSTRUCTING && roi.contains(ox, oy)))
//                    setCursor(defaultCursor);
//                else
//                    setCursor(crosshairCursor);
//        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        xMouse = offScreenX(x);
        yMouse = offScreenY(y);
        flags = e.getModifiers();
        mousePressedX = mousePressedY = -1;
        //IJ.log("mouseDragged: "+flags);
        if (flags==0)  // workaround for Mac OS 9 bug
            flags = InputEvent.BUTTON1_MASK;
//        if (Toolbar.getToolId()==Toolbar.HAND || IJ.spaceBarDown())
//            scroll(x, y);
//        else {
//            PlugInTool tool = Toolbar.getPlugInTool();
//            if (tool!=null) {
//                tool.mouseDragged(imp, e);
//                if (e.isConsumed()) return;
//            }
//            IJ.setInputEvent(e);
            Roi roi = imp.getRoi();
            if (roi != null)
                roi.handleMouseDrag(x, y, flags);
//        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //if (ij==null) return;
        int sx = e.getX();
        int sy = e.getY();
        int ox = offScreenX(sx);
        int oy = offScreenY(sy);
        flags = e.getModifiers();
        setCursor(sx, sy, ox, oy);
        mousePressedX = mousePressedY = -1;
//        IJ.setInputEvent(e);
//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null) {
//            tool.mouseMoved(imp, e);
//            if (e.isConsumed()) return;
//        }
        Roi roi = imp.getRoi();
        int type = roi!=null?roi.getType():-1;
        if (type>0 && (type==Roi.POLYGON||type==Roi.POLYLINE||type==Roi.ANGLE||type==Roi.LINE) 
        && roi.getState()==roi.CONSTRUCTING)
            roi.mouseMoved(e);
        else {
            if (ox<imageWidth && oy<imageHeight) {
//                ImageWindow win = imp.getWindow();
//                // Cursor must move at least 12 pixels before text
//                // displayed using IJ.showStatus() is overwritten.
//                if ((sx-sx2)*(sx-sx2)+(sy-sy2)*(sy-sy2)>144)
//                    showCursorStatus = true;
//                if (win!=null&&showCursorStatus) win.mouseMoved(ox, oy);
            } 
//            else
//                IJ.showStatus("");
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null)
//            tool.mouseClicked(imp, e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
//        showCursorStatus = true;
//        int toolID = Toolbar.getToolId();
//        ImageWindow win = imp.getWindow();
//        if (win!=null && win.running2 && toolID!=Toolbar.MAGNIFIER) {
//            if (win instanceof StackWindow)
//                ((StackWindow)win).setAnimate(false);
//            else
//                win.running2 = false;
//            return;
//        }
        
        int x = e.getX();
        int y = e.getY();
        flags = e.getModifiers();
        
//        if (toolID!=Toolbar.MAGNIFIER && (e.isPopupTrigger()||(!IJ.isMacintosh()&&(flags&Event.META_MASK)!=0))) {
//            handlePopupMenu(e);
//            return;
//        }

        int ox = offScreenX(x);
        int oy = offScreenY(y);
        xMouse = ox; yMouse = oy;
//        if (IJ.spaceBarDown()) {
//            // temporarily switch to "hand" tool of space bar down
//            setupScroll(ox, oy);
//            return;
//        }
//        
//        if (overOverlayLabel && (overlay!=null||showAllOverlay!=null)) {
//            if (activateOverlayRoi(ox, oy))
//                return;
//        }
        
        mousePressedX = ox;
        mousePressedY = oy;
        mousePressedTime = System.currentTimeMillis();
        
//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null) {
//            tool.mousePressed(imp, e);
//            if (e.isConsumed()) return;
//        }
//        if (customRoi && overlay!=null)
//            return;
//
//        if (toolID>=Toolbar.CUSTOM1) {
//            if (tool!=null && "Arrow Tool".equals(tool.getToolName()))
//                handleRoiMouseDown(e);
//            else
//                Toolbar.getInstance().runMacroTool(toolID);
//            return;
//        }

//        switch (toolID) {
//            case Toolbar.MAGNIFIER:
//                if (IJ.shiftKeyDown())
//                    zoomToSelection(ox, oy);
//                else if ((flags & (Event.ALT_MASK|Event.META_MASK|Event.CTRL_MASK))!=0) {
//                    zoomOut(x, y);
//                    if (getMagnification()<1.0)
//                        imp.repaintWindow();
//                } else {
//                    zoomIn(x, y);
//                    if (getMagnification()<=1.0)
//                        imp.repaintWindow();
//                }
//                break;
//            case Toolbar.HAND:
//                setupScroll(ox, oy);
//                break;
//            case Toolbar.DROPPER:
//                setDrawingColor(ox, oy, IJ.altKeyDown());
//                break;
//            case Toolbar.WAND:
//                Roi roi = imp.getRoi();
//                double tolerance = WandToolOptions.getTolerance();
//                if (roi!=null && (tolerance==0.0||imp.isThreshold()) && roi.contains(ox, oy)) {
//                    Rectangle r = roi.getBounds();
//                    if (r.width==imageWidth && r.height==imageHeight)
//                        imp.deleteRoi();
//                    else if (!e.isAltDown()) {
//                        handleRoiMouseDown(e);
//                        return;
//                    }
//                }
//                if (roi!=null) {
//                    int handle = roi.isHandle(x, y);
//                    if (handle>=0) {
//                        roi.mouseDownInHandle(handle, x, y);
//                        return;
//                    }
//                }
//                setRoiModState(e, roi, -1);
//                String mode = WandToolOptions.getMode();
//                if (Prefs.smoothWand)
//                    mode = mode + " smooth";
//                int npoints = IJ.doWand(ox, oy, tolerance, mode);
//                if (Recorder.record && npoints>0) {
//                    if (Recorder.scriptMode())
//                        Recorder.recordCall("IJ.doWand(imp, "+ox+", "+oy+", "+tolerance+", \""+mode+"\");");
//                    else {
//                        if (tolerance==0.0 && mode.equals("Legacy"))
//                            Recorder.record("doWand", ox, oy);
//                        else
//                            Recorder.recordString("doWand("+ox+", "+oy+", "+tolerance+", \""+mode+"\");\n");
//                    }
//                }
//                break;
//            case Toolbar.OVAL:
//                if (Toolbar.getBrushSize()>0)
//                    new RoiBrush();
//                else
//                    handleRoiMouseDown(e);
//                break;
//            default:  //selection tool
                handleRoiMouseDown(e);
//        }
    }

    protected void handleRoiMouseDown(MouseEvent e) {
        int sx = e.getX();
        int sy = e.getY();
        int ox = offScreenX(sx);
        int oy = offScreenY(sy);
        Roi roi = imp.getRoi();
        int handle = roi!=null?roi.isHandle(sx, sy):-1;
//        boolean multiPointMode = roi!=null && (roi instanceof PointRoi) && handle==-1
//            && Toolbar.getToolId()==Toolbar.POINT && Toolbar.getMultiPointMode();
//        if (multiPointMode) {
//            double oxd = offScreenXD(sx);
//            double oyd = offScreenYD(sy);
//            if (e.isShiftDown() && !IJ.isMacro()) {
//                FloatPolygon points = roi.getFloatPolygon();
//                if (points.npoints>0) {
//                    double x0 = points.xpoints[0];
//                    double y0 = points.ypoints[0];
//                    double slope = Math.abs((oxd-x0)/(oyd-y0));
//                    if (slope>=1.0)
//                        oyd = points.ypoints[0];
//                    else
//                        oxd = points.xpoints[0];
//                }
//            }
//            ((PointRoi)roi).addPoint(imp, oxd, oyd);
//            imp.setRoi(roi);
//            return;
//        }
        setRoiModState(e, roi, handle);
        if (roi!=null) {
//            if (handle>=0) {
//                roi.mouseDownInHandle(handle, sx, sy);
//                return;
//            }
            Rectangle r = roi.getBounds();
            int type = roi.getType();
//            if (type==Roi.RECTANGLE && r.width==imp.getWidth() && r.height==imp.getHeight()
//            && roi.getPasteMode()==Roi.NOT_PASTING && !(roi instanceof ImageRoi)) {
//                imp.deleteRoi();
//                return;
//            }
//            if (roi.contains(ox, oy)) {
//                if (roi.modState==Roi.NO_MODS)
//                    roi.handleMouseDown(sx, sy);
//                else {
//                    imp.deleteRoi();
//                    imp.createNewRoi(sx,sy);
//                }
//                return;
//            }
            if ((type==Roi.POLYGON || type==Roi.POLYLINE || type==Roi.ANGLE)
            && roi.getState()==roi.CONSTRUCTING)
                return;
//            int tool = Toolbar.getToolId();
//            if ((tool==Toolbar.POLYGON||tool==Toolbar.POLYLINE||tool==Toolbar.ANGLE)&& !(IJ.shiftKeyDown()||IJ.altKeyDown())) {
//                imp.deleteRoi();
//                return;
//            }
        }
        imp.createNewRoi(sx,sy);
    }

    void setRoiModState(MouseEvent e, Roi roi, int handle) {
        if (roi==null || (handle>=0 && roi.modState==Roi.NO_MODS))
            return;
        if (roi.state==Roi.CONSTRUCTING)
            return;
//        int tool = Toolbar.getToolId();
//        if (tool>Toolbar.FREEROI && tool!=Toolbar.WAND && tool!=Toolbar.POINT)
//            {roi.modState = Roi.NO_MODS; return;}
        if (e.isShiftDown())
            roi.modState = Roi.ADD_TO_ROI;
        else if (e.isAltDown())
            roi.modState = Roi.SUBTRACT_FROM_ROI;
        else
            roi.modState = Roi.NO_MODS;
        //IJ.log("setRoiModState: "+roi.modState+" "+ roi.state);
    }
    
    
    @Override
    public void mouseReleased(MouseEvent e) {
        int ox = offScreenX(e.getX());
        int oy = offScreenY(e.getY());
//        if ((overlay!=null||showAllOverlay!=null) && ox==mousePressedX && oy==mousePressedY) {
//            boolean cmdDown = IJ.isMacOSX() && e.isMetaDown();
//            Roi roi = imp.getRoi();
//            if (roi!=null && roi.getBounds().width==0)
//                roi=null;
//            if ((e.isAltDown()||e.isControlDown()||cmdDown) && roi==null) {
//                if (activateOverlayRoi(ox, oy))
//                    return;
//            } else if ((System.currentTimeMillis()-mousePressedTime)>250L && !drawingTool()) {
//                if (activateOverlayRoi(ox,oy))
//                    return;
//            }
//        }

//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null) {
//            tool.mouseReleased(imp, e);
//            if (e.isConsumed()) return;
//        }
        flags = e.getModifiers();
        flags &= ~InputEvent.BUTTON1_MASK; // make sure button 1 bit is not set
        flags &= ~InputEvent.BUTTON2_MASK; // make sure button 2 bit is not set
        flags &= ~InputEvent.BUTTON3_MASK; // make sure button 3 bit is not set
        Roi roi = imp.getRoi();
        if (roi != null) {
            Rectangle r = roi.getBounds();
            int type = roi.getType();
            if ((r.width==0 || r.height==0)
            && !(type==Roi.POLYGON||type==Roi.POLYLINE||type==Roi.ANGLE||type==Roi.LINE)
//            && !(roi instanceof TextRoi)
            && roi.getState()==roi.CONSTRUCTING
            && type!=roi.POINT)
                imp.deleteRoi();
            else
                roi.handleMouseUp(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null)
//            tool.mouseEntered(imp, e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
//        PlugInTool tool = Toolbar.getPlugInTool();
//        if (tool!=null) {
//            tool.mouseExited(imp, e);
//            if (e.isConsumed()) return;
//        }
//        ImageWindow win = imp.getWindow();
//        if (win!=null)
//            setCursor(defaultCursor);
//        IJ.showStatus("");
        mouseExited = true;
    }

}
