package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationGeometry;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.InterestingAnnotation;

/**
 * this action opens a dialog in which information on the neurons
 * in the current workspace is displayed
 */
public class WorkspaceInformationAction extends AbstractAction {

    private AnnotationModel annotationModel;

    public WorkspaceInformationAction(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;

        putValue(NAME, "Show workspace information...");
        putValue(SHORT_DESCRIPTION, "Show workspace info");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        if (annotationModel.getCurrentWorkspace() != null) {

            InfoTableModel tableModel = new InfoTableModel();
            tableModel.setAnnotationModel(annotationModel);
            JTable table = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(table);

            table.setFillsViewportHeight(true);
            tableModel.addNeurons(annotationModel.getCurrentWorkspace().getNeuronList());

            JOptionPane.showConfirmDialog(null,
                scrollPane,
                "Workspace information",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null);
        }
    }
}

class InfoTableModel extends AbstractTableModel {

    private  AnnotationModel annotationModel;

    private String[] columnNames = {"Neuron name", "# points", "# branches"};

    private ArrayList<TmNeuron> neurons = new ArrayList<>();

    private int npoints;
    private int nbranches;
    private Map<Long, Integer> branchMap = new HashMap<>();

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }

    public void addNeurons(List<TmNeuron> neuronList) {
        neurons.addAll(neuronList);

        // do pre-calcs
        npoints = 0;
        nbranches = 0;
        branchMap.clear();
        for (TmNeuron neuron: neurons) {
            Long neuronID = neuron.getId();
            npoints += neuron.getGeoAnnotationMap().size();
            branchMap.put(neuron.getId(), 0);
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                    if (ann.isBranch()) {
                        branchMap.put(neuronID, branchMap.get(neuronID) + 1);
                    }
                }
            }
        }

        for (Integer count:branchMap.values()) {
            nbranches += count;
        }

        fireTableDataChanged();
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        // add one for totals at the end
        return neurons.size() + 1;
    }

    public Object getValueAt(int row, int column) {
        if (row == neurons.size()) {
            switch (column) {
                case 0:
                    return "totals: " + neurons.size() + " neurons";
                case 1:
                    return npoints;
                case 2:
                    return nbranches;
                default:
                    return null;
            }
        } else {
            switch (column) {
                case 0:
                    // name
                    return neurons.get(row).getName();
                case 1:
                    // # points
                    return neurons.get(row).getGeoAnnotationMap().size();
                case 2:
                    // # branches
                    return branchMap.get(neurons.get(row).getId());
                default:
                    return null;
            }
        }
    }

}