package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import Jama.Matrix;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.swc.MatrixDrivenSWCExchanger;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.util.MatrixUtilities;

/**
 * this action opens a dialog in which information on the neurons
 * in the current workspace is displayed; currently we limit to the
 * neurons visible in the neuron list
 */
public class WorkspaceInformationAction extends AbstractAction {

    private AnnotationModel annotationModel;
    private NeuronListProvider listProvider;

    public WorkspaceInformationAction(AnnotationModel annotationModel, NeuronListProvider listProvider) {
        this.annotationModel = annotationModel;
        this.listProvider = listProvider;

        putValue(NAME, "Show neuron table...");
        putValue(SHORT_DESCRIPTION, "Show neuron table");
    }


    @Override
    public void actionPerformed(ActionEvent action) {
        if (annotationModel.getCurrentWorkspace() != null) {

            final InfoTableModel tableModel = new InfoTableModel();
            tableModel.setAnnotationModel(annotationModel);
            JTable table = new JTable(tableModel) {
                public String getToolTipText(MouseEvent event) {
                    String tip = null;
                    java.awt.Point p = event.getPoint();
                    int rowIndex = rowAtPoint(p);
                    if (rowIndex >= 0) {
                        int colIndex = columnAtPoint(p);
                        int realColumnIndex = convertColumnIndexToModel(colIndex);
                        int realRowIndex = convertRowIndexToModel(rowIndex);
                        if (realColumnIndex == 0) {
                            tip = (String) tableModel.getValueAt(realRowIndex, realColumnIndex);
                        }
                        return tip;
                    } else {
                        // off visible rows, returns null = no tip
                        return tip;
                    }
                }
            };
            JScrollPane scrollPane = new JScrollPane(table);

            table.setFillsViewportHeight(true);
            table.setAutoCreateRowSorter(true);
            tableModel.addNeurons(new ArrayList<>(listProvider.getNeuronList()));

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.add(scrollPane);
            panel.add(new JLabel("                totals:        " + tableModel.getRowCount() + " neurons        "
                + tableModel.getNpoints()+ " points        " + tableModel.getNbranches() + " branches        "
                + String.format("%.1f", tableModel.getTotalLength() / 1000.0) + " length (mm)"));

            JOptionPane.showConfirmDialog(null,
                // scrollPane,
                panel,
                "Data for filtered neurons",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null);
        }
    }
}

class InfoTableModel extends AbstractTableModel {

    private AnnotationModel annotationModel;

    private String[] columnNames = {"Neuron name", "# points", "# branches", "length (mm)"};

    private ArrayList<TmNeuronMetadata> neurons = new ArrayList<>();

    private int npoints;
    private int nbranches;
    private double totalLength;
    private Map<Long, Integer> branchMap = new HashMap<>();
    private Map<Long, Double> lengthMap = new HashMap<>();

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }

    public void addNeurons(List<TmNeuronMetadata> neuronList) {
        neurons.addAll(neuronList);

        // get ready for length calculation
        Matrix voxToMicronMatrix = MatrixUtilities.deserializeMatrix(annotationModel.getCurrentSample().getVoxToMicronMatrix(), "voxToMicronMatrix");
        Matrix micronToVoxMatrix = MatrixUtilities.deserializeMatrix(annotationModel.getCurrentSample().getMicronToVoxMatrix(), "micronToVoxMatrix");
        MatrixDrivenSWCExchanger exchanger = new MatrixDrivenSWCExchanger(micronToVoxMatrix, voxToMicronMatrix);

        // do pre-calcs
        npoints = 0;
        nbranches = 0;
        totalLength = 0.0;
        branchMap.clear();
        lengthMap.clear();
        for (TmNeuronMetadata neuron: neurons) {
            Long neuronID = neuron.getId();
            npoints += neuron.getGeoAnnotationMap().size();
            int nBranches = 0;
            double length = 0.0;
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                    // branch counting
                    if (ann.isBranch()) {
                        nBranches++;
                    }
                    // length calculation
                    if  (!ann.isRoot()) {
                        length += distance(ann, neuron.getParentOf(ann), exchanger);
                    }
                }
            }
            branchMap.put(neuronID, nBranches);
            lengthMap.put(neuronID, length);
        }

        for (Integer count: branchMap.values()) {
            nbranches += count;
        }
        for (Double length: lengthMap.values()) {
            totalLength += length;
        }

        fireTableDataChanged();
    }

    public int getNpoints() {
        return npoints;
    }

    public int getNbranches() {
        return nbranches;
    }

    public double getTotalLength() {
        return totalLength;
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return neurons.size();
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                // neuron name
                return String.class;
            case 1:
                // # points
                return Integer.class;
            case 2:
                // # branches
                return Integer.class;
            case 3:
                // length
                return Double.class;
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
        }
    }

    public Object getValueAt(int row, int column) {
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
            case 3:
                // length
                return lengthMap.get(neurons.get(row).getId()) / 1000.0;
            default:
                return null;
        }
    }

    private double distance(TmGeoAnnotation ann1, TmGeoAnnotation ann2, MatrixDrivenSWCExchanger exchanger) {
        double[] v1e = exchanger.getExternal(new double[] {ann1.getX(), ann1.getY(), ann1.getZ()});
        Vec3 v1 = new Vec3(v1e[0], v1e[1], v1e[2]);
        double[] v2e = exchanger.getExternal(new double[] {ann2.getX(), ann2.getY(), ann2.getZ()});
        Vec3 v2 = new Vec3(v2e[0], v2e[1], v2e[2]);
        return v1.minus(v2).norm();
    }
}