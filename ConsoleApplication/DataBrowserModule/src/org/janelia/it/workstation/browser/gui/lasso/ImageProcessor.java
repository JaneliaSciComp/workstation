package org.janelia.it.workstation.browser.gui.lasso;

import java.awt.Color;
import java.awt.Rectangle;

public interface ImageProcessor {

    Object getPixels();

    int getWidth();

    int getHeight();

    void setRoi(Rectangle bounds);

    void resetRoi();

    void setRoi(Roi roi);

    void setRoi(int x, int y, int rwidth, int rheight);

    void fill();

    void reset(ImageProcessor mask);

    Rectangle getRoi();
    
    void setMask(ImageProcessor mask);

    ImageProcessor getMask();

    void setColor(Color backgroundColor);

    ImageProcessor duplicate();
    
    void invert();



}
