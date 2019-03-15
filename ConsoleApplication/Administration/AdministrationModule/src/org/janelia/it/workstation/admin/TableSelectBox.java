package org.janelia.it.workstation.admin;

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author schauderd
 */
public class TableSelectBox extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    JComboBox selectBox;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        selectBox = (JComboBox) value;
        return selectBox;
    }

    @Override
    public Object getCellEditorValue() {
        return selectBox.getSelectedItem();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return (JComboBox) value;
    }

}
