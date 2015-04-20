package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnnotationSelectionListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.EditNoteRequestedListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

/**
 * this UI element displays a list of annotations according to a
 * user-specified filter of some kind; the filters may include
 * either predefined buttons or free-form text filters; the
 * filtering conditions will include both geometry (eg,
 * end or branch) and notes (and terms contained therein)
 *
 * implementation note: updates are really brute force
 * right now; essentially end up rebuilding the whole model
 * and view every time anything changes
 *
 * djo, 4/15
 *
 */
public class FilteredAnnotationList extends JPanel {

    // GUI stuff
    private int width;
    private static final int height = AnnotationPanel.SUBPANEL_STD_HEIGHT;
    JTable filteredTable;

    // data stuff
    FilteredAnnotationModel model;
    TmNeuron currentNeuron;
    TmWorkspace currentWorkspace;


    // interaction
    private CameraPanToListener panListener;
    private AnnotationSelectionListener annoSelectListener;
    private EditNoteRequestedListener editNoteRequestedListener;



    public FilteredAnnotationList(int width) {
        this.width = width;

        // set up model
        model = new FilteredAnnotationModel();


        // GUI stuff
        setupUI();

        // interactions & behaviors
        // allows (basic) sorting
        // eventually replace with something custom that also filters
        filteredTable.setAutoCreateRowSorter(true);


    }

    public void loadNeuron(TmNeuron neuron) {
        currentNeuron = neuron;
        updateData();

        // testing
        if (neuron != null) {
            System.out.println("neuron loaded: " + neuron.getName());
        }

    }

    public void loadWorkspace(TmWorkspace workspace) {
        currentWorkspace = workspace;
        if (currentWorkspace == null) {
            currentNeuron = null;
        }
        updateData();

        // testing
        if (workspace != null) {
            System.out.println("workspace loaded: " + workspace.getName());
        }

    }

    private void updateData() {
        // totally brute force; we don't know what updated, so
        //  start from scratch each time

        // clear model

        // loop over neurons
        // loop over roots in neuron
        // loop over annotations per root
        // for each "interesting" annotation, create a row in the table model

        // if root, branch, or end = interesting
        // if has note = interesting




        // trigger redisplay (if needed?)


    }

    private void setupUI() {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.weightx = 1.0;
        c.weighty = 0.0;
        add(new JLabel("Annotations", JLabel.LEADING), c);

        filteredTable = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(filteredTable);
        filteredTable.setFillsViewportHeight(true);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 1.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        add(scrollPane, c2);
    }

    public void setAnnoSelectListener(AnnotationSelectionListener annoSelectListener) {
        this.annoSelectListener = annoSelectListener;
    }

    public void setEditNoteRequestListener(EditNoteRequestedListener editNoteRequestedListener) {
        this.editNoteRequestedListener = editNoteRequestedListener;
    }

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

}


class FilteredAnnotationModel extends AbstractTableModel {
    private String[] columns = {"ann ID", "geom", "note"};
    private Object[][] data = {
            {1234, "o--", "hello"},
            {2132, "--<", "hello again"}
    };

    public String getColumnName(int column) {
        return columns[column];
    }
    public int getColumnCount() {
        return columns.length;
    }

    public int getRowCount() {
        return data.length;
    }

    public Object getValueAt(int row, int column) {
        return data[row][column];
    }
}