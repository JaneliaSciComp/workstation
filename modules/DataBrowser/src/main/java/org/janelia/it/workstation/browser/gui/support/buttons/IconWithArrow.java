package org.janelia.it.workstation.browser.gui.support.buttons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;

import org.openide.util.ImageUtilities;

/**
 * An icon that paints a small arrow to the right of the provided icon.
 * 
 * @author S. Aubrecht
 * @since 6.11
 */
class IconWithArrow implements Icon {

    private static final String ARROW_IMAGE_NAME = "org/openide/awt/resources/arrow.png"; // NOI18N

    private Icon orig;
    private Icon arrow = ImageUtilities.loadImageIcon(ARROW_IMAGE_NAME, false);
    private boolean paintRollOver;

    private static final int GAP = 6;

    /** Creates a new instance of IconWithArrow */
    public IconWithArrow(Icon orig, boolean paintRollOver) {
        this.orig = orig;
        this.paintRollOver = paintRollOver;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int height = getIconHeight();
        if (orig!=null) {
            orig.paintIcon(c, g, x, y + (height - orig.getIconHeight()) / 2);
        }

        int fx = orig==null ? x : x + GAP + orig.getIconWidth();
        int fy = y + (height - arrow.getIconHeight()) / 2;
        arrow.paintIcon(c, g, fx, fy);

        if (paintRollOver) {
            Color brighter = UIManager.getColor("controlHighlight"); // NOI18N
            Color darker = UIManager.getColor("controlShadow"); // NOI18N
            if (null == brighter || null == darker) {
                brighter = c.getBackground().brighter();
                darker = c.getBackground().darker();
            }
            if (null != brighter && null != darker) {
                int ax = orig==null ? x : x+orig.getIconWidth();
                g.setColor(brighter);
                g.drawLine(ax + 1, y, ax + 1, y + getIconHeight());
                g.setColor(darker);
                g.drawLine(ax + 2, y, ax + 2, y + getIconHeight());
            }
        }
    }

    @Override
    public int getIconWidth() {
        if (orig==null) {
            return arrow.getIconWidth();
        }
        return orig.getIconWidth() + GAP + arrow.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        if (orig==null) {
            return arrow.getIconHeight();
        }
        return Math.max(orig.getIconHeight(), arrow.getIconHeight());
    }
}
