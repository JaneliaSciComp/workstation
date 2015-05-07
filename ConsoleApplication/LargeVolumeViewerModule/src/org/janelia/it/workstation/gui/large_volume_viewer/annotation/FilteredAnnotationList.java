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
import javax.swing.table.AbstractTableModel;
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
    private JTable filteredTable;
    private JTextField filterField;
    private TableRowSorter<FilteredAnnotationModel> sorter;

    // data stuff
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private FilteredAnnotationModel model;

    private Map<String, AnnotationFilter> filters = new HashMap<>();
    private AnnotationFilter currentFilter;

    // interaction
    private CameraPanToListener panListener;
    private AnnotationSelectionListener annoSelectListener;

    // I'm leaving this in for now, even though it's not used; I can
    //  imagine allowing note editing from this widget in the future
    private EditNoteRequestedListener editNoteRequestedListener;



    public FilteredAnnotationList(final AnnotationManager annotationMgr, final AnnotationModel annotationModel, int width) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.width = width;

        // set up model & data-related stuff
        model = new FilteredAnnotationModel();
        setupFilters();


        // GUI stuff
        setupUI();

        // interactions & behaviors
        // sorter allows click-on-column-header sorting, plus required
        //  to do text filtering
        sorter = new TableRowSorter<>((FilteredAnnotationModel) filteredTable.getModel());
        filteredTable.setRowSorter(sorter);

        // default sort order: let's go with ID column for now?
        filteredTable.getRowSorter().toggleSortOrder(0);


        // single-click selects annotation, and
        //  double-click shifts camera to annotation
        filteredTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = filteredTable.convertRowIndexToModel(viewRow);
                    if (me.getClickCount() == 1) {
                        InterestingAnnotation ann = model.getAnnotationAtRow(modelRow);
                        annoSelectListener.annotationSelected(ann.getAnnotationID());
                    } else if (me.getClickCount() == 2) {
                        if (panListener != null) {
                            InterestingAnnotation interestingAnnotation = model.getAnnotationAtRow(modelRow);
                            TmGeoAnnotation ann = annotationModel.getGeoAnnotationFromID(interestingAnnotation.getAnnotationID());
                            if (ann != null) {
                                panListener.cameraPanTo(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
                            }
                        }
                    }
                }
            }
        });

    }


    // the next routines are called by PanelController (etc) when data changes;
    //   for now, they all call the same internal, brute force update

    public void loadNeuron(TmNeuron neuron) {
        System.out.println("filtered list: loadNeuron()");
        updateData();
    }

    public void loadWorkspace(TmWorkspace workspace) {
        updateData();
    }

    public void annotationChanged(TmGeoAnnotation ann) {
        updateData();
    }

    public void annotationsChanged(List<TmGeoAnnotation> annotationList) {
        updateData();
    }

    private void updateData() {
        // totally brute force; we don't know what updated, so
        //  start from scratch each time

        TmWorkspace currentWorkspace = annotationModel.getCurrentWorkspace();
        if (currentWorkspace == null) {
            return;
        }

        // loop over neurons, roots in neuron, annotations per root;
        //  put all the "interesting" annotations in a list
        model.clear();
        AnnotationFilter filter = getCurrentFilter();
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

    private void setupFilters() {
        // set up all the filters once

        // default filter: interesting = has a note or isn't a straight link (ie, root, end, branch)
        filters.put("default", new OrFilter(new HasNoteFilter(), new NotFilter(new GeometryFilter(AnnotationGeometry.LINK))));


        // endpoint that isn't marked traced or problem
        List<AnnotationFilter> tempFilters = new ArrayList<>();
        tempFilters.add(new NotFilter(new PredefNoteFilter(PredefinedNote.TRACED_END)));
        tempFilters.add(new NotFilter(new PredefNoteFilter(PredefinedNote.PROBLEM_END)));
        tempFilters.add(new GeometryFilter(AnnotationGeometry.END));
        filters.put("ends", new AllFilter(tempFilters));


        // points marked as branches-to-be
        filters.put("branches", new PredefNoteFilter(PredefinedNote.FUTURE_BRANCH));


        // this is probably not right...need to set the variable and the UI state, ugh
        //  do I need to do a model here?
        setCurrentFilter(filters.get("default"));
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
        // implement tool tip while we're here
        filteredTable = new JTable(model) {
            // mostly taken from the Oracle tutorial
            public String getToolTipText(MouseEvent event) {
                String tip = null;
                java.awt.Point p = event.getPoint();
                int rowIndex = rowAtPoint(p);
                if (rowIndex >= 0) {
                    int colIndex = columnAtPoint(p);
                    int realColumnIndex = convertColumnIndexToModel(colIndex);
                    int realRowIndex = convertRowIndexToModel(rowIndex);

                    if (realColumnIndex == 0) {
                        // show full ID
                        tip = model.getAnnotationAtRow(realRowIndex).getAnnotationID().toString();
                    } else if (realColumnIndex == 1) {
                        // no tip here
                        tip = null;
                    } else {
                        // for the rest, show the full text (esp. for notes)
                        tip = (String) model.getValueAt(realRowIndex, realColumnIndex);
                    }
                    return tip;
                } else {
                    // off visible rows, returns null = no tip
                    return tip;
                }
            }
        };


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


        // buttons for pre-defined filters
        JPanel filterButtons = new JPanel();
        filterButtons.setLayout(new BoxLayout(filterButtons, BoxLayout.LINE_AXIS));

        JButton defaultButton = new JButton();
        defaultButton.setAction(new AbstractAction("Default") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentFilter(filters.get("default"));
                updateData();
            }
        });
        filterButtons.add(defaultButton);

        JButton endsButton = new JButton();
        endsButton.setAction(new AbstractAction("Ends") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentFilter(filters.get("ends"));
                updateData();
            }
        });
        filterButtons.add(endsButton);

        JButton branchButton = new JButton();
        branchButton.setAction(new AbstractAction("Branches") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCurrentFilter(filters.get("branches"));
                updateData();
            }
        });
        filterButtons.add(branchButton);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.gridx = 0;
        c3.gridy = GridBagConstraints.RELATIVE;
        c3.weighty = 0.0;
        c3.anchor = GridBagConstraints.PAGE_START;
        c3.fill = GridBagConstraints.HORIZONTAL;
        add(filterButtons, c3);


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

        GridBagConstraints c4 = new GridBagConstraints();
        c4.gridx = 0;
        c4.gridy = GridBagConstraints.RELATIVE;
        c4.weighty = 0.0;
        c4.anchor = GridBagConstraints.PAGE_START;
        c4.fill = GridBagConstraints.HORIZONTAL;
        add(filterPanel, c4);




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
    public AnnotationFilter getFilter() {
        // default filter: has note or isn't a straight link
        return new OrFilter(new HasNoteFilter(), new NotFilter(new GeometryFilter(AnnotationGeometry.LINK)));
    }

    public AnnotationFilter getCurrentFilter() {
        return currentFilter;
    }

    public void setCurrentFilter(AnnotationFilter currentFilter) {
        this.currentFilter = currentFilter;
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
 * a system of configurable filters that will be generated
 * from the UI that will determine whether an annotation
 * is interesting or not
 */
interface AnnotationFilter {
    public boolean isInteresting(InterestingAnnotation ann);
}

class NotFilter implements AnnotationFilter {
    private AnnotationFilter filter;
    public NotFilter(AnnotationFilter filter) {
        this.filter = filter;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return !filter.isInteresting(ann);
    }
}

class AndFilter implements AnnotationFilter {
    private AnnotationFilter filter1;
    private AnnotationFilter filter2;
    public AndFilter(AnnotationFilter filter1, AnnotationFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return filter1.isInteresting(ann) && filter2.isInteresting(ann);
    }
}

class AllFilter implements AnnotationFilter {
    private List<AnnotationFilter> filterList;
    public AllFilter(List<AnnotationFilter> filterList) {
        this.filterList = filterList;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        for (AnnotationFilter filter: filterList) {
            if (!filter.isInteresting(ann)) {
                return false;
            }
        }
        return true;
    }
}

class OrFilter implements AnnotationFilter {
    private AnnotationFilter filter1;
    private AnnotationFilter filter2;
    public OrFilter(AnnotationFilter filter1, AnnotationFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return filter1.isInteresting(ann) || filter2.isInteresting(ann);
    }
}

class AnyFilter implements AnnotationFilter {
    private List<AnnotationFilter> filterList;
    public AnyFilter(List<AnnotationFilter> filterList) {
        this.filterList = filterList;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        for (AnnotationFilter filter: filterList) {
            if (filter.isInteresting(ann)) {
                return true;
            }
        }
        return false;
    }
}

class GeometryFilter implements AnnotationFilter {
    private AnnotationGeometry geometry;
    public GeometryFilter(AnnotationGeometry geometry) {
        this.geometry = geometry;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return ann.getGeometry() == this.geometry;
    }
}

class NoteTextFilter implements AnnotationFilter {
    private String text;
    public NoteTextFilter(String text) {
        this.text = text;
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return ann.getNoteText().contains(text);
    }
}

class HasNoteFilter implements  AnnotationFilter {
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return ann.hasNote();
    }
}

class PredefNoteFilter implements AnnotationFilter {
    private PredefinedNote predefNote;
    public PredefNoteFilter(PredefinedNote predefNote) {
        this.predefNote = predefNote;
    }

    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return ann.getNoteText().contains(predefNote.getNoteText());
    }
}