package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by murphys on 8/31/2015.
 *
 * http://stackoverflow.com/questions/25255287/mouse-coordinates-relative-to-imageicon-within-a-jscrollpane
 */

public class ImagePanel extends JPanel {

    private BufferedImage img;

    public ImagePanel(BufferedImage img) {
        this.img = img;
    }

    @Override
    public Dimension getPreferredSize() {
        return img == null ? super.getPreferredSize() : new Dimension(img.getWidth(), img.getHeight());
    }

    protected Point getImageLocation() {
        Point p = null;
        if (img != null) {
            int x = (getWidth() - img.getWidth()) / 2;
            int y = (getHeight() - img.getHeight()) / 2;
            p = new Point(x, y);
        }
        return p;
    }

    public Point toImageContext(Point p) {
        Point imgLocation = getImageLocation();
        Point relative = new Point(p);
        relative.x -= imgLocation.x;
        relative.y -= imgLocation.y;
        return relative;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img != null) {
            Point p = getImageLocation();
            g.drawImage(img, p.x, p.y, this);
        }
    }

}
