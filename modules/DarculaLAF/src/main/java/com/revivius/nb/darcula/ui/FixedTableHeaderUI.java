package com.revivius.nb.darcula.ui;

import com.bulenkov.iconloader.util.ColorUtil;
import com.bulenkov.iconloader.util.GraphicsConfig;
import com.bulenkov.iconloader.util.Gray;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Enumeration;

/**
 * KR: Fixed: last header cell does not have a right border, making it difficult to resize 
 * 
 * @author Konstantin Bulenkov
 */
public class FixedTableHeaderUI extends BasicTableHeaderUI {

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new FixedTableHeaderUI();
  }

  @Override
  public void paint(Graphics g2, JComponent c) {
    final Graphics2D g = (Graphics2D)g2;
    final GraphicsConfig config = new GraphicsConfig(g);
    final Color bg = c.getBackground();
    g.setPaint(new GradientPaint(0, 0, ColorUtil.shift(bg, 1.4), 0, c.getHeight(), ColorUtil.shift(bg, 0.9)));
    final int h = c.getHeight();
    final int w = c.getWidth();
    g.fillRect(0,0, w, h);
    g.setPaint(ColorUtil.shift(bg, 0.75));
    g.drawLine(0, h-1, w, h-1);
    g.drawLine(w-1, 0, w-1, h-1);

    final Enumeration<TableColumn> columns = ((JTableHeader)c).getColumnModel().getColumns();

    final Color lineColor = ColorUtil.shift(bg, 0.7);
    final Color shadow = Gray._255.withAlpha(30);
    int offset = 0;
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      // Fixed:
//      if (columns.hasMoreElements() && column.getWidth() > 0) {
      if (column.getWidth() > 0) {
        offset += column.getWidth();
        g.setColor(lineColor);
        g.drawLine(offset-1, 1, offset-1, h-3);
        g.setColor(shadow);
        g.drawLine(offset, 1, offset, h-3);
      }
    }

    config.restore();

    super.paint(g, c);
  }
}
