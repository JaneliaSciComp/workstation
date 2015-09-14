package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 * Table model to support encapsulation/presentation of interesting annotations.
 *
 * @author olbrisd
 */
public class FilteredAnnotationModel extends AbstractTableModel {

    // private String[] columnNames = {"ID", "geo", "note"};
    private String[] columnNames = {"date", "geo", "note"};

    private ArrayList<InterestingAnnotation> annotations = new ArrayList<>();

    public void clear() {
        annotations = new ArrayList<>();
    }

    public void addAnnotation(InterestingAnnotation ann) {
        annotations.add(ann);
    }

    // boilerplate stuff
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return annotations.size();
    }

    public InterestingAnnotation getAnnotationAtRow(int row) {
        return annotations.get(row);
    }

    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return annotations.get(row).getCreationDate();
            case 1:
                return annotations.get(row).getGeometryText();
            case 2:
                return annotations.get(row).getNoteText();
            default:
                return null;
        }

    }
}
