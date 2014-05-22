package org.janelia.it.workstation.shared.util;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/7/11
 * Time: 10:14 AM
 */
public class PrintableComponent implements Printable {

    Component comp;
    double scaleFactor = 0.0;

    public PrintableComponent(Component comp) {
        this.comp = comp;
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        if (scaleFactor == 0.0) {
            double scaleFactorX = 0.0;
            double scaleFactorY = 0.0;
            scaleFactorX = pageFormat.getImageableWidth() / comp.getWidth();
            scaleFactorY = pageFormat.getImageableHeight() / comp.getHeight();
            scaleFactor = (scaleFactorX < scaleFactorY) ? scaleFactorX : scaleFactorY;
        }
        graphics.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
        if (graphics instanceof Graphics2D) {
            ((Graphics2D) graphics).scale(scaleFactor, scaleFactor);
        }
        comp.printAll(graphics);
        if (pageIndex >= 1) return NO_SUCH_PAGE;
        return PAGE_EXISTS;
    }
}