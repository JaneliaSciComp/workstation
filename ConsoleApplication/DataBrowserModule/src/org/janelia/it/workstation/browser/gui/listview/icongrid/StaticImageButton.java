package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.janelia.it.workstation.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.model.ImageDecorator;

/**
 * An AnnotatedImageButton with a static UNKNOWN_ICON or label.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StaticImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private static final float ICON_OPACITY = 1.0f;
    
    private JComponent infoPanel;

    private List<ImageDecorator> decorators;

    public StaticImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel, String filepath) {
        super(imageObject, imageModel, selectionModel, imagesPanel, filepath);
        this.decorators = imageModel.getDecorators(imageObject);
    }

    @Override
    public JComponent init(T imageObject, ImageModel<T,S> imageModel, String filepath) {
        this.infoPanel = new DecoratedInfoPanel();
        infoPanel.addMouseListener(new MouseForwarder(this, "DecoratedInfoPanel->StaticImageButton"));
        return infoPanel;
    }

    @Override
    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        infoPanel.setPreferredSize(new Dimension(width, height));
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    @Override
    public void setViewable(boolean viewable) {
    }

    class DecoratedInfoPanel extends JPanel {
        
        public DecoratedInfoPanel() {
            ToolTipManager.sharedInstance().registerComponent(this);
            setOpaque(false);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);       

            int decoratorOffset = 5;
            int decoratorSpacing = 10;
            
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

            Rectangle viewRect = new Rectangle(0, 0, getWidth(), getHeight());
            
            // Draw a border
//            g.setColor(UIManager.getColor("ws.ComponentBorderColor"));
//            g.drawRect(viewRect.x, viewRect.y, viewRect.width-1, viewRect.height-1);
           
            // Draw label
            String title = "Selected result type not available";
        
            int fontSize = (int) Math.round(getWidth() * 0.005) + 10;
            Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
            
            FontMetrics metrics = g.getFontMetrics(titleLabelFont);
            
            Rectangle iconR = new Rectangle();
            Rectangle textR = new Rectangle();
            
            String clippedLabel = SwingUtilities.layoutCompoundLabel(
                    this, 
                    metrics, 
                    title,
                    null,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    SwingConstants.CENTER,
                    viewRect,
                    iconR,
                    textR,
                    0);
            
            BufferedImage image = Icons.getImage("file_missing.png");
            int yLimit = textR.y - metrics.getHeight();
            int iconX = (viewRect.width - image.getWidth()) / 2;
            int iconY = (viewRect.height - image.getHeight()) / 2;
            
            if (image.getHeight() < yLimit) {
                // Both icon and label fit. Draw the label first.
                g.setFont(titleLabelFont);  
                g.setColor(UIManager.getColor("Label.disabledForeground"));
                g.drawString(clippedLabel, textR.x, textR.y);
                // Put the icon above the text
                iconY = yLimit - image.getHeight();
                // Tooltip on the text
                tooltipLocations.put(textR, title);
            }
            else {
                // Tooltip everywhere
                tooltipLocations.put(viewRect, title);
            }
            
            // Draw the icon with lowered opacity
            Graphics2D g2 = (Graphics2D)g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ICON_OPACITY));
            g2.drawImage(image, iconX, iconY, image.getWidth(), image.getHeight(), null);
        }  
        
        private void paintDecorator(Graphics g, ImageDecorator imageDecorator, ImageIcon decorator, int x, int y) {
            Rectangle rect = new Rectangle(x, y, decorator.getIconWidth(), decorator.getIconHeight());
            tooltipLocations.put(rect, imageDecorator.getLabel());
            g.drawImage(decorator.getImage(), x, y, decorator.getIconWidth(), decorator.getIconHeight(), null);
        }

        private Map<Rectangle, String> tooltipLocations = new HashMap<>();
        
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
}
