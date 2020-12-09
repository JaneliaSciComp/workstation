package org.janelia.workstation.controller.model.color;

import javax.swing.*;
import java.awt.*;

public class ColorSwatch extends JComponent {
    Color color;

    public void paint(Graphics g) {
        g.setColor(color);
        g.fillRect( 0, 0, 15, 15);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(15,15);
    }
}
