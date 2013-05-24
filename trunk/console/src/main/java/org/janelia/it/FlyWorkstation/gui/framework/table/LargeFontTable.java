package org.janelia.it.FlyWorkstation.gui.framework.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Renders a JTable with a larger default font.
 * Code borrowed from: @link(http://www.codeguru.com/forum/showthread.php?t=36979)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LargeFontTable extends JTable {

	private Font customFont;
	private int customRowHeight;

	public LargeFontTable(Font customFont) {
		
		this.customFont = customFont;
		
		if (customFont!=null) {
			// Get the actual height of the custom font.
			FontMetrics metrics = getFontMetrics(customFont);
			customRowHeight = metrics.getHeight();
			
			// Set table row height to match font height.
			setRowHeight(customRowHeight);
		}
	}

	/**
	 * Override <tt>getRowHeight()</tt> so that look-and-feels which do not use
	 * the default table cell renderer (for example Substance) use the desired
	 * row height.
	 */
	@Override
	public int getRowHeight() {
		if (customRowHeight > 0) {
			return customRowHeight;
		}
		return super.getRowHeight();
	}

	/**
	 * Override <tt>prepareRenderer()</tt> to set the font size in the instances
	 * of the cell renderers returned by the look-and-feel's renderer.
	 * <p>
	 * Extending <tt>DefaultTableCellRenderer</tt>, setting the font in method
	 * <tt>getTableCellRendererComponent()</tt>, and setting the columns to use
	 * the custom renderer does work with the built-in look-and-feels. It also
	 * works sufficiently with other look-and-feels, but some of these normally
	 * supply their own table cell renderers, and setting a custom renderer
	 * means that some of the functionality of the look-and-feel's renderer is
	 * lost. For example, with Substance, one loses the different backgrounds
	 * for alternate rows.
	 * <p>
	 * By overriding the table's prepareRenderer() method instead, the
	 * functionality of the look-and-feel's renderer is retained.
	 */
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);
		
		if (customFont != null) {
			c.setFont(customFont);
		}
		
		// For labels we can also fix the size information
		if (renderer instanceof JLabel) {
			if (customFont!=null) {
				FontMetrics metrics = getFontMetrics(customFont);
				JLabel dtcr = (JLabel)renderer;
				int width = metrics.stringWidth(dtcr.getText());
				c.setPreferredSize(new Dimension(width, getRowHeight()));
			}
		}
		
		return c;
	}
}