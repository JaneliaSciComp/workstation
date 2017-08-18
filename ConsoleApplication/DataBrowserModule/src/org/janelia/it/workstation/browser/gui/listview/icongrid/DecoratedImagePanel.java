package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.janelia.it.workstation.browser.model.ImageDecorator;

/**
 * An image with some decorators overlaid on top of it.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DecoratedImagePanel extends JPanel {

    protected final int decoratorOffset = 5;
    protected final int decoratorSpacing = 10;

    protected final Map<Rectangle, String> tooltipLocations = new HashMap<>();
    
    protected final List<ImageDecorator> decorators;
    protected BufferedImage image;
    
    public DecoratedImagePanel(List<ImageDecorator> decorators) {
        this.decorators = decorators;
        ToolTipManager.sharedInstance().registerComponent(this);
        setOpaque(false);
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);       
        paintImage(g);
        paintDecorators(g);
        
//        int textY = decoratorOffset; 
//
//        g.setColor(UIManager.getColor("Label.foreground"));
//        
//        if (!StringUtils.isBlank(title)) {
//            int fontSize = (int) Math.round(image.getWidth() * 0.005) + 10;
//            Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
//            
//            FontMetrics metrics = g.getFontMetrics(titleLabelFont);
//            Rectangle2D stringBounds = metrics.getStringBounds(title, g);
//
//            textY += stringBounds.getHeight();
//            
//            g.setFont(titleLabelFont);
//            g.drawString(title, decoratorOffset, textY);
//            
//        }
//
//        if (!StringUtils.isBlank(subtitle)) {
//            int fontSize = (int) Math.round(image.getWidth() * 0.003) + 10;
//            Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
//
//            FontMetrics metrics = g.getFontMetrics(titleLabelFont);
//            Rectangle2D stringBounds = metrics.getStringBounds(subtitle, g);
//            
//            textY += stringBounds.getHeight();
//            
//            g.setFont(titleLabelFont);
//            g.drawString(subtitle, decoratorOffset, textY);
//        }
    }  
    
    protected void paintImage(Graphics g) {
        if (image!=null) {
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        }
    }

    protected void paintDecorators(Graphics g) {
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
    
    protected void paintDecorator(Graphics g, ImageDecorator imageDecorator, ImageIcon decorator, int x, int y) {
        Rectangle rect = new Rectangle(x, y, decorator.getIconWidth(), decorator.getIconHeight());
        tooltipLocations.put(rect, imageDecorator.getLabel());
        g.drawImage(decorator.getImage(), x, y, decorator.getIconWidth(), decorator.getIconHeight(), null);
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