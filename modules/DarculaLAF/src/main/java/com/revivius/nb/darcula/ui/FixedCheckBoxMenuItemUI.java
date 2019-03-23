package com.revivius.nb.darcula.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;

import com.bulenkov.iconloader.util.GraphicsConfig;
import com.bulenkov.iconloader.util.Gray;
import com.bulenkov.iconloader.util.UIUtil;

import sun.swing.MenuItemLayoutHelper;

/**
 * @author Konstantin Bulenkov
 */
public class FixedCheckBoxMenuItemUI extends FixedMenuItemUIBase {

  public static ComponentUI createUI(JComponent c) {
      return new FixedCheckBoxMenuItemUI();
  }

  protected String getPropertyPrefix() {
      return "CheckBoxMenuItem";
  }

  // Stay open after click
  @Override
  protected void doClick(MenuSelectionManager msm) {
     menuItem.doClick(0);
  }
  
  @Override
  protected void paintCheckIcon(Graphics g2, MenuItemLayoutHelper lh, MenuItemLayoutHelper.LayoutResult lr, Color holdc, Color foreground) {
    Graphics2D g = (Graphics2D) g2;
    final GraphicsConfig config = new GraphicsConfig(g);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

    g.translate(lr.getCheckRect().x+2, lr.getCheckRect().y+2);

    final int sz = 13;
    g.setPaint(new GradientPaint(sz / 2, 1, Gray._110, sz / 2, sz, Gray._95));
    g.fillRoundRect(0, 0, sz, sz - 1 , 4, 4);

    g.setPaint(new GradientPaint(sz / 2, 1, Gray._120.withAlpha(0x5a), sz / 2, sz, Gray._105.withAlpha(90)));
    g.drawRoundRect(0, (UIUtil.isUnderDarcula() ? 1 : 0), sz, sz - 1, 4, 4);

    g.setPaint(Gray._40.withAlpha(180));
    g.drawRoundRect(0, 0, sz, sz - 1, 4, 4);


    if (lh.getMenuItem().isSelected()) {
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
      g.setPaint(Gray._30);
      g.drawLine(4, 7, 7, 10);
      g.drawLine(7, 10, sz, 2);
      g.setPaint(Gray._170);
      g.drawLine(4, 5, 7, 8);
      g.drawLine(7, 8, sz, 0);
    }

    g.translate(-lr.getCheckRect().x-2, -lr.getCheckRect().y-2);
    config.restore();
    g.setColor(foreground);
  }
}
