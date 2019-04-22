package org.janelia.workstation.browser.gui.listview.icongrid;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.janelia.workstation.core.model.Decorator;

/**
 * An image with some decorators overlaid on top of it.
 *
 * Rendering works in two very different modes defined by fillParent.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DecoratedImage extends JComponent {
    
    private static final float ICON_OPACITY = 1.0f;
    private static final float DECORATOR_OPACITY = 0.9f;

    /**
     * If this is true, then the image will be painted at the size determined by the parent container.
     * Otherwise, its preferredSize will be calculated and set based on the size of the given content. 
     */
    private boolean fillParent = false;
    
    /**
     * If fillParent is true, this determines whether the aspect ratio of the image is maintained.
     * Set to <code>false</code> to allow the image to distort to fill the component.
     */
    protected boolean proportionate = true;
    
    private final int decoratorOffset = 5;
    private final int decoratorSpacing = 10;
    private final int padding = 5;
    private final int textSpacing = 5;
    
    private final Map<Rectangle, String> tooltipLocations = new HashMap<>();
 
    private BufferedImage image;
    private final List<Decorator> decorators;
    private String text;
    private Color fontColor;

    public DecoratedImage(BufferedImage image, List<Decorator> decorators) {
        this(image, decorators, null, null);
    }
    
    public DecoratedImage(BufferedImage image, List<Decorator> decorators, String text) {
        this(image, decorators, text, null);
    }
    
    public DecoratedImage(BufferedImage image, List<Decorator> decorators, String text, Color fontColor) {
        setImage(image);
        this.decorators = decorators;
        this.text = text;
        this.fontColor = fontColor;
        ToolTipManager.sharedInstance().registerComponent(this);
        setOpaque(false);
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        updatePreferredSize();
    }

    public void setFillParent(boolean fillParent) {
        this.fillParent = fillParent;
    }
    
    public void setText(String text, Color fontColor) {
        this.text = text;
        this.fontColor = fontColor;
        updatePreferredSize();
    }

    private void updatePreferredSize() {

        if (fillParent) return;
        
        int w = 100;
        int h = 100;
        
        if (image!=null) {
            w = image.getWidth();
            h = image.getHeight();
        }

        if (text!=null) {
            Rectangle viewRect = new Rectangle(0, 0, w, h);
            Rectangle iconR = new Rectangle();
            Rectangle textR = new Rectangle();
            layout(text, viewRect, iconR, textR);
            
            if (textR.width > w) w = textR.width;
            h += textR.height + textSpacing;
            // I can't figure out why this needs a fudge factor, but it works for now:  
            h += 15;
        }
        
        setPreferredSize(new Dimension(w, h));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle bounds = calculateBounds();
        if (text==null) {
            paintImage(g, bounds);
        }
        else {
            paintImageWithText(g, bounds);
        }
        paintDecorators(g, bounds);
        paintTitles(g, bounds);
    }  
    
    /**
     * Calculate the bounds that we have to draw within.
     * @return a rectangle that can be drawn in
     */
    private Rectangle calculateBounds() {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int w = getWidth() - x - insets.right;
        int h = getHeight() - y - insets.bottom;
        return new Rectangle(x, y, w, h); 
    }
    
    /**
     * Paint the image (if any) within the given bounds.
     * @param g
     * @param bounds
     */
    private void paintImage(Graphics g, Rectangle bounds) {
        if (image!=null) {
            // For now, force align all images to top center because it looks best. 
            // This should be made into an alignment setting.
            int x = bounds.x;
            int y = bounds.y;
            int w = bounds.width;
            int h = bounds.height;
            
            if (fillParent) {
                int iw = image.getWidth();
                int ih = image.getHeight();
                
                // To fill the parent, we resize proportionately and stretch the image to the available space.
                if (iw * h < ih * w) {
                    iw = (h * iw) / ih;
                    x += (w - iw) / 2;
                    w = iw;
                }
                else {
                    ih = (w * ih) / iw;
                    y += (h - ih) / 2;
                    h = ih;
                }
            }
            else {
                // In this case, we assume that the preferred size was already set to accommodate the image.
                if (image.getWidth() < w)
                    w = image.getWidth();
                if (image.getHeight() < h)
                    h = image.getHeight();
                // If the image is smaller than the bounds, center it at the top.
                if (w < bounds.width) {
                    x += (bounds.width - w) / 2;
                }
            }
            
            g.drawImage(image, x, y, w, h, null);
        }
    }
    
    private void paintImageWithText(Graphics g, Rectangle bounds) {

        Font titleLabelFont = getTitleFont();
        Rectangle viewRect = bounds;
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        String clippedLabel = layout(text, viewRect, iconR, textR);
        
        if (image==null) {
            // No image, draw only the text
            if (clippedLabel!=null) {
                paintText(g, clippedLabel, titleLabelFont, textR);
            }
        }
        else {
            // Center the image by itself
            int imageX = (viewRect.width - image.getWidth()) / 2;
            int imageY = (viewRect.height - image.getHeight()) / 2;
            
            // Add text, if necessary
            if (clippedLabel!=null) {    
                // Calculate the full content height (icon + text)
                int contentHeight = image.getHeight() + textSpacing + textR.height;
                
                if (contentHeight + padding*2 < viewRect.getHeight()) {
                    // Both icon and label fit, center both vertically.
                    imageY = (viewRect.height - contentHeight) / 2;
                    textR.y = imageY + image.getHeight() + textSpacing; 
                    
                    // Draw the text
                    paintText(g, clippedLabel, titleLabelFont, textR);
                    
                    // Add a tooltip on the text
                    tooltipLocations.put(textR, text);
                }
                else {
                    // Only the icon fits, it will be drawn centered.
                    // Make sure to show the tooltip everywhere.
                    tooltipLocations.put(viewRect, text);
                }
            }
            
            // Set opacity
            Graphics2D g2 = (Graphics2D)g;
            Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ICON_OPACITY));
            
            // Draw the icon
            g2.drawImage(image, imageX, imageY, image.getWidth(), image.getHeight(), null);
            
            // Restore composite
            g2.setComposite(savedComposite);
        }
    }

    private void paintText(Graphics g, String text, Font titleLabelFont, Rectangle textR) {
        g.setFont(titleLabelFont);  
        if (fontColor!=null) {
            g.setColor(fontColor);
        }
        else {
            g.setColor(UIManager.getColor("Label.disabledForeground"));    
        }
        g.drawString(text, textR.x, textR.y + textR.height);
        
    }

    private void paintDecorators(Graphics g, Rectangle bounds) {
        if (decorators!=null && !decorators.isEmpty()) {

            int totalWidth = (int)bounds.getWidth();
            int x = totalWidth-decoratorOffset;

            // Set opacity
            Graphics2D g2 = (Graphics2D)g;
            Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DECORATOR_OPACITY));
            
            // Draw the decorators
            for (Decorator imageDecorator : decorators) {
                ImageIcon icon = imageDecorator.getIcon();
                x -= icon.getIconWidth();
                paintDecorator(g, imageDecorator, icon, x, bounds.y+decoratorOffset);
                x -= decoratorSpacing;
            }
            
            // Restore composite
            g2.setComposite(savedComposite);
        }
    }
    
    private void paintDecorator(Graphics g, Decorator imageDecorator, ImageIcon decorator, int x, int y) {
        Rectangle rect = new Rectangle(x, y, decorator.getIconWidth(), decorator.getIconHeight());
        tooltipLocations.put(rect, imageDecorator.getLabel());
        g.drawImage(decorator.getImage(), x, y, decorator.getIconWidth(), decorator.getIconHeight(), null);
    }
    
    private void paintTitles(Graphics g, Rectangle bounds) {

//      int textY = decoratorOffset; 
//
//      g.setColor(UIManager.getColor("Label.foreground"));
//      
//      if (!StringUtils.isBlank(title)) {
//          int fontSize = (int) Math.round(image.getWidth() * 0.005) + 10;
//          Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
//          
//          FontMetrics metrics = g.getFontMetrics(titleLabelFont);
//          Rectangle2D stringBounds = metrics.getStringBounds(title, g);
//
//          textY += stringBounds.getHeight();
//          
//          g.setFont(titleLabelFont);
//          g.drawString(title, decoratorOffset, textY);
//          
//      }
//
//      if (!StringUtils.isBlank(subtitle)) {
//          int fontSize = (int) Math.round(image.getWidth() * 0.003) + 10;
//          Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
//
//          FontMetrics metrics = g.getFontMetrics(titleLabelFont);
//          Rectangle2D stringBounds = metrics.getStringBounds(subtitle, g);
//          
//          textY += stringBounds.getHeight();
//          
//          g.setFont(titleLabelFont);
//          g.drawString(subtitle, decoratorOffset, textY);
//      }
    }


    private Font getTitleFont() {
        int fontSize = (int) Math.round(getWidth() * 0.005) + 10;
        return new Font("Sans Serif", Font.PLAIN, fontSize);
    }

    private String layout(String text, Rectangle viewRect, Rectangle iconRect, Rectangle textRect) {
        Font font = getTitleFont();
        if (text!=null) {
            return SwingUtilities.layoutCompoundLabel(
                    this, 
                    getFontMetrics(font), 
                    text,
                    null,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    viewRect,
                    iconRect,
                    textRect,
                    0);
        }
        return null;
    }
    
    @Override
    public String getToolTipText(MouseEvent e) {
        for (Rectangle rect : tooltipLocations.keySet()) {
            if (rect.contains(e.getPoint())) {
                return tooltipLocations.get(rect);
            }
        }
        
        return super.getToolTipText(e);
    }
}