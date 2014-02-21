package org.janelia.it.FlyWorkstation.gui.framework.table;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Renders a table cell as a progress bar. Extend this class and implement getValueAtRowIndex(int rowIndex) to 
 * provide the progress values.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ProgressCellRenderer extends JProgressBar implements TableCellRenderer {

	public ProgressCellRenderer() {
		setPreferredSize(new Dimension(100, 16));
		setMinimum(0);
		setMaximum(100);
		setIndeterminate(false);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int rowIndex, int colIndex) {
		setValue(getValueAtRowIndex(rowIndex));
		return this;
	}
	
	protected abstract int getValueAtRowIndex(int rowIndex);
}