package org.janelia.it.workstation.ab2.renderer;

import java.awt.Point;

public interface AB2Renderer3DControls {

    public void rotatePixels(double dx, double dy, double dz);

    public void translatePixels(double dx, double dy, double dz);

    public void zoomPixels(Point newPoint, Point oldPoint);

    public void zoom(double zoomRatio);

}



