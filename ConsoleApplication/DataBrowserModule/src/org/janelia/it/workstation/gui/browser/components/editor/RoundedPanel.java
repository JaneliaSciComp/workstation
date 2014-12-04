package org.janelia.it.workstation.gui.browser.components.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.util.MouseHandler;

/**
 * A panel with a label and a rounded panel which supports mouse interaction.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class RoundedPanel extends JPanel {

    public static final int DEFAULT_CORNER_RADIUS = 10;
    public static final int MAX_WIDTH = 120;
    public static final Font DEFAULT_LABEL_FONT = new Font("Sans Serif", Font.BOLD, 12);
    
    private int cornerRadius = DEFAULT_CORNER_RADIUS;
    private final Font labelFont = DEFAULT_LABEL_FONT;
    private Color borderColor;
    private JLabel label;
    
    public RoundedPanel() {
        
        this.label = new JLabel() {
            @Override
            public String getText() {
                return RoundedPanel.this.getLabel();
            }
        };
        label.setFont(labelFont);
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
        
        addMouseListener(new MouseHandler() {

            Color normalColor;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (normalColor==null) {
                    normalColor = getBackground();
                }
                super.mouseEntered(e);
                setBorderColor(Color.white);
                setBackground(getBackground().brighter());
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                setBorderColor(getForeground());
                setBackground(normalColor);
                repaint();
            }

            @Override
            protected void popupTriggered(MouseEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                showPopupMenu(e);
            }
            
            protected void doubleLeftClicked(MouseEvent e) {
                doubleClicked(e);
            }
        });
    }
    
    // From http://stackoverflow.com/questions/15025092/border-with-rounded-corners-transparency
    @Override
    protected final void paintComponent(Graphics g) {
        
        FontMetrics fontMetrics = getGraphics().getFontMetrics(labelFont);
        int labelWidth = fontMetrics.stringWidth(label.getText());
        if (labelWidth>MAX_WIDTH) {
            labelWidth = MAX_WIDTH;
        }
        int labelHeight = fontMetrics.getHeight();
        label.setPreferredSize(new Dimension(labelWidth, labelHeight));
        
        if (borderColor==null) {
            borderColor = getForeground();
        }
        super.paintComponent(g);
        Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //Draws the rounded panel with borders.
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
        graphics.setColor(borderColor);
        graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
    }

    /**
     * Override this to provide an action that occurs when the panel is
     * right-clicked by the user.
     */
    protected void showPopupMenu(MouseEvent e) {
    }

    /**
     * Override this to provide an action that occurs when the panel is double
     * clicked by the user.
     */
    protected void doubleClicked(MouseEvent e) {
    }
    
    /**
     * Override this method to provide the label to be displayed in the middle
     * of the panel.
     */
    protected abstract String getLabel();
    
    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }
}
