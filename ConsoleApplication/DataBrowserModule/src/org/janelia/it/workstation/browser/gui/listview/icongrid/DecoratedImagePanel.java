package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.janelia.it.workstation.browser.model.ImageDecorator;

/**
 * An image with some decorators overlaid on top of it.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DecoratedImagePanel extends JPanel {

    private static final float ICON_OPACITY = 1.0f;
    
    private final int decoratorOffset = 5;
    private final int decoratorSpacing = 10;
    private final int padding = 5;
    private final int textSpacing = 5;

    private final Map<Rectangle, String> tooltipLocations = new HashMap<>();

    private BufferedImage image;
    private final List<ImageDecorator> decorators;
    private String text;
    private Color fontColor;

    public DecoratedImagePanel(BufferedImage image, List<ImageDecorator> decorators) {
        this(image, decorators, null, null);
    }
    
    public DecoratedImagePanel(BufferedImage image, List<ImageDecorator> decorators, String text) {
        this(image, decorators, text, null);
    }
    
    public DecoratedImagePanel(BufferedImage image, List<ImageDecorator> decorators, String text, Color fontColor) {
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
    
    public void setText(String text, Color fontColor) {
        this.text = text;
        this.fontColor = fontColor;
        updatePreferredSize();
    }

    private void updatePreferredSize() {

        int w = 100;
        int h = 100;
        
        if (image!=null) {
            w = image.getWidth();
            h = image.getHeight();
        }

        if (text!=null) {

            int fontSize = (int) Math.round(w * 0.005) + 10;
            Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);

            Rectangle viewRect = new Rectangle(0, 0, w, h);
            Rectangle iconR = new Rectangle();
            Rectangle textR = new Rectangle();
            String clippedLabel = null;
            if (text!=null) {
                clippedLabel = SwingUtilities.layoutCompoundLabel(
                        this, 
                        getFontMetrics(titleLabelFont), 
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
            }
            
            // TODO: calculate this
            if (textR.width > w) w = textR.width;
            h += textR.height + textSpacing;
        }
        
        setPreferredSize(new Dimension(w, h));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (text==null) {
            paintImage(g);
        }
        else {
            paintImageWithText(g);
        }
        paintDecorators(g);
        paintTitles(g);
    }  
    
    private void paintImage(Graphics g) {
        if (image!=null) {
            Rectangle viewRect = new Rectangle(0, 0, getWidth(), getHeight());
            int imageX = (viewRect.width - image.getWidth()) / 2;
            // For now, force align all images to top because it looks better. 
            // This should be made into an alignment setting.
            int imageY = 0;//(viewRect.height - image.getHeight()) / 2;
            g.drawImage(image, imageX, imageY, image.getWidth(), image.getHeight(), null);
        }
    }

    private void paintImageWithText(Graphics g) {
        
        Rectangle viewRect = new Rectangle(0, 0, getWidth(), getHeight());
        
        int fontSize = (int) Math.round(getWidth() * 0.005) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        String clippedLabel = null;
        if (text!=null) {
            clippedLabel = SwingUtilities.layoutCompoundLabel(
                    this, 
                    getFontMetrics(titleLabelFont), 
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
        }
        
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
            
            // Draw the icon 
            Graphics2D g2 = (Graphics2D)g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ICON_OPACITY));
            g2.drawImage(image, imageX, imageY, image.getWidth(), image.getHeight(), null);
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

    private void paintDecorators(Graphics g) {
        if (decorators!=null && !decorators.isEmpty()) {
            
            int totalWidth = (int)getSize().getWidth();
            int x = totalWidth-decoratorOffset;
            
            for (ImageDecorator imageDecorator : decorators) {
                ImageIcon icon = imageDecorator.getIcon();
                x -= icon.getIconWidth();
                paintDecorator(g, imageDecorator, icon, x, decoratorOffset);
                x -= decoratorSpacing;
            }
        }
    }
    
    private void paintDecorator(Graphics g, ImageDecorator imageDecorator, ImageIcon decorator, int x, int y) {
        Rectangle rect = new Rectangle(x, y, decorator.getIconWidth(), decorator.getIconHeight());
        tooltipLocations.put(rect, imageDecorator.getLabel());
        g.drawImage(decorator.getImage(), x, y, decorator.getIconWidth(), decorator.getIconHeight(), null);
    }
    
    private void paintTitles(Graphics g) {

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