package org.janelia.it.workstation.browser.gui.lasso;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

public class BufferedImageProcessor implements ImageProcessor {

    static final int INVERT=0, FILL=1, ADD=2, MULT=3, AND=4, OR=5,
        XOR=6, GAMMA=7, LOG=8, MINIMUM=9, MAXIMUM=10, SQR=11, SQRT=12, EXP=13, ABS=14, SET=15;
    
    protected BufferedImage image;
    protected byte[] snapshotPixels;
    protected int width, snapshotWidth;
    protected int height, snapshotHeight;
    protected int roiX, roiY, roiWidth, roiHeight;
    protected int xMin, xMax, yMin, yMax;
    protected ImageProcessor mask;
    protected Color drawingColor = Color.black;
    int fgColor = 0;
    
    public BufferedImageProcessor(BufferedImage image) {
        this.image = image;
    }
    
    public BufferedImageProcessor(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public Object getPixels() {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        return pixels;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    /** Returns a duplicate of this image. */ 
    @Override
    public ImageProcessor duplicate() { 
        return new BufferedImageProcessor(deepCopy(image)); 
    } 
    
    // From https://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    
    /** Fills the image or ROI bounding rectangle with the current fill/draw value. Use
    *   fill(Roi) or fill(ip.getMask()) to fill non-rectangular selections.
    *   @see #setColor(Color)
    *   @see #setValue(double)
    *   @see #fill(Roi)
    */
    @Override
    public void fill() {
        process(FILL, 0.0);
    }

    private void process(int op, double value) {
        double SCALE = 255.0/Math.log(255.0);
        int v;
        
        int[] lut = new int[256];
        for (int i=0; i<256; i++) {
            switch(op) {
                case INVERT:
                    v = 255 - i;
                    break;
                case FILL:
                    v = fgColor;
                    break;
                case SET:
                    v = (int)value;
                    break;
                case ADD:
                    v = i + (int)value;
                    break;
                case MULT:
                    v = (int)Math.round(i * value);
                    break;
                case AND:
                    v = i & (int)value;
                    break;
                case OR:
                    v = i | (int)value;
                    break;
                case XOR:
                    v = i ^ (int)value;
                    break;
                case GAMMA:
                    v = (int)(Math.exp(Math.log(i/255.0)*value)*255.0);
                    break;
                case LOG:
                    if (i==0)
                        v = 0;
                    else
                        v = (int)(Math.log(i) * SCALE);
                    break;
                case EXP:
                    v = (int)(Math.exp(i/SCALE));
                    break;
                case SQR:
                        v = i*i;
                    break;
                case SQRT:
                        v = (int)Math.sqrt(i);
                    break;
                case MINIMUM:
                    if (i<value)
                        v = (int)value;
                    else
                        v = i;
                    break;
                case MAXIMUM:
                    if (i>value)
                        v = (int)value;
                    else
                        v = i;
                    break;
                 default:
                    v = i;
            }
            if (v < 0)
                v = 0;
            if (v > 255)
                v = 255;
            lut[i] = v;
        }
        applyTable(lut);
    }
    
    public void applyTable(int[] lut) {
        int lineStart, lineEnd;
        for (int y=roiY; y<(roiY+roiHeight); y++) {
            lineStart = y * width + roiX;
            lineEnd = lineStart + roiWidth;
            for (int i=lineEnd; --i>=lineStart;) {
                byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                pixels[i] = (byte)lut[pixels[i]&0xff];
            }
        }
    }

    /** Inverts the image or ROI. */
    @Override
    public void invert() {process(INVERT, 0.0);}
    
    /** Defines a rectangular region of interest and sets the mask 
        to null if this ROI is not the same size as the previous one. 
        @see ImageProcessor#resetRoi        
    */
    @Override
    public void setRoi(Rectangle roi) {
        if (roi==null)
            resetRoi();
        else
            setRoi(roi.x, roi.y, roi.width, roi.height);
    }

    /** Defines a rectangular region of interest and sets the mask to 
        null if this ROI is not the same size as the previous one. 
        @see ImageProcessor#resetRoi        
    */
    @Override
    public void setRoi(int x, int y, int rwidth, int rheight) {
//        if (x<0 || y<0 || x+rwidth>width || y+rheight>height) {
//            //find intersection of roi and this image
//            Rectangle r1 = new Rectangle(x, y, rwidth, rheight);
//            Rectangle r2 = r1.intersection(new Rectangle(0, 0, width, height));
//            if (r2.width<=0 || r2.height<=0) {
//                roiX=0; roiY=0; roiWidth=0; roiHeight=0;
//                xMin=0; xMax=0; yMin=0; yMax=0;
//                mask=null;
//                return;
//            }
//            if (mask!=null && mask.getWidth()==rwidth && mask.getHeight()==rheight) {
//                Rectangle r3 = new Rectangle(0, 0, r2.width, r2.height);
//                if (x<0) r3.x = -x;
//                if (y<0) r3.y = -y;
//                mask.setRoi(r3);
//                mask = mask.crop();
//            }
//            roiX=r2.x; roiY=r2.y; roiWidth=r2.width; roiHeight=r2.height;
//        } else {
            roiX=x; roiY=y; roiWidth=rwidth; roiHeight=rheight;
//        }
        if (mask!=null && (mask.getWidth()!=roiWidth||mask.getHeight()!=roiHeight))
            mask = null;
        //setup limits for 3x3 filters
        xMin = Math.max(roiX, 1);
        xMax = Math.min(roiX + roiWidth - 1, width - 2);
        yMin = Math.max(roiY, 1);
        yMax = Math.min(roiY + roiHeight - 1, height - 2);
    }
    
    /** Defines a non-rectangular region of interest that will consist of a
        rectangular ROI and a mask. After processing, call <code>reset(mask)</code>
        to restore non-masked pixels. Here is an example:
        <pre>
        ip.setRoi(new OvalRoi(50, 50, 100, 50));
        ip.fill();
        ip.reset(ip.getMask());
        </pre>
        The example assumes <code>snapshot()</code> has been called, which is the case
        for code executed in the <code>run()</code> method of plugins that implement the 
        <code>PlugInFilter</code> interface.
        @see ij.ImagePlus#getRoi
    */
    @Override
    public void setRoi(Roi roi) {
        if (roi==null)
            resetRoi();
        else {
//            if (roi instanceof PointRoi && ((PointRoi)roi).getNCoordinates()==1) {
//                setMask(null);
//                FloatPolygon p = roi.getFloatPolygon();
//                setRoi((int)p.xpoints[0], (int)p.ypoints[0], 1, 1);
//            } else {
                setMask(roi.getMask());
                setRoi(roi.getBounds());
//            }
        }
    }

    @Override
    /** Sets the ROI (Region of Interest) and clipping rectangle to the entire image. */
    public void resetRoi() {
//        roiX=0; roiY=0; roiWidth=width; roiHeight=height;
//        xMin=1; xMax=width-2; yMin=1; yMax=height-2;
//        mask=null;
//        clipXMin=0; clipXMax=width-1; clipYMin=0; clipYMax=height-1; 
    }

    /** Returns a Rectangle that represents the current
        region of interest. */
    public Rectangle getRoi() {
        return new Rectangle(roiX, roiY, roiWidth, roiHeight);
    }

    /**
     * Defines a byte mask that limits processing to an irregular ROI.
     * Background pixels in the mask have a value of zero.
     */
    @Override
    public void setMask(ImageProcessor mask) {
        this.mask = mask;
    }

    /**
     * For images with irregular ROIs, returns a mask, otherwise, returns null.
     * Pixels outside the mask have a value of zero.
     */
    @Override
    public ImageProcessor getMask() {
        return mask;
    }
    
    /** Restore pixels that are within roi but not part of mask. */
    @Override
    public void reset(ImageProcessor mask) {
//        if (mask==null || snapshotPixels==null)
//            return; 
//        if (mask.getWidth()!=roiWidth||mask.getHeight()!=roiHeight)
//            throw new IllegalArgumentException(maskSizeError(mask));
//        byte[] mpixels = (byte[])mask.getPixels();
//        for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
//            int i = y * width + roiX;
//            int mi = my * roiWidth;
//            for (int x=roiX; x<(roiX+roiWidth); x++) {
//                if (mpixels[mi++]==0)
//                    pixels[i] = snapshotPixels[i];
//                i++;
//            }
//        }
    }
    

    /** Sets the foreground drawing color. */
    @Override
    public void setColor(Color color) {
        drawingColor = color;
        //fgColor = getBestIndex(color);
    }
}
