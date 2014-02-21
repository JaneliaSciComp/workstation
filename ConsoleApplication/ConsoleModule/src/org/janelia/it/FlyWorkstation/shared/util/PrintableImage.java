package org.janelia.it.FlyWorkstation.shared.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/7/11
 * Time: 10:10 AM
 */
public class PrintableImage extends JComponent implements Printable {
    //---------------------------------------MEMBER VARIABLES
    private Image mImage;
    private Dimension mPreferredSize = new Dimension(0, 0); //Nvr a null moment!

    //---------------------------------------CONSTRUCTORS

    /**
     * Constructor takes the name of an image file to read.
     *
     * @param lFileName the name of the file. (full path)
     */
    public PrintableImage(String lFileName) {
        // readToBuffer(lFileName);
        ImageIcon lImageIcon = new ImageIcon(lFileName);
        setup(lImageIcon);
    } // End constructor

    /**
     * Constructor takes the name of an image file to read.
     *
     * @param lBufferedImage the name of the file. (full path)
     */
    public PrintableImage(BufferedImage lBufferedImage) {
        setup(lBufferedImage);
    } // End constructor

    /**
     * Constructor takes the name of an image file to read.
     *
     * @param lJPEGBuffer the name of the file. (full path)
     */
    public PrintableImage(byte[] lJPEGBuffer) {
        ImageIcon lImageIcon = new ImageIcon(lJPEGBuffer);
        setup(lImageIcon);
    } // End constructor

    //---------------------------------------OVERRIDES FOR JComponent

    /**
     * Overrides the paint method so the image may be drawn.
     *
     * @param lGraphics the graphics context on which to draw.
     */
    public void paint(Graphics lGraphics) {
        lGraphics.drawImage(mImage, 0, 0, null);
    } // End method: paint

    /**
     * Returns the pref'd size for the component.
     *
     * @return Dimension the pref'd size.
     */
    public Dimension getPreferredSize() {
        return mPreferredSize;
    } // End method: getPreferredSize

    /**
     * Returns width. Overridden here to allow query of width to return
     * that of the image being drawn.
     *
     * @return int the width of the image.
     */
    public int getWidth() {
        return mPreferredSize.width;
    } // End method: getWidth

    /**
     * Returns height. Overridden here to allow query of height to return
     * that of the image being drawn.
     *
     * @return int the width of the image.
     */
    public int getHeight() {
        return mPreferredSize.height;
    } // End method: getHeight

    /**
     * Overrides so size never changes.
     */
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    //----------------------------------------IMPLEMENTATION OF Printable

    /**
     * Used to print the contents of the screen.
     *
     * @param graphics the context onto which to print.
     * @return int 0
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        double scaleFactorX = 0.0;
        double scaleFactorY = 0.0;
        scaleFactorX = pageFormat.getImageableWidth() / getPreferredSize().getWidth();
        scaleFactorY = pageFormat.getImageableHeight() / getPreferredSize().getHeight();

        graphics.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

        if (graphics instanceof Graphics2D) {
            ((Graphics2D) graphics).scale(scaleFactorX, scaleFactorY);
        } // 2D Graphics Available.

        paint(graphics);
        if (pageIndex >= 1) return NO_SUCH_PAGE;
        return PAGE_EXISTS;

    } // End method: print
    //---------------------------------------HELPER METHODS

    /**
     * Sets up the characteristics of the component, given the image icon.
     *
     * @param lImageIcon a pre-loaded image icon.
     */
    private void setup(ImageIcon lImageIcon) {
        mImage = lImageIcon.getImage();
        mPreferredSize = new Dimension(mImage.getWidth(null), mImage.getHeight(null));
    } // End method: setup

    /**
     * Sets up the characteristics of the component, given the loaded image.
     *
     * @param lImage a pre-loaded image.
     */
    private void setup(Image lImage) {
        mImage = lImage;
        mPreferredSize = new Dimension(mImage.getWidth(null), mImage.getHeight(null));
    } // End method: setup
}
