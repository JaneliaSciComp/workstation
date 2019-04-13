package org.janelia.workstation.admin;

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author schauderd
 */
public class TableButton extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    JButton button;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        button = (JButton) value;
        return button;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        button = (JButton) value;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return button;
    }

}
