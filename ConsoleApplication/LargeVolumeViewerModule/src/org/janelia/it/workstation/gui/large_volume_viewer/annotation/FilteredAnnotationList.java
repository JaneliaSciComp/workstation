package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnnotationSelectionListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.EditNoteRequestedListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;

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
    AnnotationManager annotationMgr;
    FilteredAnnotationModel model;
    TmNeuron currentNeuron;
    TmWorkspace currentWorkspace;


    // interaction
    private CameraPanToListener panListener;
    private AnnotationSelectionListener annoSelectListener;
    private EditNoteRequestedListener editNoteRequestedListener;



    public FilteredAnnotationList(AnnotationManager annotationMgr, int width) {
        this.annotationMgr = annotationMgr;
        this.width = width;

        // set up model
        model = new FilteredAnnotationModel();


        // GUI stuff
        setupUI();

        // interactions & behaviors
        // allows (basic) sorting
        // future: replace with custom sorter which gets us
        //  filtering (filter by regex on text columns, can
        //  restrict to specific column)
        filteredTable.setAutoCreateRowSorter(true);


    }

    /**
     * given the current data and the state of the UI,
     * populate the model with the subset of the data that
     * satisfies the filter
     */
    private void filterData() {

        // pass

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

        if (currentWorkspace == null) {
            return;
        }


        // clear model
        model.clear();

        // loop over neurons
        // loop over roots in neuron
        // loop over annotations per root
        // for each "interesting" annotation, create a row in the table model

        // if root, branch, or end = interesting
        // if has note = interesting

        // first try: annotations with notes
        for (TmNeuron neuron: currentWorkspace.getNeuronList()) {
            for (TmStructuredTextAnnotation note: neuron.getStructuredTextAnnotationMap().values()) {
                // recall that the parent ID field of the note tells us which ann it's on
                model.addAnnotation(new InterestingAnnotation(note.getParentId(), annotationMgr.getNote(note.getParentId())));
            }
        }



        // refilter:
        filterData();


        // trigger redisplay (?)
        model.fireTableDataChanged();



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
    private String[] columnNames = {"ann ID", "note"};

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

    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return annotations.get(row).getAnnIDText();
            case 1:
                return annotations.get(row).getNoteText();
            default:
                return null;
        }

    }
}

/**
 * this class represents an interesting annotation in a way that
 * is easy to put into the table model; ie, it just contains the
 * specific info the table model needs to display
 */
class InterestingAnnotation {
    private Long annotationID;
    private String noteText;

    public InterestingAnnotation(Long annotationID, String noteText) {
        this.annotationID = annotationID;
        this.noteText = noteText;
    }

    public String getAnnIDText() {
        String annID = annotationID.toString();
        return annID.substring(annID.length() - 4);
    }

    public String getNoteText() {
        return noteText;
    }
}