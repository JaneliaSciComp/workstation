package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by murphys on 9/1/2015.
 */
public class ColorPanel extends JPanel {

    private BufferedImage img;
    Color color;

    public ColorPanel(int width, int height, Color color) {
        img=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        if (color!=null) {
            this.color=new Color(0, 0, 0);
        }
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public Dimension getPreferredSize() {
        return img == null ? super.getPreferredSize() : new Dimension(img.getWidth(), img.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img != null) {
            g.setColor(color);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
        }
    }

}
