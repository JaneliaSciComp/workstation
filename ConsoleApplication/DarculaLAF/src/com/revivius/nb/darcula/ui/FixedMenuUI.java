package com.revivius.nb.darcula.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

import sun.swing.MenuItemLayoutHelper;
import sun.swing.SwingUtilities2;

/**
 * The Darcula UI doesn't indent non-radio-buttons enough. 
 * 
 * This fix is not NetBeans or Workstation specific and could be merged into Darcula.
 */
public class FixedMenuUI extends BasicMenuUI {

    public static ComponentUI createUI(JComponent c) {
        return new FixedMenuUI();
    }
    
    @Override
    protected void paintMenuItem(Graphics g, JComponent c,
                                     Icon checkIcon, Icon arrowIcon,
                                     Color background, Color foreground,
                                     int defaultTextIconGap) {
      // Save original graphics font and color
      Font holdf = g.getFont();
      Color holdc = g.getColor();

      JMenuItem mi = (JMenuItem) c;
      g.setFont(mi.getFont());

      Rectangle viewRect = new Rectangle(0, 0, mi.getWidth(), mi.getHeight());
      // KR: commenting this out because it does not render correctly
//      applyInsets(viewRect, mi.getInsets());
      
      MenuItemLayoutHelper lh = new MenuItemLayoutHelper(mi, checkIcon,
          arrowIcon, viewRect, defaultTextIconGap, acceleratorDelimiter,
          mi.getComponentOrientation().isLeftToRight(), mi.getFont(),
          acceleratorFont, MenuItemLayoutHelper.useCheckAndArrow(menuItem),
          getPropertyPrefix());
      MenuItemLayoutHelper.LayoutResult lr = lh.layoutMenuItem();

      paintBackground(g, mi, background);
      paintCheckIcon(g, lh, lr, holdc, foreground);
      paintIcon(g, lh, lr, holdc);
      g.setColor(foreground);
      paintText(g, lh, lr);
      paintAccText(g, lh, lr);
      paintArrowIcon(g, lh, lr, foreground);

      // Restore original graphics font and color
      g.setColor(holdc);
      g.setFont(holdf);
    }

    protected void paintIcon(Graphics g, MenuItemLayoutHelper lh,
                           MenuItemLayoutHelper.LayoutResult lr, Color holdc) {
        if (lh.getIcon() != null) {
            Icon icon;
            ButtonModel model = lh.getMenuItem().getModel();
            if (!model.isEnabled()) {
                icon = lh.getMenuItem().getDisabledIcon();
            } else if (model.isPressed() && model.isArmed()) {
                icon = lh.getMenuItem().getPressedIcon();
                if (icon == null) {
                    // Use default icon
                    icon = lh.getMenuItem().getIcon();
                }
            } else {
                icon = lh.getMenuItem().getIcon();
            }

            if (icon != null) {
                icon.paintIcon(lh.getMenuItem(), g, lr.getIconRect().x,
                        lr.getIconRect().y);
                g.setColor(holdc);
            }
        }
    }

    protected void paintCheckIcon(Graphics g, MenuItemLayoutHelper lh,
                                MenuItemLayoutHelper.LayoutResult lr,
                                Color holdc, Color foreground) {
        if (lh.getCheckIcon() != null) {
            ButtonModel model = lh.getMenuItem().getModel();
            if (model.isArmed() || (lh.getMenuItem() instanceof JMenu
                    && model.isSelected())) {
                g.setColor(foreground);
            } else {
                g.setColor(holdc);
            }
            if (lh.useCheckAndArrow()) {
                lh.getCheckIcon().paintIcon(lh.getMenuItem(), g,
                        lr.getCheckRect().x, lr.getCheckRect().y);
            }
            g.setColor(holdc);
        }
    }
    
    protected void paintAccText(Graphics g, MenuItemLayoutHelper lh,
                                MenuItemLayoutHelper.LayoutResult lr) {
      if (!lh.getAccText().equals("")) {
        ButtonModel model = lh.getMenuItem().getModel();
        g.setFont(lh.getAccFontMetrics().getFont());
        if (!model.isEnabled()) {
          // *** paint the accText disabled
          if (disabledForeground != null) {
            g.setColor(disabledForeground);
            SwingUtilities2.drawString(lh.getMenuItem(), g,
                lh.getAccText(), lr.getAccRect().x,
                lr.getAccRect().y + lh.getAccFontMetrics().getAscent());
          } else {
            g.setColor(lh.getMenuItem().getBackground().brighter());
            SwingUtilities2.drawString(lh.getMenuItem(), g,
                lh.getAccText(), lr.getAccRect().x,
                lr.getAccRect().y + lh.getAccFontMetrics().getAscent());
            g.setColor(lh.getMenuItem().getBackground().darker());
            SwingUtilities2.drawString(lh.getMenuItem(), g,
                lh.getAccText(), lr.getAccRect().x - 1,
                lr.getAccRect().y + lh.getFontMetrics().getAscent() - 1);
          }
        } else {
          // *** paint the accText normally
          if (model.isArmed()
              || (lh.getMenuItem() instanceof JMenu
              && model.isSelected())) {
            g.setColor(acceleratorSelectionForeground);
          } else {
            g.setColor(acceleratorForeground);
          }
          SwingUtilities2.drawString(lh.getMenuItem(), g, lh.getAccText(),
              lr.getAccRect().x, lr.getAccRect().y +
                  lh.getAccFontMetrics().getAscent());
        }
      }
    }

    protected void paintText(Graphics g, MenuItemLayoutHelper lh,
                           MenuItemLayoutHelper.LayoutResult lr) {
        if (!lh.getText().equals("")) {
            if (lh.getHtmlView() != null) {
                // Text is HTML
                lh.getHtmlView().paint(g, lr.getTextRect());
            } else {
                // Text isn't HTML
                // KR: fix menu text color
                g.setColor(menuItem.getForeground());
                paintText(g, lh.getMenuItem(), lr.getTextRect(), lh.getText());
            }
        }
    }
    
    protected void paintArrowIcon(Graphics g, MenuItemLayoutHelper lh,
                                MenuItemLayoutHelper.LayoutResult lr,
                                Color foreground) {
        if (lh.getArrowIcon() != null) {
            ButtonModel model = lh.getMenuItem().getModel();
            if (model.isArmed() || (lh.getMenuItem() instanceof JMenu
                                && model.isSelected())) {
                g.setColor(foreground);
            }
            if (lh.useCheckAndArrow()) {
                lh.getArrowIcon().paintIcon(lh.getMenuItem(), g,
                        lr.getArrowRect().x, lr.getArrowRect().y);
            }
        }
    }

    protected void applyInsets(Rectangle rect, Insets insets) {
        if(insets != null) {
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= (insets.right + rect.x);
            rect.height -= (insets.bottom + rect.y);
        }
    }
}
