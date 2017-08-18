package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.janelia.it.workstation.browser.model.ImageDecorator;

/**
 * An customization of DecoratedImagePanel which shows an error instead of the intended image, 
 * but maintains the decorations.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DecoratedErrorPanel extends DecoratedImagePanel {

    private static final float ICON_OPACITY = 1.0f;
    
    private String text;
    private Color fontColor;
    
    public DecoratedErrorPanel(List<ImageDecorator> decorators, String text, Color fontColor) {
        super(decorators);
        this.text = text;
        this.fontColor = fontColor;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);    
        
        Rectangle viewRect = new Rectangle(0, 0, getWidth(), getHeight());
        
        // Draw a border
//        g.setColor(UIManager.getColor("ws.ComponentBorderColor"));
//        g.drawRect(viewRect.x, viewRect.y, viewRect.width-1, viewRect.height-1);
    
        int fontSize = (int) Math.round(getWidth() * 0.005) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        
        FontMetrics metrics = g.getFontMetrics(titleLabelFont);
        
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        
        String clippedLabel = SwingUtilities.layoutCompoundLabel(
                this, 
                metrics, 
                text,
                null,
                SwingConstants.CENTER,
                SwingConstants.CENTER,
                SwingConstants.CENTER,
                SwingConstants.CENTER,
                viewRect,
                iconR,
                textR,
                0);
        
        int yLimit = textR.y - metrics.getHeight();
        int iconX = (viewRect.width - image.getWidth()) / 2;
        int iconY = (viewRect.height - image.getHeight()) / 2;
        
        if (image.getHeight() < yLimit) {
            // Both icon and label fit. Draw the label first.
            g.setFont(titleLabelFont);  
            if (fontColor!=null) {
                g.setColor(fontColor);
            }
            else {
                g.setColor(UIManager.getColor("Label.disabledForeground"));    
            }
            g.drawString(clippedLabel, textR.x, textR.y);
            // Put the icon above the text
            iconY = yLimit - image.getHeight();
            // Tooltip on the text
            tooltipLocations.put(textR, text);
        }
        else {
            // Tooltip everywhere
            tooltipLocations.put(viewRect, text);
        }
        
        // Draw the icon with lowered opacity
        Graphics2D g2 = (Graphics2D)g;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ICON_OPACITY));
        
        g2.drawImage(image, iconX, iconY, image.getWidth(), image.getHeight(), null);
    }
    
    @Override
    protected void paintImage(Graphics g) {
        // Don't paint the image as usual, it needs to be positioned with the text above
    }
}
