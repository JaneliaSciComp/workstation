package org.janelia.workstation.browser.gui.lasso;

import java.awt.Image;

/**
 * Interface to mock ImageJA's ImagePlus.
 */
public interface ImagePlus {

    int getID();

    Image getImage();
    
    ImageCanvas getCanvas();

    int getWidth();

    int getHeight();

    int getStackSize();
    
    void draw(int clipX, int clipY, int clipWidth, int clipHeight);

    void draw();

    void setRoi(Roi roi2);

    Roi getRoi();

    void createNewRoi(int sx, int sy);

    void deleteRoi();

    void updateImage();

    void updateAndDraw();

    ImageProcessor getImageProcessor();

    ImageProcessor getMask();





}
