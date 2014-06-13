/*
 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 Copyright (c) 1999 - 2006 Applera Corporation.
 301 Merritt 7 
 P.O. Box 5435 
 Norwalk, CT 06856-5435 USA

 This is free software; you can redistribute it and/or modify it under the 
 terms of the GNU Lesser General Public License as published by the 
 Free Software Foundation; version 2.1 of the License.

 This software is distributed in the hope that it will be useful, but 
 WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE. 
 See the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License 
 along with this software; if not, write to the Free Software Foundation, Inc.
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
*/
package org.janelia.it.workstation.shared.swing.table;

import javax.swing.*;
import java.awt.*;

/**
 * Produces an Icon representation of an UP/DOWN arrow.
 *
 * @version $Id: BevelArrowIcon.java,v 1.2 2011/03/08 16:16:49 saffordt Exp $
 * @author Douglas Mason
 * @see javax.swing.Icon
 */
public class BevelArrowIcon implements Icon {
    // Arror directions
    public static final int UP    = 1;
    public static final int DOWN  = 2;

    private static final int DEFAULT_SIZE = 11;

    private Color edge1;
    private Color edge2;
    private Color fill;
    private int size;
    private int direction;

    /**
     *  Constructor.
     */
    public BevelArrowIcon(int direction, boolean isRaisedView, boolean isPressedView) {
        if (isRaisedView) {
          if (isPressedView) {
            init( UIManager.getColor("controlLtHighlight"),
                  UIManager.getColor("controlDkShadow"),
                  UIManager.getColor("controlShadow"),
                  DEFAULT_SIZE, direction);
          } else {
            init( UIManager.getColor("controlHighlight"),
                  UIManager.getColor("controlShadow"),
                  UIManager.getColor("control"),
                  DEFAULT_SIZE, direction);
          }
        } else {
          if (isPressedView) {
            init( UIManager.getColor("controlDkShadow"),
                  UIManager.getColor("controlLtHighlight"),
                  UIManager.getColor("controlShadow"),
                  DEFAULT_SIZE, direction);
          } else {
            init( UIManager.getColor("controlShadow"),
                  UIManager.getColor("controlHighlight"),
                  UIManager.getColor("control"),
                  DEFAULT_SIZE, direction);
          }
        }
    }

    /**
     * Constructor.
     */
    public BevelArrowIcon(Color edge1, Color edge2, Color fill,
                          int size, int direction) {
        init(edge1, edge2, fill, size, direction);
    }

    /**
     * Implements the Icon Interface. Draw the arrow icon at the specified location.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        switch (direction) {
            case DOWN:
                drawDownArrow(g, x, y);
                break;
            case   UP:
                drawUpArrow(g, x, y);
                break;
        }
    }

    /**
     * Implements the Icon Interface. Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    public int getIconWidth() {
        return size;
    }

    /**
     * Implements the Icon Interface. Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    public int getIconHeight() {
        return size;
    }

    /**
     * Initializes the BevelArrowIcon attributes.
     */
    private void init(Color edge1, Color edge2, Color fill,
                   int size, int direction) {
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.fill = fill;
        this.size = size;
        this.direction = direction;
    }

    /**
     * Draws the down arrow representation.
     */
    private void drawDownArrow(Graphics g, int xo, int yo) {
        g.setColor(edge1);
        g.drawLine(xo, yo,   xo+size-1, yo);
        g.drawLine(xo, yo+1, xo+size-3, yo+1);
        g.setColor(edge2);
        g.drawLine(xo+size-2, yo+1, xo+size-1, yo+1);
        int x = xo+1;
        int y = yo+2;
        int dx = size-6;
        while (y+1 < yo+size) {
          g.setColor(edge1);
          g.drawLine(x, y,   x+1, y);
          g.drawLine(x, y+1, x+1, y+1);
          if (0 < dx) {
            g.setColor(fill);
            g.drawLine(x+2, y,   x+1+dx, y);
            g.drawLine(x+2, y+1, x+1+dx, y+1);
          }
          g.setColor(edge2);
          g.drawLine(x+dx+2, y,   x+dx+3, y);
          g.drawLine(x+dx+2, y+1, x+dx+3, y+1);
          x += 1;
          y += 2;
          dx -= 2;
        }
        g.setColor(edge1);
        g.drawLine(xo+(size/2), yo+size-1, xo+(size/2), yo+size-1);
    }

    /**
     * Draws the up arrow representation.
     */
    private void drawUpArrow(Graphics g, int xo, int yo) {
        g.setColor(edge1);
        int x = xo+(size/2);
        g.drawLine(x, yo, x, yo);
        x--;
        int y = yo+1;
        int dx = 0;
        while (y+3 < yo+size) {
          g.setColor(edge1);
          g.drawLine(x, y,   x+1, y);
          g.drawLine(x, y+1, x+1, y+1);
          if (0 < dx) {
            g.setColor(fill);
            g.drawLine(x+2, y,   x+1+dx, y);
            g.drawLine(x+2, y+1, x+1+dx, y+1);
          }
          g.setColor(edge2);
          g.drawLine(x+dx+2, y,   x+dx+3, y);
          g.drawLine(x+dx+2, y+1, x+dx+3, y+1);
          x -= 1;
          y += 2;
          dx += 2;
        }
        g.setColor(edge1);
        g.drawLine(xo, yo+size-3,   xo+1, yo+size-3);
        g.setColor(edge2);
        g.drawLine(xo+2, yo+size-2, xo+size-1, yo+size-2);
        g.drawLine(xo, yo+size-1, xo+size, yo+size-1);
    }
}