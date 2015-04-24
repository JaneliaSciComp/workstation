package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnnotationSelectionListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.EditNoteRequestedListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

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
    private static final int height = 2 * AnnotationPanel.SUBPANEL_STD_HEIGHT;
    JTable filteredTable;
    JTextField filterField;
    TableRowSorter<FilteredAnnotationModel> sorter;

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
        // filteredTable.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>((FilteredAnnotationModel) filteredTable.getModel());
        filteredTable.setRowSorter(sorter);


        // single-click selects annotation
        // NOTE: I suspect this could (or even should) go into the mouse
        //  listener alongside the double-click action below
        filteredTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int viewRow = filteredTable.getSelectedRow();
                if (viewRow >= 0) {
                    // selection still visible
                    int modelRow = filteredTable.convertRowIndexToModel(viewRow);
                    InterestingAnnotation ann = model.getAnnotationAtRow(modelRow);
                    annoSelectListener.annotationSelected(ann.getAnnotationID());
                }
            }
        });

        // double-click shifts camera to annotation
        filteredTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    JTable table = (JTable) me.getSource();
                    int viewRow = table.rowAtPoint(me.getPoint());
                    if (viewRow >= 0) {
                        int modelRow = filteredTable.convertRowIndexToModel(viewRow);
                        if (panListener != null) {
                            InterestingAnnotation interestingAnnotation = model.getAnnotationAtRow(modelRow);
                            TmGeoAnnotation ann = currentNeuron.getGeoAnnotationMap().get(interestingAnnotation.getAnnotationID());
                            if (ann != null) {
                                panListener.cameraPanTo(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
                            }
                        }
                    }
                }
            }
        });


    }


    public void loadNeuron(TmNeuron neuron) {
        currentNeuron = neuron;
        updateData();
    }

    public void loadWorkspace(TmWorkspace workspace) {
        currentWorkspace = workspace;
        if (currentWorkspace == null) {
            currentNeuron = null;
        }
        updateData();
    }

    private void updateData() {
        // totally brute force; we don't know what updated, so
        //  start from scratch each time

        if (currentWorkspace == null) {
            return;
        }

        // loop over neurons, roots in neuron, annotations per root;
        //  put all the "interesting" annotations in a list
        model.clear();
        SimpleAnnotationFilter filter = getFilter();
        String note;
        for (TmNeuron neuron: currentWorkspace.getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                    if (annotationMgr.getNote(ann.getId()).length() > 0) {
                        note = annotationMgr.getNote(ann.getId());
                    } else {
                        note = "";
                    }
                    InterestingAnnotation maybeInteresting =
                        new InterestingAnnotation(ann.getId(),
                            getAnnotationGeometry(ann),
                            note);
                    if (filter.isInteresting(maybeInteresting)) {
                        model.addAnnotation(maybeInteresting);
                    }
                }
            }
        }


        model.fireTableDataChanged();
    }

    private void setupUI() {
        setLayout(new GridBagLayout());

        // label
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.weightx = 1.0;
        c.weighty = 0.0;
        add(new JLabel("Annotations", JLabel.LEADING), c);

        // table
        filteredTable = new JTable(model);

        // we respond to clicks, but we're not really selecting rows
        filteredTable.setRowSelectionAllowed(false);


        // bit inelegant, but hand-tune some widths (default is 75):
        // ...and seems to be ignored, ugh
        filteredTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        filteredTable.getColumnModel().getColumn(1).setPreferredWidth(40);

        JScrollPane scrollPane = new JScrollPane(filteredTable);
        filteredTable.setFillsViewportHeight(true);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 1.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        add(scrollPane, c2);

        // text field for filter
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.LINE_AXIS));
        JLabel filterLabel = new JLabel("Filter text:");
        filterPanel.add(filterLabel);
        filterField = new JTextField();
        filterField.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateRowFilter();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateRowFilter();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        updateRowFilter();
                    }
                }
        );
        filterPanel.add(filterField);
        JButton clearFilter = new JButton("Clear");
        clearFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterField.setText("");
            }
        });
        filterPanel.add(clearFilter);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.gridx = 0;
        c3.gridy = GridBagConstraints.RELATIVE;
        c3.weighty = 0.0;
        c3.anchor = GridBagConstraints.PAGE_START;
        c3.fill = GridBagConstraints.HORIZONTAL;
        add(filterPanel, c3);




    }

    /**
     * update the table filter based on user text input in
     * the filter box; this is a filter based on text in
     * the table, done by Java, compared with a filter on
     * annotation information that we do explicitly above
     */
    private void updateRowFilter() {
        RowFilter<FilteredAnnotationModel, Object> rowFilter = null;
        try {
            rowFilter = RowFilter.regexFilter(filterField.getText());
        } catch (java.util.regex.PatternSyntaxException e) {
            // if the regex doesn't parse, don't update the filter
            return;
        }
        sorter.setRowFilter(rowFilter);
    }

    /**
     * examine the state of the UI and generate an
     * appropriate filter
     */
    public SimpleAnnotationFilter getFilter() {
        // testing:
        return new SimpleAnnotationFilter();
    }

    public AnnotationGeometry getAnnotationGeometry(TmGeoAnnotation ann) {
        if (ann.isRoot()) {
            return AnnotationGeometry.ROOT;
        } else if (ann.isBranch()) {
            return AnnotationGeometry.BRANCH;
        } else if (ann.isEnd()) {
            return AnnotationGeometry.END;
        } else {
            return AnnotationGeometry.LINK;
        }
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
    private String[] columnNames = {"ID", "geo", "note"};

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
                return annotations.get(row).getAnnIDText();
            case 1:
                return annotations.get(row).getGeometryText();
            case 2:
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
    private AnnotationGeometry geometry;

    public InterestingAnnotation(Long annotationID, AnnotationGeometry geometry) {
        new InterestingAnnotation(annotationID, geometry, "");
    }

    public InterestingAnnotation(Long annotationID, AnnotationGeometry geometry, String noteText) {
        this.annotationID = annotationID;
        this.noteText = noteText;
        this.geometry = geometry;
    }

    public Long getAnnotationID() {
        return annotationID;
    }

    public String getAnnIDText() {
        String annID = annotationID.toString();
        return annID.substring(annID.length() - 4);
    }

    public boolean hasNote() {
        return getNoteText().length() > 0;
    }

    public String getNoteText() {
        return noteText;
    }

    public AnnotationGeometry getGeometry() {
        return geometry;
    }

    public String getGeometryText() {
        return geometry.getTexticon();
    }
}

/**
 * terms for describing annotation geometry
 */
enum AnnotationGeometry {
    ROOT        ("o--"),
    BRANCH      ("--<"),
    LINK        ("---"),
    END         ("--o");

    private String texticon;

    AnnotationGeometry(String texticon) {
        this.texticon = texticon;
    }

    public String getTexticon() {
        return texticon;
    }
}

/**
 * a configurable filter generated from the UI that
 * determines whether an annotation is interesting or not
 *
 * I'm going to wrap geometry and note matching in one
 * filter for now; could imagine developing a whole
 * hierarchy of filters and combining them via
 * boolean filters based on other filters, etc.;
 * but let's not get too far ahead of our needs
 */
class SimpleAnnotationFilter {

    public boolean isInteresting(InterestingAnnotation ann) {
        // minimal: has note or geom not link
        return ann.hasNote() || ann.getGeometry() != AnnotationGeometry.LINK;
    }
}