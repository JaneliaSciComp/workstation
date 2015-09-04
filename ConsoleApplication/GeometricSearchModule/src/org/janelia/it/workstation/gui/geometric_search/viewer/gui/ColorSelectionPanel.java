package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by murphys on 9/1/2015.
 */
public class ColorSelectionPanel extends JPanel {

    private static final int DEFAULT_WIDTH=90;
    private static final int DEFAULT_HEIGHT=30;

    private BufferedImage img;
    boolean initialized=false;

    public ColorSelectionPanel() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ColorSelectionPanel(int width, int height) {
        img=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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

        float XMAX=1.0f*img.getWidth()-1.0f;
        float YMAX=1.0f*img.getHeight()-1.0f;
        for (int x=0;x<img.getWidth();x++) {
            for (int y=0;y<img.getHeight();y++) {
                float xf=x*1.0f;
                float yf=y*1.0f;
                float xn=(xf/XMAX)*3.0f;
                float yn=yf/YMAX;
                int red=0;
                int green=0;
                int blue=0;

                if (xn <= 1.0f) {               // RED...green(X) blue (Y)
                    red=255;
                    green=(int)(255f*xn);
                    blue=(int)(255f*yn);
                } else if (xn <= 2.0f) {        // GREEN...red(X) blue (Y)
                    xn -= 1.0f;
                    red=(int)(255f*xn);
                    green=255;
                    blue=(int)(255f*yn);
                } else {                        // BLUE...red (X) green (Y)
                    xn -= 2.0f;
                    red=(int)(255f*xn);
                    green=(int)(255f*yn);
                    blue=255;
                }

                g.setPaint(new Color(red, green, blue));
                g.fillRect(x, y, x+1, y+1);
            }
        }
    }

    public Color getColorFromClickCoordinate(Point point) {
        Color color = new Color(img.getRGB(point.x, point.y));
        return color;
    }

}
