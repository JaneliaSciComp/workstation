package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NeuronSelectedListener;

/**
 * this widget displays a list of neurons in a workspace
 *
 * djo, 12/13
 */
public class WorkspaceNeuronList extends JPanel {

    private JTable neuronTable;
    private NeuronTableModel neuronTableModel;
    private DefaultRowSorter<TableModel, String> sorter;
    private AnnotationManager annotationManager;
    private AnnotationModel annotationModel;
    private CameraPanToListener panListener;
    private NeuronSelectedListener neuronSelectedListener;

    private int width;
    private static final int height = AnnotationPanel.SUBPANEL_STD_HEIGHT;

    /**
     * @param neuronSelectedListener the neuronSelectedListener to set
     */
    public void setNeuronSelectedListener(NeuronSelectedListener neuronSelectedListener) {
        this.neuronSelectedListener = neuronSelectedListener;
    }

    // to add new sort order: add to enum here, add menu in AnnotationPanel.java,
    //  and implement the sort in sortOrderChanged, below
    public enum NeuronSortOrder {ALPHABETICAL, CREATIONDATE};
    private NeuronSortOrder neuronSortOrder = NeuronSortOrder.CREATIONDATE;

    public WorkspaceNeuronList(AnnotationManager annotationManager,
        AnnotationModel annotationModel, int width) {
        this.annotationManager = annotationManager;
        this.annotationModel = annotationModel;
        this.width = width;
        setupUI();
    }

    /**
     * @param panListener the panListener to set
     */
    public void setPanListener(CameraPanToListener panListener) {
        this.panListener = panListener;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    private void setupUI() {
        setLayout(new GridBagLayout());

        // list of neurons
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        add(new JLabel("Neurons", JLabel.LEADING), c);


        // neuron table
        neuronTableModel = new NeuronTableModel();
        neuronTableModel.setAnnotationModel(annotationModel);
        neuronTable = new JTable(neuronTableModel);

        neuronTable.getColumnModel().getColumn(0).setPreferredWidth(175);
        neuronTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        neuronTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        neuronTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // hide columns that we only maintain for sorting (eg, creation date)
        neuronTable.removeColumn(neuronTable.getColumnModel().getColumn(2));

        // sort, but only programmatically
        neuronTable.setAutoCreateRowSorter(true);
        sorter = (DefaultRowSorter<TableModel, String>) neuronTable.getRowSorter();
        for (int i=0 ; i<neuronTable.getColumnCount() ; i++) {
            sorter.setSortable(i, false);
        }

        // custom renderer does color swatches for the neurons
        neuronTable.setDefaultRenderer(Color.class, new ColorCellRenderer(true));

        neuronTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = neuronTable.convertRowIndexToModel(viewRow);
                    TmNeuron selectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
                    if (me.getClickCount() == 1) {
                        // which column?
                        int viewColumn = table.columnAtPoint(me.getPoint());
                        int modelColumn = neuronTable.convertColumnIndexToModel(viewColumn);
                        if (modelColumn == 0) {
                            // single click name, select neuron
                            if (neuronSelectedListener != null)
                                neuronSelectedListener.selectNeuron(selectedNeuron);
                        } else if (modelColumn == 1) {
                            // single click color, edit style
                            annotationManager.chooseNeuronStyle(selectedNeuron);

                            // what update?


                        }
                    } else if (me.getClickCount() == 2) {
                        // double click, go to neuron
                        onNeuronDoubleClicked(selectedNeuron);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(neuronTable);
        neuronTable.setFillsViewportHeight(true);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 1.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        add(scrollPane, c2);

        loadWorkspace(null);
    }

    /**
     * called when current neuron changes; both selects the neuron visually
     * as well as replaces the old neuron in the model with the new one
     */
    public void selectNeuron(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        updateModel(neuron);
        int neuronModelRow = neuronTableModel.getRowForNeuron(neuron);
        if (neuronModelRow >= 0) {
            int neuronTableRow = neuronTable.convertRowIndexToView(neuronModelRow);
            neuronTable.setRowSelectionInterval(neuronTableRow, neuronTableRow);
        } else {
            neuronTable.clearSelection();
        }
    }

    /**
     * called when the sort order is changed in the UI
     */
    public void sortOrderChanged(NeuronSortOrder neuronSortOrder) {
        if (this.neuronSortOrder == neuronSortOrder) {
            return;
        }
        this.neuronSortOrder = neuronSortOrder;
        setSortOrder(neuronSortOrder);
    }

    private void setSortOrder(NeuronSortOrder neuronSortOrder) {
        if (neuronTableModel.getRowCount() > 0) {
            switch(neuronSortOrder) {
                case ALPHABETICAL:
                    sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
                    break;
                case CREATIONDATE:
                    sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(2, SortOrder.ASCENDING)));
                    break;
            }
        }
    }

    /**
     * populate the UI with info from the input workspace
     */
    public void loadWorkspace(TmWorkspace workspace) {
        updateModel(workspace);
        setSortOrder(neuronSortOrder);
    }

    /**
     * update the table model given a new workspace
     */
    private void updateModel(TmWorkspace workspace) {
        neuronTableModel.clear();
        if (workspace != null) {
            for (TmNeuron neuron: workspace.getNeuronList()) {
                neuronTableModel.addNeuron(neuron);
            }
            neuronTableModel.fireTableDataChanged();
        }
    }

    /**
     * update the table neuron with a new version of an
     * existing neuron (replaces in place)
     */
    private void updateModel(TmNeuron neuron) {
        neuronTableModel.fireTableDataChanged();
    }


    private void onNeuronDoubleClicked(TmNeuron neuron) {
        // should pan to center of neuron; let's call that the center
        //  of the bounding cube for its annotations
        // I'd prefer this calculation be part of TmNeuron, but
        //  I can't use BoundingBox3d there
        if (neuron.getGeoAnnotationMap().size() != 0) {
            BoundingBox3d bounds = new BoundingBox3d();
            for (TmGeoAnnotation ann: neuron.getGeoAnnotationMap().values()) {
                bounds.include(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
            }
            if (panListener != null) {
                panListener.cameraPanTo(bounds.getCenter());
            }
        }
    }

}

class NeuronTableModel extends AbstractTableModel {

    // note: creation date column will be hidden!
    private String[] columnNames = {"Name", "Style", "Creation Date"};

    private ArrayList<TmNeuron> neurons = new ArrayList<>();

    // need this to retrieve colors!
    private AnnotationModel annotationModel;

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }

    public void clear() {
        neurons = new ArrayList<>();
    }

    public void addNeuron(TmNeuron neuron) {
        neurons.add(neuron);
    }

    public void updateNeuron(TmNeuron neuron) {
        int neuronRow = getRowForNeuron(neuron);
        neurons.set(neuronRow, neuron);
    }

    // boilerplate stuff
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return neurons.size();
    }

    public TmNeuron getNeuronAtRow(int row) {
        return neurons.get(row);
    }

    public int getRowForNeuron(TmNeuron neuron) {
        // we're matching by ID, not object identity
        TmNeuron foundNeuron = null;
        for (TmNeuron n: neurons) {
            if (n.getId().equals(neuron.getId())) {
                foundNeuron = n;
                break;
            }
        }
        if (foundNeuron != null) {
            return neurons.indexOf(foundNeuron);
        } else {
            return -1;
        }
    }

    // needed to get color to work right
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                // neuron itself, which will display as name
                return neurons.get(row);
            case 1:
                // color, from style
                return annotationModel.getNeuronStyle(neurons.get(row)).getColor();
            case 2:
                // creation date, hidden, but there for sorting
                return neurons.get(row).getCreationDate();
            default:
                return null;
        }

    }

}

// pretty much taken from Oracle Java Table tutorial
class ColorCellRenderer extends JLabel implements TableCellRenderer {
    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;


    public ColorCellRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object color,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Color newColor = (Color) color;
        setBackground(newColor);

        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                            table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                            table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        }

        setToolTipText("RGB value: " + newColor.getRed() + ", "
                + newColor.getGreen() + ", "
                + newColor.getBlue());
        return this;
    }

}

