package org.janelia.scenewindow.fps;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class FPSGraph extends JComponent
{
    public FPSGraph() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // super.paintComponent(g);
        
        // black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // TODO - useful foreground
        g.setColor(Color.GREEN);
        g.drawString("Test", 10, 10);
    }
}
