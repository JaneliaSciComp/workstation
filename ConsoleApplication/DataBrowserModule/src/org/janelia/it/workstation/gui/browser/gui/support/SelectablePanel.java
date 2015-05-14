package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;

/**
 * A panel that can be selected and displays a border when selected. The 
 * mouse handling is not done by this class, it requires you to call 
 * setSelected() whenever the panel is selected or deselected.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SelectablePanel extends JPanel {
    
    public static final int BORDER_WIDTH = 10;
    
    private static BufferedImage normalBorderImage;
    private static BufferedImage selectedBorderImage;
    private static Color normalBackground;
    private static Color selectedBackground;
    
    static {
        // TODO: this should be done whenever the L&F changes, but currently we need to restart anyway, 
        // so it doesn't matter until that is fixed

        String normalBorder = "border_normal.png";
        String selectedBorder = "border_selected.png";
        normalBackground = new Color(241, 241, 241);
        selectedBackground = new Color(203, 203, 203);

        if (SessionMgr.getSessionMgr().isDarkLook()) {
            normalBorder = "border_dark_normal.png";
            selectedBorder = "border_dark_selected.png";
            normalBackground = null;
            selectedBackground = null;
        }

        try {
            normalBorderImage = Utils.toBufferedImage(Utils.getClasspathImage(normalBorder).getImage());
            selectedBorderImage = Utils.toBufferedImage(Utils.getClasspathImage(selectedBorder).getImage());
        }
        catch (FileNotFoundException e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    private boolean selected;
    
    public SelectablePanel() {
        normalBackground = getBackground();
        setBackground(normalBackground);   
    }

    /**
     * Returns true if the panel is currently selected.
     * @return 
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Change the selection state and repaint the panel.
     * @param selected 
     */
    public void setSelected(boolean selected) {
        this.selected = selected;

        if (selected) {
            if (selectedBackground != null) {
                setBackground(selectedBackground);
            }
        }
        else {
            if (normalBackground != null) {
                setBackground(normalBackground);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        BufferedImage borderImage = selected ? selectedBorderImage : normalBorderImage;
        if (borderImage == null) {
            return;
        }

        int b = BORDER_WIDTH; // border width
        int w = getWidth();
        int h = getHeight();
        int iw = borderImage.getWidth();
        int ih = borderImage.getHeight();

        g.drawImage(borderImage, 0, 0, b, b, 0, 0, b, b, null); // top left
        g.drawImage(borderImage, w - b, 0, w, b, iw - b, 0, iw, b, null); // top right
        g.drawImage(borderImage, 0, h - b, b, h, 0, ih - b, b, ih, null); // bottom right
        g.drawImage(borderImage, w - b, h - b, w, h, iw - b, ih - b, iw, ih, null); // bottom left

        g.drawImage(borderImage, b, 0, w - b, b, b, 0, iw - b, b, null); // top
        g.drawImage(borderImage, 0, b, b, h - b, 0, b, b, ih - b, null); // left
        g.drawImage(borderImage, b, h - b, w - b, h, b, ih - b, iw - b, ih, null); // bottom
        g.drawImage(borderImage, w - b, b, w, h - b, iw - b, b, iw, ih - b, null); // right
    }
}
