package org.janelia.workstation.gui.large_volume_viewer.annotation;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.SynchronizationHelper;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.workstation.geom.Vec3;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationSelectionListener;
import org.janelia.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.workstation.gui.large_volume_viewer.controller.EditNoteRequestedListener;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerLocationProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * another implementation note: OK, I admit it, I prefer Python
 * over Java; as such, I thought throwing one or two support classes
 * in this file would be not a big deal, but it mushroomed, and
 * now there's all kinds of stuff, all of it only used by
 * the primary class, but still...definitely need to refactor
 * at some point...
 *
 * djo, 4/15
 *
 */
public class FilteredAnnotationList extends JPanel {

    // GUI stuff
    private int width;
    private static final int height = 3 * AnnotationPanel.SUBPANEL_STD_HEIGHT;
    private JTable filteredTable;
    private JTextField filterField;
    private TableRowSorter<FilteredAnnotationModel> sorter;
    private JCheckBox currentNeuronCheckbox;

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

    private static FilteredAnnotationList theInstance;
    private boolean skipUpdate=false;

    public static FilteredAnnotationList createInstance(final AnnotationManager annotationMgr, final AnnotationModel annotationModel, int width) {
        theInstance = new FilteredAnnotationList(annotationMgr, annotationModel, width);
        return theInstance;
    }

    public static FilteredAnnotationList getInstance() {
        return theInstance;
    }

    public void setSkipUpdate(boolean skipUpdate) {
        this.skipUpdate = skipUpdate;
    }
    
    public void beginTransaction() {
        this.skipUpdate = true;
        updateData();
    }
    
    public void endTransaction() {
        this.skipUpdate = false;
        updateData();
    }

    private FilteredAnnotationList(final AnnotationManager annotationMgr, final AnnotationModel annotationModel, int width) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.width = width;

        // set up model & data-related stuff
        model = annotationModel.getFilteredAnnotationModel();
        setupFilters();

        // GUI stuff
        setupUI();

        // interactions & behaviors
        // sorter allows click-on-column-header sorting, plus required
        //  to do text filtering
        sorter = new TableRowSorter<>((FilteredAnnotationModel) filteredTable.getModel());
        filteredTable.setRowSorter(sorter);

        // default sort order: let's go with first (date) column for now
        filteredTable.getRowSorter().toggleSortOrder(0);


        // single-click selects annotation, and
        //  double-click shifts camera to annotation, except if you
        //  double-click note, then you get the edit/delete note dialog
        filteredTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = filteredTable.convertRowIndexToModel(viewRow);
                    if (me.getClickCount() == 1) {
                        InterestingAnnotation ann = model.getAnnotationAtRow(modelRow);
                        table.setRowSelectionInterval(viewRow, viewRow);
                        annoSelectListener.annotationSelected(ann.getAnnotationID());
                    } else if (me.getClickCount() == 2) {
                        // which column?
                        int viewColumn = table.columnAtPoint(me.getPoint());
                        int modelColumn = table.convertColumnIndexToModel(viewColumn);
                        InterestingAnnotation interestingAnnotation = model.getAnnotationAtRow(modelRow);
                        TmGeoAnnotation ann = annotationModel.getGeoAnnotationFromID(interestingAnnotation.getNeuronID(), interestingAnnotation.getAnnotationID());
                        if (modelColumn == 2) {
                            // double-click note: edit note dialog
                            editNoteRequestedListener.editNote(ann);
                        } else {
                            // everyone else, shift camera to annotation
                            if (panListener != null) {
                                if (ann != null) {
                                    SimpleWorker syncher = new SimpleWorker() {
                                        @Override
                                        protected void doStuff() throws Exception {
                                            panListener.cameraPanTo(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
                                            Vec3 location = annotationMgr.getTileFormat().micronVec3ForVoxelVec3Centered(new Vec3(ann.getX(), ann.getY(), ann.getZ()));

                                            // send event to Horta to also center on this item
                                            SynchronizationHelper helper = new SynchronizationHelper();
                                            Tiled3dSampleLocationProviderAcceptor originator = helper.getSampleLocationProviderByName(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
                                            SampleLocation sampleLocation = originator.getSampleLocation();
                                            sampleLocation.setFocusUm(location.getX(), location.getY(), location.getZ());
                                            sampleLocation.setNeuronId(ann.getNeuronId());
                                            sampleLocation.setNeuronVertexId(ann.getId());
                                            Collection<Tiled3dSampleLocationProviderAcceptor> locationAcceptors = helper.getSampleLocationProviders(LargeVolumeViewerLocationProvider.PROVIDER_UNIQUE_NAME);
                                            for (Tiled3dSampleLocationProviderAcceptor acceptor: locationAcceptors) {
                                                if (acceptor.getProviderDescription().equals("Horta - Focus On Location")) {
                                                    acceptor.setSampleLocation(sampleLocation);
                                                }
                                            }
                                        }

                                        @Override
                                        protected void hadSuccess() {
                                            annotationModel.selectPoint(ann.getNeuronId(), ann.getId());
                                        }

                                        @Override
                                        protected void hadError(Throwable error) {
                                            FrameworkAccess.handleException(error);
                                        }
                                    };
                                    syncher.execute();
                                }
                            }
                        }
                    }
                }
            }
        });

        // set the current filter late, after both the filters and UI are
        //  set up
        setCurrentFilter(filters.get("default"));
    }

    // the next routines are called by PanelController (etc) when data changes;
    //   for now, they all call the same internal, brute force update

    public void loadNeuron(TmNeuronMetadata neuron) {
        updateData();
    }

    public void loadWorkspace(TmWorkspace workspace) {
        updateData();
    }

    public void notesChanged(TmGeoAnnotation ann) {
        updateData();
    }

    public void annotationChanged(TmGeoAnnotation ann) {
        updateData();
    }

    public void annotationsChanged(List<TmGeoAnnotation> annotationList) {
        updateData();
    }

    public synchronized void updateData() {

        if (skipUpdate)
            return;

        // check how long to update
        // ans: with ~2k annotations, <20ms to update
        // Stopwatch stopwatch = new Stopwatch();
        // stopwatch.start();

        // totally brute force; we don't know what updated, so
        //  start from scratch each time

        int savedSelectionRow = filteredTable.getSelectedRow();
        InterestingAnnotation savedAnn = null;
        if (savedSelectionRow >= 0) {
            savedAnn = model.getAnnotationAtRow(filteredTable.convertRowIndexToModel(savedSelectionRow));
        }

        TmWorkspace currentWorkspace = annotationModel.getCurrentWorkspace();
        if (currentWorkspace == null) {
            return;
        }
        
        model.clear();

        if (currentNeuronCheckbox.isSelected()) {
            // Necessary optimization: only consider current neuron
            TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();
            if (currentNeuron!=null) {
                updateData(currentNeuron);
            }
        }
        else {
            // Consider all neurons
            for (TmNeuronMetadata neuron: new ArrayList<>(annotationModel.getNeuronList())) {
                updateData(neuron);
            }
        }

        model.fireTableDataChanged();

        // restore selection
        if (savedSelectionRow >= 0 && savedAnn != null) {
            int newRow = model.findAnnotation(savedAnn);
            if (newRow >= 0) {
                int viewRow = filteredTable.convertRowIndexToView(newRow);
                filteredTable.setRowSelectionInterval(viewRow, viewRow);
            }
        }

        // stopwatch.stop();
        // System.out.println("updated filtered annotation list; elapsed time = " + stopwatch.toString());
    }
    
    public void updateData(TmNeuronMetadata neuron) {

        // loop over roots in neuron, annotations per root;
        //  put all the "interesting" annotations in a list
        
        AnnotationFilter filter = getCurrentFilter();
        String note;
        
        for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
            for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                note = annotationMgr.getNote(neuron.getId(), ann.getId());
                if (note.length() == 0) {
                    note = "";
                }
                InterestingAnnotation maybeInteresting =
                    new InterestingAnnotation(ann.getId(),
                        neuron.getId(),
                        ann.getCreationDate(),
                        ann.getModificationDate(),
                        getAnnotationGeometry(ann),
                        note);
                if (filter.isInteresting(maybeInteresting)) {
                    model.addAnnotation(maybeInteresting);
                }
            }
        }
    }

    private void setupFilters() {
        // set up all the filters once; put in order you want them to appear

        // default filter: interesting = has a note or isn't a straight link (ie, root, end, branch)
        filters.put("default", new OrFilter(new HasNoteFilter(), new NotFilter(new GeometryFilter(AnnotationGeometry.LINK))));

        // ...and those two conditions separately:
        filters.put("notes", new HasNoteFilter());
        filters.put("geometry", new NotFilter(new GeometryFilter(AnnotationGeometry.LINK)));


        // endpoint that isn't marked traced or problem
        List<AnnotationFilter> tempFilters = new ArrayList<>();
        tempFilters.add(new NotFilter(new PredefNoteFilter(PredefinedNote.TRACED_END)));
        tempFilters.add(new NotFilter(new PredefNoteFilter(PredefinedNote.PROBLEM_END)));
        tempFilters.add(new GeometryFilter(AnnotationGeometry.END));
        filters.put("ends", new AllFilter(tempFilters));


        // points marked as branches-to-be
        filters.put("branches", new PredefNoteFilter(PredefinedNote.FUTURE_BRANCH));


        // roots (which are often placed in cell bodies)
        filters.put("roots", new GeometryFilter(AnnotationGeometry.ROOT));

        // interesting and review tags
        filters.put("interesting", new PredefNoteFilter(PredefinedNote.POINT_OF_INTEREST));
        filters.put("review", new PredefNoteFilter(PredefinedNote.REVIEW));
        filters.put("unique 1", new PredefNoteFilter(PredefinedNote.UNIQUE_1));
        filters.put("unique 2", new PredefNoteFilter(PredefinedNote.UNIQUE_2));

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
                        // show modification date
                        tip = model.getAnnotationAtRow(realRowIndex).getModificationDate().toString();
                    } else if (realColumnIndex == 1) {
                        // no tip here
                        tip = null;
                    } else {
                        // for the rest, show the full text (esp. for notes), if any
                        tip = (String) model.getValueAt(realRowIndex, realColumnIndex);
                        if (tip.length() == 0) {
                            tip = null;
                        }
                    }
                    return tip;
                } else {
                    // off visible rows, returns null = no tip
                    return tip;
                }
            }
        };


        filteredTable.setRowSelectionAllowed(true);


        // custom renderer for date column:
        filteredTable.getColumnModel().getColumn(0).setCellRenderer(new ShortDateRenderer());

        // inelegant, but hand-tune column widths (finally seems to work):
        filteredTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        filteredTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        filteredTable.getColumnModel().getColumn(2).setPreferredWidth(105);
        // and let all the extra space go into the note column on resize
        filteredTable.getColumnModel().getColumn(0).setMaxWidth(70);
        filteredTable.getColumnModel().getColumn(1).setMaxWidth(50);

        JScrollPane scrollPane = new JScrollPane(filteredTable);
        filteredTable.setFillsViewportHeight(true);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.weighty = 1.0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        add(scrollPane, c2);


        // combo box to change filters
        // names taken from contents of filter list set up elsewhere
        //  probably should take names directly?  but I'd like to control
        //  the order, something the returned keySet doesn't do;
        //  plus, want to be sure 'default' comes up selected
        JPanel filterMenuPanel = new JPanel();
        filterMenuPanel.setLayout(new BorderLayout(2, 2));
        filterMenuPanel.add(new JLabel("Filter:"), BorderLayout.LINE_START);
        String[] filterNames = {"default", "ends", "branches", "roots", "notes",
            "geometry", "interesting", "review", "unique 1", "unique 2"};
        final JComboBox<String> filterMenu = new JComboBox<>(filterNames);
        filterMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>) e.getSource();
                String name = (String) cb.getSelectedItem();
                setCurrentFilter(filters.get(name));
                updateData();
            }
        });
        filterMenuPanel.add(filterMenu, BorderLayout.CENTER);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.gridx = 0;
        c3.gridy = GridBagConstraints.RELATIVE;
        c3.weighty = 0.0;
        c3.anchor = GridBagConstraints.PAGE_START;
        c3.fill = GridBagConstraints.HORIZONTAL;
        add(filterMenuPanel, c3);

        // these buttons will trigger a change in the drop-down menu below
        JPanel filterButtons = new JPanel();
        filterButtons.setLayout(new BoxLayout(filterButtons, BoxLayout.LINE_AXIS));

        JButton defaultButton = new JButton();
        filterButtons.add(defaultButton);
        defaultButton.setSelected(true);

        JButton endsButton = new JButton();
        filterButtons.add(endsButton);

        JButton branchButton = new JButton();
        filterButtons.add(branchButton);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultButton);
        buttonGroup.add(endsButton);
        buttonGroup.add(branchButton);

        // need a second row of these:
        JPanel filterButtons2 = new JPanel();
        filterButtons2.setLayout(new BoxLayout(filterButtons2, BoxLayout.LINE_AXIS));

        JButton reviewButton = new JButton();
        JButton unique1Button = new JButton();
        JButton unique2Button = new JButton();
        filterButtons2.add(reviewButton);
        filterButtons2.add(unique1Button);
        filterButtons2.add(unique2Button);

        // same button group:
        buttonGroup.add(reviewButton);

        GridBagConstraints c4 = new GridBagConstraints();
        c4.gridx = 0;
        c4.gridy = GridBagConstraints.RELATIVE;
        c4.weighty = 0.0;
        c4.anchor = GridBagConstraints.PAGE_START;
        c4.fill = GridBagConstraints.HORIZONTAL;
        add(filterButtons, c4);
        add(filterButtons2, c4);


        // hook buttons to filter menu
        defaultButton.setAction(new AbstractAction("Default") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("default");
            }
        });
        endsButton.setAction(new AbstractAction("Ends") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("ends");
            }
        });
        branchButton.setAction(new AbstractAction("Branches") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("branches");
            }
        });
        reviewButton.setAction(new AbstractAction("Review") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("review");
            }
        });
        unique1Button.setAction(new AbstractAction("Unique 1") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("unique 1");
            }
        });
        unique2Button.setAction(new AbstractAction("Unique 2") {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterMenu.setSelectedItem("unique 2");
            }
        });

        // checkbox for current neuron only
        currentNeuronCheckbox = new JCheckBox("Current neuron only");
        currentNeuronCheckbox.setSelected(true);
        currentNeuronCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateData();
            }
        });

        GridBagConstraints c5 = new GridBagConstraints();
        c5.gridx = 0;
        c5.gridy = GridBagConstraints.RELATIVE;
        c5.weighty = 0.0;
        c5.anchor = GridBagConstraints.PAGE_START;
        c5.fill = GridBagConstraints.HORIZONTAL;
        add(currentNeuronCheckbox, c5);


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

        GridBagConstraints c6 = new GridBagConstraints();
        c6.gridx = 0;
        c6.gridy = GridBagConstraints.RELATIVE;
        c6.weighty = 0.0;
        c6.anchor = GridBagConstraints.PAGE_START;
        c6.fill = GridBagConstraints.HORIZONTAL;
        add(filterPanel, c6);


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

    /**
     * returns the currently active filter as determined
     * by the UI; includes the effect of "current neuron only" as
     * well as the drop menus and buttons
     *
     * implementation note: the "current neuron" logic works better
     * here than in the "set" side, because flipping the "current
     * neuron" toggle doesn't explicitly set the filter
     */
    public AnnotationFilter getCurrentFilter() {
        TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuronCheckbox.isSelected() && currentNeuron != null) {
            return new AndFilter(new NeuronFilter(currentNeuron), currentFilter);
        } else {
            return currentFilter;
        }
    }

    /**
     * sets the current filter
     */
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

/**
 * this renderer displays a short form of the time stamp:
 *
 *   hour:minute if today
 *   day/month if less than a year old
 *   year if more than a year old
 *
 */
class ShortDateRenderer extends DefaultTableCellRenderer {
    private final static String TIME_DATE_FORMAT = "HH:mm";
    private final static String DAY_DATE_FORMAT = "MM/dd";
    private final static String YEAR_DATE_FORMAT = "yyyy";

    public ShortDateRenderer() { super(); }

    public void setValue(Object value) {
        if (value != null) {
            /*
            // this is 24 hours ago, which is not what they want anymore
            Calendar oneDayAgo = Calendar.getInstance();
            oneDayAgo.setTime(new Date());
            oneDayAgo.add(Calendar.DATE, -1);
            */

            Calendar midnight = new GregorianCalendar();
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);


            Calendar oneYearAgo = Calendar.getInstance();
            oneYearAgo.setTime(new Date());
            oneYearAgo.add(Calendar.YEAR, -1);

            Calendar creation = Calendar.getInstance();
            creation.setTime((Date) value);

            String dateFormat;
            if (midnight.compareTo(creation) < 0) {
                // hour:minute if today
                dateFormat = TIME_DATE_FORMAT;
            } else if (oneYearAgo.compareTo(creation) < 0) {
                // month/day if older than 1 day
                dateFormat = DAY_DATE_FORMAT;
            } else {
                // if older than 1 year, just year
                dateFormat = YEAR_DATE_FORMAT;
            }
            setText(new SimpleDateFormat(dateFormat).format((Date) value));
        }
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

class NeuronFilter implements AnnotationFilter {
    private Long neuronID;
    public NeuronFilter(TmNeuronMetadata neuron) {
        this.neuronID = neuron.getId();
    }
    @Override
    public boolean isInteresting(InterestingAnnotation ann) {
        return ann.getNeuronID().equals(neuronID);
    }
}

