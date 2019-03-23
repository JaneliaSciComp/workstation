package org.janelia.it.workstation.browser.gui.lasso;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Code copy and pasted from the ImageJA project. It's not possible to reuse their code as-is because of dependencies on AWT.
 */
public class BufferedImagePlus implements ImagePlus {

    protected Image img;
    protected ImageProcessor ip;
    protected ImageCanvas canvas;
    protected Roi roi;
    
    public BufferedImagePlus(BufferedImage image) {
        this.img = image;
        this.ip = new ColorProcessor(image);
        this.canvas = new BufferedImageCanvas(this);
    }
    
    public ImageProcessor getImageProcessor() {
        return ip;
    }

    @Override
    public int getID() {
        return -1;
    }
    
    @Override
    /** Returns this image as a AWT image. */
    public Image getImage() {
        if (img==null && ip!=null)
            img = ip.createImage();
        return img;
    }
    
    @Override
    public ImageCanvas getCanvas() {
        return canvas;
    }

    @Override
    public int getWidth() {
        return ip.getWidth();
    }

    @Override
    public int getHeight() {
        return ip.getHeight();
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    /** Draws the image. If there is an ROI, its
        outline is also displayed.  Does nothing if there
        is no window associated with this image (i.e. show()
        has not been called).*/
    @Override
    public void draw(){
        if (canvas!=null) {
            canvas.repaint();
        }
    }

    /** Updates this image from the pixel data in its 
        associated ImageProcessor, then displays it. Does
        nothing if there is no window associated with
        this image (i.e. show() has not been called).*/
    @Override
    public synchronized void updateAndDraw() {
//        if (stack!=null && !stack.isVirtual() && currentSlice>=1 && currentSlice<=stack.getSize()) {
//            Object pixels = stack.getPixels(currentSlice);
//            if (ip!=null && pixels!=null && pixels!=ip.getPixels()) { // was stack updated?
//                try {
//                    ip.setPixels(pixels);
//                    ip.setSnapshotPixels(null);
//                } catch(Exception e) {}
//            }
//        }
        if (canvas!=null) {
            canvas.setImageUpdated();
//            if (listeners.size()>0) notifyListeners(UPDATED);
        }
        draw();
    }
    
    /** ImageCanvas.paint() calls this method when the
        ImageProcessor has generated a new image. */
    @Override
    public void updateImage() {
        if (ip!=null)
            img = ip.createImage();
    }
    
    /** Draws image and roi outline using a clip rect. */
    @Override
    public void draw(int x, int y, int width, int height){
        if (canvas!=null) {
            ImageCanvas ic = canvas;
            double mag = 1.0;//ic.getMagnification();
            x = ic.screenX(x);
            y = ic.screenY(y);
            width = (int)(width*mag);
            height = (int)(height*mag);
            ic.repaint(x, y, width, height);
//            if (listeners.size()>0 && roi!=null && roi.getPasteMode()!=Roi.NOT_PASTING)
//                notifyListeners(UPDATED);
        }
    }

    /** For images with irregular ROIs, returns a byte mask, otherwise, returns
        null. Mask pixels have a non-zero value. */
    public ImageProcessor getMask() {
        if (roi==null) {
            if (ip!=null) ip.resetRoi();
            return null;
        }
        ImageProcessor mask = roi.getMask();
        if (mask==null)
            return null;
        if (ip!=null && roi!=null) {
            ip.setMask(mask);
            ip.setRoi(roi.getBounds());
        }
        return mask;
    }
    
    /** Returns the current selection, or null if there is no selection. */
    @Override
    public Roi getRoi() {
        return roi;
    }
    
    /** Assigns the specified ROI to this image and displays it. Any existing
        ROI is deleted if <code>roi</code> is null or its width or height is zero. */
    @Override
    public void setRoi(Roi newRoi) {
        setRoi(newRoi, true);
    }

    /** Assigns 'newRoi'  to this image and displays it if 'updateDisplay' is true. */
    public void setRoi(Roi newRoi, boolean updateDisplay) {
        Rectangle bounds = newRoi.getBounds();
        if (newRoi.isVisible()) {
//            if ((newRoi instanceof Arrow) && newRoi.getState()==Roi.CONSTRUCTING && bounds.width==0 && bounds.height==0) {
//                deleteRoi();
//                roi = newRoi;
//                return;
//            }
            newRoi = (Roi)newRoi.clone();
//            if (newRoi==null)
//                {deleteRoi(); return;}
        }
//        if (bounds.width==0 && bounds.height==0 && !(newRoi.getType()==Roi.POINT||newRoi.getType()==Roi.LINE))
//            {deleteRoi(); return;}
        roi = newRoi;
        if (ip!=null) {
            ip.setMask(null);
            if (roi.isArea())
                ip.setRoi(bounds);
            else
                ip.resetRoi();
        }
        roi.setImage(this);
        if (updateDisplay)
            draw();
//        roi.notifyListeners(RoiListener.CREATED);
    }
    

    /** Starts the process of creating a new selection, where sx and sy are the
        starting screen coordinates. The selection type is determined by which tool in
        the tool bar is active. The user interactively sets the selection size and shape. */
    public void createNewRoi(int sx, int sy) {
        deleteRoi();
//        switch (Toolbar.getToolId()) {
//            case Toolbar.RECTANGLE:
//                if (Toolbar.getRectToolType()==Toolbar.ROTATED_RECT_ROI)
//                    roi = new RotatedRectRoi(sx, sy, this);
//                else
//                    roi = new Roi(sx, sy, this, Toolbar.getRoundRectArcSize());
//                break;
//            case Toolbar.OVAL:
//                if (Toolbar.getOvalToolType()==Toolbar.ELLIPSE_ROI)
//                    roi = new EllipseRoi(sx, sy, this);
//                else
//                    roi = new OvalRoi(sx, sy, this);
//                break;
//            case Toolbar.POLYGON:
//            case Toolbar.POLYLINE:
//            case Toolbar.ANGLE:
//                roi = new PolygonRoi(sx, sy, this);
//                break;
//            case Toolbar.FREEROI:
//            case Toolbar.FREELINE:
                roi = new FreehandRoi(sx, sy, this);
//                break;
//            case Toolbar.LINE:
//                if ("arrow".equals(Toolbar.getToolName()))
//                    roi = new Arrow(sx, sy, this);
//                else
//                    roi = new Line(sx, sy, this);
//                break;
//            case Toolbar.TEXT:
//                roi = new TextRoi(sx, sy, this);
//                break;
//            case Toolbar.POINT:
//                roi = new PointRoi(sx, sy, this);
//                if (Prefs.pointAddToOverlay) {
//                    int measurements = Analyzer.getMeasurements();
//                    if (!(Prefs.pointAutoMeasure && (measurements&Measurements.ADD_TO_OVERLAY)!=0))
//                        IJ.run(this, "Add Selection...", "");
//                    Overlay overlay2 = getOverlay();
//                    if (overlay2!=null)
//                        overlay2.drawLabels(!Prefs.noPointLabels);
//                    Prefs.pointAddToManager = false;
//                }
//                if (Prefs.pointAutoMeasure || (Prefs.pointAutoNextSlice&&!Prefs.pointAddToManager))
//                    IJ.run(this, "Measure", "");
//                if (Prefs.pointAddToManager) {
//                    IJ.run(this, "Add to Manager ", "");
//                    ImageCanvas ic = getCanvas();
//                    if (ic!=null) {
//                        RoiManager rm = RoiManager.getInstance();
//                        if (rm!=null) {
//                            if (Prefs.noPointLabels)
//                                rm.runCommand("show all without labels");
//                            else
//                                rm.runCommand("show all with labels");
//                        }
//                    }
//                }
//                if (Prefs.pointAutoNextSlice && getStackSize()>1) {
//                    IJ.run(this, "Next Slice [>]", "");
//                    deleteRoi();
//                }
//                break;
//        }
//        if (roi!=null)
//            roi.notifyListeners(RoiListener.CREATED);
    }

    /** Deletes the current region of interest. Makes a copy of the ROI
        so it can be recovered by Edit/Selection/Restore Selection. */
    public void deleteRoi() {
        if (roi!=null) {
            saveRoi();
//            if (!(IJ.altKeyDown()||IJ.shiftKeyDown())) {
//                RoiManager rm = RoiManager.getInstance();
//                if (rm!=null)
//                    rm.deselect(roi);
//            }
//            if (roi!=null) {
//                roi.notifyListeners(RoiListener.DELETED);
//                if (roi instanceof PointRoi)
//                    ((PointRoi)roi).resetCounters();
//            }
            roi = null;
            if (ip!=null)
                ip.resetRoi();
            draw();
        }
    }

    /** Deletes the current region of interest. */
    public void killRoi() {
        deleteRoi();
    }

    public synchronized void saveRoi() {
        if (roi!=null) {
//            roi.endPaste();
            Rectangle r = roi.getBounds();
            if ((r.width>0 || r.height>0)) {
                Roi.previousRoi = (Roi)roi.clone();
//                if (IJ.debugMode) IJ.log("saveRoi: "+roi);
            }
        }
    }
}
