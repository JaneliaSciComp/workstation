package org.janelia.workstation.controller.model.annotations.neuron;

import java.util.ArrayList;
import java.util.Date;
import javax.swing.table.AbstractTableModel;

/**
 * Table model to support encapsulation/presentation of interesting annotations.
 *
 * @author olbrisd
 */
public class FilteredAnnotationModel extends AbstractTableModel {

    private String[] columnNames = {"date", "geo", "note"};

    private ArrayList<InterestingAnnotation> annotations = new ArrayList<>();

    public void clear() {
        annotations = new ArrayList<>();
    }

    public void addAnnotation(InterestingAnnotation ann) {
        annotations.add(ann);
    }

    // boilerplate stuff
    @Override
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
                return annotations.get(row).getModificationDate();
            case 1:
                return annotations.get(row).getGeometry();
            case 2:
                return annotations.get(row).getNoteText();
            default:
                return null;
        }

    }

    // find annotation based on ID; returns its row or -1 if not found
    public int findAnnotation(InterestingAnnotation ann) {
        for (int row = 0; row < annotations.size(); row++)
            if (ann.getAnnotationID().equals(annotations.get(row).getAnnotationID())) {
                return row;
        }
        return -1;
    }

    // this needs to be done to get Date column to sort right
    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                // date
                return Date.class;
            case 1:
                // geometry
                return AnnotationGeometry.class;
            case 2:
                // note string
                return String.class;
            default:
                return Object.class;
        }
    }

}
