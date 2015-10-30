package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by murphys on 9/1/2015.
 */
public class ColorPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableColorRowPanel.class);

    private BufferedImage img;
    boolean initialized=false;
    Color color;

    public ColorPanel(int width, int height, Color color) {
        img=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.color=color;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        initialized=false;
        logger.info("setColor set to "+color.toString());
        repaint();
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
            if (!initialized) {
                initializeImage();
                initialized=true;
            }
            Point p = getImageLocation();
            g.drawImage(img, p.x, p.y, this);
        }
    }

    private void initializeImage() {
        Graphics2D g = img.createGraphics();
        g.setPaint (color);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
    }

}
