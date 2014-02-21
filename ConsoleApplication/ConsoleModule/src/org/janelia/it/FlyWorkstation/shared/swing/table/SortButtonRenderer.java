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
package org.janelia.it.FlyWorkstation.shared.swing.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Hashtable;

/**
 * Button used to represent the sort direction of a JTable column.
 *
 * @version $Id: SortButtonRenderer.java,v 1.2 2011/03/08 16:16:49 saffordt Exp $
 * @author Douglas Mason
 */
public class SortButtonRenderer extends JButton implements TableCellRenderer {
  public static final int NONE = 0;
  public static final int DOWN = 1;
  public static final int UP   = 2;

  private int pushedColumn;
  private Hashtable state;
  private JButton downButton, upButton;

  /**
   * Default constructor.  Initializes the Sort Button.
   */
    public SortButtonRenderer() {
        try {
            init();
        } catch (Exception ex) {
            String msg = "Unable to initialize SortButtonRenderer: " + ex;
            System.err.println(msg);
        }
    }

    /**
     * Initializes the renderer button.
     */
    private void init() {
        pushedColumn   = -1;
        state = new Hashtable();

        setMargin(new Insets(0, 0, 0, 0));
        setHorizontalTextPosition(LEFT);
        setIcon(new BlankIcon());

        downButton = new JButton();
        downButton.setMargin(new Insets(0, 0, 0, 0));
        downButton.setHorizontalTextPosition(LEFT);
        downButton.setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));
        downButton.setPressedIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, true));

        upButton = new JButton();
        upButton.setMargin(new Insets(0, 0, 0, 0));
        upButton.setHorizontalTextPosition(LEFT);
        upButton.setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
        upButton.setPressedIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, true));
    }

    /**
     *  Returns the  used for drawing the cell.  This method is
     *  used to configure the renderer appropriately before drawing.
     *
     * @param	table		the <code>JTable</code> that is asking the
     *				renderer to draw; can be <code>null</code>
     * @param	value		the value of the cell to be rendered.  It is
     *				up to the specific renderer to interpret
     *				and draw the value.  For example, if
     *				<code>value</code>
     *				is the string "true", it could be rendered as a
     *				string or it could be rendered as a check
     *				box that is checked.  <code>null</code> is a
     *				valid value
     * @param	isSelected	true if the cell is to be rendered with the
     *				selection highlighted; otherwise false
     * @param	hasFocus	if true, render cell appropriately.  For
     *				example, put a special border on the cell, if
     *				the cell can be edited, render in the color used
     *				to indicate editing
     * @param	row	        the row index of the cell being drawn.  When
     *				drawing the header, the value of
     *				<code>row</code> is -1
     * @param	column	        the column index of the cell being drawn
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                   boolean isSelected, boolean hasFocus, int row, int column) {
        JButton button = this;
        Object obj = state.get(new Integer(column));
        if (obj != null) {
            if (((Integer)obj).intValue() == DOWN) {
                button = downButton;
            } else {
                button = upButton;
            }
        }
        button.setText((value ==null) ? "" : value.toString());
        boolean isPressed = (column == pushedColumn);
        button.getModel().setPressed(isPressed);
        button.getModel().setArmed(isPressed);

        return button;
    }

    /**
    * Sets the pressed column
    *
    * @param col the column index selected
    */
    public void setPressedColumn(int col) {
        pushedColumn = col;
    }

    /**
     * Sets the selected column
     *
     * @param col the column index selected
     */
    public void setSelectedColumn(int col) {
        if (col < 0) {
            return;
        }
        Integer value = null;
        Object obj = state.get(new Integer(col));
        if (obj == null) {
            value = new Integer(DOWN);
        } else {
            if (((Integer)obj).intValue() == DOWN) {
                value = new Integer(UP);
            } else {
                value = new Integer(DOWN);
            }
        }
        state.clear();
        state.put(new Integer(col), value);
    }

    /**
     * Returns the state of the button.
     *
     * @param col the column to run the state
     * @return the state (NONE/DOWN/UP) of the column button
     */
    public int getState(int col) {
        int retValue;
        Object obj = state.get(new Integer(col));
        if (obj == null) {
            retValue = NONE;
        } else {
            if (((Integer)obj).intValue() == DOWN) {
                retValue = DOWN;
            } else {
                retValue = UP;
            }
        }

        return retValue;
    }
}