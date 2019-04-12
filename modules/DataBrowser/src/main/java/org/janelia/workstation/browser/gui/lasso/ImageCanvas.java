package org.janelia.workstation.browser.gui.lasso;

import java.awt.Rectangle;

/**
 * Interface to mock ImageJA's ImageCanvas.
 */
public interface ImageCanvas {

    int screenX(int ox);

    int screenY(int oy);

    int screenXD(double startXD);

    int screenYD(double startYD);
    
    int offScreenX(int sx);

    int offScreenY(int sy);
    
    double offScreenXD(int sx);

    double offScreenYD(int sy);

    Rectangle getSrcRect();

    void repaint(int x, int y, int width, int height);

    void repaint();

    void setImageUpdated();

}
