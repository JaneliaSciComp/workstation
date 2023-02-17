package org.janelia.workstation.infopanel;

import org.janelia.workstation.controller.widgets.SimpleIcons;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.action.NeuronChooseColorAction;
import org.janelia.workstation.controller.dialog.ChangeNeuronOwnerDialog;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.infopanel.action.NeuronListProvider;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
//import org.janelia.workstation.gui.large_volume_viewer.style.NeuronStyle;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * this widget displays a list of neurons in a workspace
 *
 * djo, 12/13
 */
public class WorkspaceNeuronList extends JPanel implements NeuronListProvider {

    private JLabel neuronLabel;
    private JTable neuronTable;
    private NeuronTableModel neuronTableModel;
    private DefaultRowSorter<TableModel, String> sorter;
    private JTextField filterField;
    private JTextField ignoreField;
    private JComboBox<String> tagModeMenu;
    private JComboBox<String> tagMenu;
    private JLabel spatialFilterLabel = new JLabel("Disabled");

    private NeuronManager annotationModel;
    private TmModelManager modelManager;

    // for preserving selection across operations
    private TmNeuronMetadata savedSelectedNeuron;

    private int width;
    private static final int height = 2 * AnnotationPanel.SUBPANEL_STD_HEIGHT;

    private static final int NARROW_COLUNN_WIDTH = 50;

    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    // to add new sort order: add to enum here, add menu in AnnotationPanel.java,
    //  and implement the sort in sortOrderChanged, below
    public enum NeuronSortOrder {ALPHABETICAL, CREATIONDATE, OWNER};
    private NeuronSortOrder neuronSortOrder = NeuronSortOrder.CREATIONDATE;

    public WorkspaceNeuronList(int width) {
        this.width = width;
        setupUI();
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
        annotationModel = NeuronManager.getInstance();
        modelManager = TmModelManager.getInstance();
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
        neuronLabel =new JLabel("Neurons", JLabel.LEADING);
        add(neuronLabel, c);


        // neuron table
        neuronTableModel = new NeuronTableModel();
        neuronTableModel.setAnnotationModel(annotationModel);
        neuronTable = new JTable(neuronTableModel){
            // mostly taken from the Oracle tutorial
            public String getToolTipText(MouseEvent event) {
                String tip = null;
                java.awt.Point p = event.getPoint();
                int rowIndex = rowAtPoint(p);
                if (rowIndex >= 0) {
                    int colIndex = columnAtPoint(p);
                    int realColumnIndex = convertColumnIndexToModel(colIndex);
                    int realRowIndex = convertRowIndexToModel(rowIndex);
                    TmNeuronMetadata neuronMetadata = neuronTableModel.getNeuronAtRow(realRowIndex);
                    if (realColumnIndex == NeuronTableModel.COLUMN_NAME) {
                        if (neuronMetadata.getSyncLevel() < NeuronTableModel.SYNC_WARN_LEVEL) {
                            tip = neuronMetadata.getName();
                        } else {
                            tip = neuronMetadata.getName() + " (" + neuronMetadata.getSyncLevel() + " unsynced changes)";
                        }
                    } else if (realColumnIndex == NeuronTableModel.COLUMN_OWNER_ICON) {
                        tip = neuronMetadata.getOwnerName();
                    } else if (realColumnIndex == NeuronTableModel.COLUMN_COLOR) {
                        Color color = neuronMetadata.getColor();
                        if (color == null) {
                            // get the default if there isn't a stored user-chosen color
                            //color = annotationModel.getNeuronStyle(neuronMetadata).getColor();
                        }
                        tip = "RGB value: " + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue();
                    }
                    return tip;
                } else {
                    // off visible rows, returns null = no tip
                    return tip;
                }
            }
            // likewise:
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                        int modelIndex =
                                columnModel.getColumn(index).getModelIndex();
                        return neuronTableModel.getColumnTooltip(modelIndex);
                    }
                };
            }
        };

        // we want the name column to be the only one that expands
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_NAME).setPreferredWidth(175);

        // fixed width, narrow columns:
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_COLOR).setPreferredWidth(NARROW_COLUNN_WIDTH);
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_COLOR).setMaxWidth(NARROW_COLUNN_WIDTH);
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_OWNER_ICON).setPreferredWidth(NARROW_COLUNN_WIDTH);
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_OWNER_ICON).setMaxWidth(NARROW_COLUNN_WIDTH);
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_VISIBILITY).setPreferredWidth(NARROW_COLUNN_WIDTH);
        neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_VISIBILITY).setMaxWidth(NARROW_COLUNN_WIDTH);

        neuronTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        // hide columns that we only maintain for sorting (eg, creation date)
        // this is fragile; peeling the columns off from the right end works, but it failed
        //  from the other direction; so always put the fake columns at the end and remove
        //  from the end
        neuronTable.removeColumn(neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_OWNER_NAME));
        neuronTable.removeColumn(neuronTable.getColumnModel().getColumn(NeuronTableModel.COLUMN_CREATION_DATE));

        // sort, but only programmatically
        neuronTable.setAutoCreateRowSorter(true);
        sorter = (DefaultRowSorter<TableModel, String>) neuronTable.getRowSorter();
        for (int i=0 ; i<neuronTable.getColumnCount() ; i++) {
            sorter.setSortable(i, false);
        }

        // custom renderer does color swatches for the neurons
        neuronTable.setDefaultRenderer(Color.class, new ColorCellRenderer(true));

        // name is italic if neuron is becoming unsynced
        neuronTable.getColumn(neuronTable.getColumnName(NeuronTableModel.COLUMN_NAME)).setCellRenderer(new SyncLevelRenderer());


        neuronTable.addMouseListener(new MouseHandler() {

            private void selectNeuron(TmNeuronMetadata neuron) {
                List<DomainObject> neuronList = new ArrayList<>();
                neuronList.add(neuron);
                SelectionNeuronsEvent event = new SelectionNeuronsEvent(this,
                        neuronList, true, false);
                ViewerEventBus.postEvent(event);
            }

            @Override
            protected void popupTriggered(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = neuronTable.convertRowIndexToModel(viewRow);
                    TmNeuronMetadata selectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
                    // select neuron
                    selectNeuron(selectedNeuron);

                    // show popup menu for the selected neuron
                    JPopupMenu popupMenu = createPopupMenu(selectedNeuron);
                    if (popupMenu!=null) {
                        popupMenu.show(me.getComponent(), me.getX(), me.getY());
                    }
                    me.consume();
                }
            }

            @Override
            protected void singleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = neuronTable.convertRowIndexToModel(viewRow);
                    TmNeuronMetadata selectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
                    // which column?
                    int viewColumn = table.columnAtPoint(me.getPoint());
                    int modelColumn = neuronTable.convertColumnIndexToModel(viewColumn);
                    if (modelColumn == NeuronTableModel.COLUMN_NAME) {
                        // single click name, select neuron
                        selectNeuron(selectedNeuron);
                    }  else if (modelColumn == NeuronTableModel.COLUMN_COLOR) {
                        NeuronChooseColorAction action = new NeuronChooseColorAction();
                        action.chooseNeuronColor(NeuronManager.getInstance().getNeuronFromNeuronID(selectedNeuron.getId()));
                        // the click might move the neuron selection, which we don't want
                        syncSelection();
                    } else if (modelColumn == NeuronTableModel.COLUMN_VISIBILITY) {
                        // single click visibility = toggle visibility
                        modelManager.getCurrentView().toggleHidden(selectedNeuron.getId());
                        neuronTableModel.updateNeuron(selectedNeuron);
                        NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                                this, Arrays.asList(new TmNeuronMetadata[]{selectedNeuron}));
                        ViewerEventBus.postEvent(updateEvent);
                        // the click might move the neuron selection, which we don't want
                        syncSelection();
                    } else if (modelColumn == NeuronTableModel.COLUMN_OWNER_ICON) {
                        String owner = selectedNeuron.getOwnerName();
                        String ownerKey = selectedNeuron.getOwnerKey();
                        String username = AccessManager.getAccessManager().getActualSubject().getName();

                        if (owner.equals(username) ||
                            ownerKey.equals(TRACERS_GROUP) ||
                            // admins can change ownership on any neuron
                                TmViewerManager.getInstance().isOwnershipAdmin()) {

                            // pop up a dialog so the user can request to change the ownership
                            //  of the neuron
                            ChangeNeuronOwnerDialog dialog = new ChangeNeuronOwnerDialog(null);
                            dialog.setVisible(true);
                            if (dialog.isSuccess()) {
                                SimpleWorker changer = new SimpleWorker() {
                                    @Override
                                    protected void doStuff() throws Exception {
                                        NeuronManager.getInstance().changeNeuronOwner(Arrays.asList(new TmNeuronMetadata[]{selectedNeuron}),
                                                dialog.getNewOwnerKey());
                                    }

                                    @Override
                                    protected void hadSuccess() {

                                    }

                                    @Override
                                    protected void hadError(Throwable error) {
                                        FrameworkAccess.handleException("Could not change neuron owner!", error);
                                    }
                                };
                                changer.execute();
                            }

                        } else {
                            // submit a request to take ownership

                            // for now:
                            JOptionPane.showMessageDialog(null,
                                owner + " owns this neuron. You need to ask them or an admin to give this neuron to you.");
                        }
                        // the click might move the neuron selection, which we don't want
                        syncSelection();
                    }
                }
                me.consume();
            }
            
            @Override
            protected void doubleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = neuronTable.convertRowIndexToModel(viewRow);
                    TmNeuronMetadata selectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
                    // double click, go to neuron
                    onNeuronDoubleClicked(selectedNeuron);
                }
                me.consume();
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


        // text field for filter
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.LINE_AXIS));
        JLabel filterLabel = new JLabel("Text filter:");
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
        clearFilter.addActionListener(event -> filterField.setText(""));
        filterPanel.add(clearFilter);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.gridx = 0;
        c3.gridy = GridBagConstraints.RELATIVE;
        c3.weighty = 0.0;
        c3.anchor = GridBagConstraints.PAGE_START;
        c3.fill = GridBagConstraints.HORIZONTAL;
        add(filterPanel, c3);

        // text field for ignore prefix
        JPanel ignorePanel = new JPanel();
        ignorePanel.setLayout(new BoxLayout(ignorePanel, BoxLayout.LINE_AXIS));
        ignorePanel.add(new JLabel("Ignore prefix:"));
        ignoreField = new JTextField();
        ignoreField.getDocument().addDocumentListener(
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
        ignorePanel.add(ignoreField);
        JButton clearIgnore = new JButton("Clear");
        clearIgnore.addActionListener(event -> ignoreField.setText(""));
        ignorePanel.add(clearIgnore);

        add(ignorePanel, c3);


        // tag filter
        JPanel tagFilterPanel = new JPanel();
        tagFilterPanel.setLayout(new BoxLayout(tagFilterPanel, BoxLayout.LINE_AXIS));
        tagFilterPanel.add(new JLabel("Tag filter:"));

        String [] modeStrings = {"(none)", "include", "exclude"};
        tagModeMenu = new JComboBox<String>(modeStrings);
        tagModeMenu.setSelectedIndex(0);
        tagModeMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String mode = (String) tagModeMenu.getSelectedItem();
                tagModeChanged(mode);
            }
        });
        tagFilterPanel.add(tagModeMenu);
        tagMenu = new JComboBox<String>();
        // add items later (they aren't available now)
        tagMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String tag = (String) tagMenu.getSelectedItem();
                tagFilterChanged(tag);
            }
        });

        tagFilterPanel.add(tagMenu);

        // same packing behavior as text filter panel
        add(tagFilterPanel, c3);

        // spatial filter
        JPanel spatialFilterPanel = new JPanel();
        spatialFilterPanel.setLayout(new BoxLayout(spatialFilterPanel, BoxLayout.LINE_AXIS));
        spatialFilterPanel.add(new JLabel("Spatial filter: "));
        spatialFilterPanel.add(spatialFilterLabel);
        add(spatialFilterPanel, c3);

        loadWorkspace(null);
    }

    protected JPopupMenu createPopupMenu(TmNeuronMetadata neuron) {
        NeuronContextMenu menu = new NeuronContextMenu(neuron);
        menu.addMenuItems();
        return menu;
    }

    public void saveSelection() {
        int viewRow = neuronTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = neuronTable.convertRowIndexToModel(viewRow);
            savedSelectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
        } else {
            savedSelectedNeuron = null;
        }
    }
    
    public void restoreSelection() {
        if (savedSelectedNeuron != null) {
            int modelRow = neuronTableModel.getRowForNeuron(savedSelectedNeuron);
            if (modelRow >= 0) {
                int viewRow = neuronTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    neuronTable.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        }
    }

    /**
     * set the table selection to match the "current neuron" stored
     * by the AnnModel
     */
    private void syncSelection() {
        if (TmSelectionState.getInstance().getCurrentNeuron() != null) {
            int modelRow = neuronTableModel.getRowForNeuron(TmSelectionState.getInstance().getCurrentNeuron());
            if (modelRow >= 0) {
                int viewRow = neuronTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    neuronTable.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        }
    }

    /**
     * retrieve the current tags and update the drop-down menu
     */
    private void updateTagMenu() {
        // maintain the current menu state; the mode gets reset
        //  when you insert the "" item in the tag menu
        String currentSelection = (String) tagMenu.getSelectedItem();
        String currentMode = (String) tagModeMenu.getSelectedItem();
        tagMenu.removeAllItems();

        Set<String> tagSet = annotationModel.getAvailableNeuronTags();
        String[] tagList = tagSet.toArray(new String[tagSet.size()]);
        Arrays.sort(tagList);

        tagMenu.addItem("");
        for (String tag: tagList) {
            tagMenu.addItem(tag);
        }

        // reset menus
        if (tagSet.contains(currentSelection)) {
            tagMenu.setSelectedItem(currentSelection);
        }
        tagModeMenu.setSelectedItem(currentMode);
    }

    /**
     * update the table filter based on user text input in
     * the filter box; this is a filter based on text in
     * the table, done by Java, compared with a filter on
     * annotation information that we do explicitly above
     */
    private void updateRowFilter() {
        // we're only filtering on name and owner columns
        int[] filterColumns = {NeuronTableModel.COLUMN_NAME, NeuronTableModel.COLUMN_OWNER_NAME};
        RowFilter<TableModel, String> rowFilter = RowFilter.regexFilter("", filterColumns);
        try {
            // old: get regex from one text field
            // rowFilter = RowFilter.regexFilter(filterField.getText());

            // new: get desired filter and filter to ignore from two
            //  fields and combine them, carefully
            // note if include filter is empty, it's ok, but if
            //  exclude pattern is empty, don't use it

            String ignoreText = ignoreField.getText();
            String includeText = filterField.getText();

            // note: RowFilter.regexFilter() doesn't seem to like single-character
            //  patterns; we'll live with having to type two characters on
            //  the include side, but for the exclude side, I limited the
            //  ignore filter to prefixes so I can always force the pattern
            //  to be two characters, and it behaves predictably

            RowFilter includeFilter = RowFilter.regexFilter(includeText, filterColumns);
            RowFilter ignoreFilter = RowFilter.notFilter(RowFilter.regexFilter("^" + ignoreText, filterColumns));

            if (ignoreText.length() > 0 && includeText.length() > 0) {
                List<RowFilter<Object,Object>> filters = new ArrayList<>();
                filters.add(includeFilter);
                filters.add(ignoreFilter);
                rowFilter = RowFilter.andFilter(filters);
            } else if (ignoreText.length() > 0) {
                rowFilter = ignoreFilter;
            } else if (includeText.length() > 0) {
                rowFilter = includeFilter;
            } else {
                // no filter text = return the null filter as-is
            }

        } catch (java.util.regex.PatternSyntaxException e) {
            // if the regex doesn't parse, don't update the filter
            return;
        }
        sorter.setRowFilter(rowFilter);
        updateNeuronLabel();
        updateFilteredNeuronList();
    }

    private void updateFilteredNeuronList() {
        annotationModel.setCurrentFilteredNeuronList(this.getNeuronList());
    }

    private void updateNeuronLabel() {
        int showing = neuronTable.getRowCount();
        int loaded = neuronTableModel.getTotalNeuronCount();
        int total = annotationModel.getNumTotalNeurons();

        neuronLabel.setText(String.format("Neurons (%s/%s/%s)", showing, loaded, total));
        neuronLabel.setToolTipText(String.format("%s in table/%s in memory/%s total", showing, loaded, total));
    }

    public void updateNeuron (TmNeuronMetadata neuron) {
        neuronTableModel.updateNeuron(neuron);
    }

    public void updateNeuronSpatialFilter(boolean enabled, String description) {
        if (enabled) {
            spatialFilterLabel.setText(description);
        } else {
            spatialFilterLabel.setText("Disabled");
        }
    }

    /**
     * called when current neuron changes; both selects the neuron visually
     * as well as replaces the old neuron in the model with the new one
     */
    public void selectNeuron(TmNeuronMetadata tmNeuronMetadata) {
        if (tmNeuronMetadata == null) {
            return;
        }
        updateModel(tmNeuronMetadata);
        updateNeuronLabel();
        selectNeuronInTable(tmNeuronMetadata);
    }

    /**
     * select the row in the table for the input neuron; doesn't do
     * anything else (no other effects)
     */
    private void selectNeuronInTable(TmNeuronMetadata neuron) {
        int neuronModelRow = neuronTableModel.getRowForNeuron(neuron);

        if (neuronModelRow >= 0) {
            int neuronTableRow = neuronTable.convertRowIndexToView(neuronModelRow);
            if (neuronTableRow >= 0) {
                neuronTable.setRowSelectionInterval(neuronTableRow, neuronTableRow);
                neuronTable.scrollRectToVisible(new Rectangle(neuronTable.getCellRect(neuronTableRow, 0, true)));
            } else {
                // the row is in the model but not the table; clear the selection,
                //  which unfortunately won't return if the filter is cleared
                neuronTable.clearSelection();
            }
        } else {
            neuronTable.clearSelection();
        }
    }

    /**
     * called when tag mode drop-down is changed
     */
    public void tagModeChanged(String mode) {
        // note: if you set the mode to NONE, we don't change
        //  the tag; you might want to come back to it later

        // probably ought to have a constant map for this...
        NeuronTableModel.NeuronTagMode tagMode;
        if ("(none)".equals(mode)) {
            tagMode = NeuronTableModel.NeuronTagMode.NONE;
        } else if ("include".equals(mode)) {
            tagMode = NeuronTableModel.NeuronTagMode.INCLUDE;
        } else if ("exclude".equals(mode)) {
            tagMode = NeuronTableModel.NeuronTagMode.EXCLUDE;
        } else {
            // should never happen
            tagMode = NeuronTableModel.NeuronTagMode.NONE;
        }
        neuronTableModel.setTagMode(tagMode);
        updateNeuronLabel();
    }

    public void tagFilterChanged(String tag) {
        // note: if you set the tag filter to empty string,
        //  we explicitly change the mode to NONE as well
        if (StringUtils.isEmpty(tag)) {
            tagModeMenu.setSelectedItem("(none)");
            neuronTableModel.setTagFilter(tag, NeuronTableModel.NeuronTagMode.NONE);
        } else {
            neuronTableModel.setTagFilter(tag);
        }
        updateNeuronLabel();
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
                // always sort by creation date as a secondary key
                case ALPHABETICAL:
                      sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(NeuronTableModel.COLUMN_NAME, SortOrder.ASCENDING)));

                    break;
                case OWNER:
                    sorter.setSortKeys(Arrays.asList(
                        new RowSorter.SortKey(NeuronTableModel.COLUMN_OWNER_NAME, SortOrder.ASCENDING),
                        new RowSorter.SortKey(NeuronTableModel.COLUMN_CREATION_DATE, SortOrder.ASCENDING)
                    ));
                    break;
                case CREATIONDATE:
                    sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(NeuronTableModel.COLUMN_CREATION_DATE, SortOrder.ASCENDING)));
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
        if (workspace != null) {
            updateTagMenu();
        }
        updateFilteredNeuronList();
    }
    
   /**
     * add a new Neuron into the list
     */
    public void addNeuronToModel(TmNeuronMetadata neuron) {
        List neuronList = new ArrayList();
        neuronList.add(neuron);
        saveSelection();
        neuronTableModel.addNeurons(neuronList);
        restoreSelection();
        updateNeuronLabel();
        updateFilteredNeuronList();
    }

    /**
     * update the table model given a new workspace
     */
    private void updateModel(TmWorkspace workspace) {
        neuronTableModel.clear();
        if (workspace != null) {
            neuronTableModel.addNeurons(annotationModel.getNeuronList());
        }
        updateNeuronLabel();
    }
    
    public void deleteFromModel(TmNeuronMetadata neuron) {
        saveSelection();
        neuronTableModel.deleteNeuron(neuron);
        restoreSelection();
        updateFilteredNeuronList();
        updateNeuronLabel();
    }

    /**
     * update the table neuron with a new vernon of an
     * existing neuron (replaces in place)
     */
    public void updateModel(TmNeuronMetadata neuron) {
        saveSelection();
        neuronTableModel.updateNeuron(neuron);
        restoreSelection();
        updateFilteredNeuronList();
    }

    private void sendViewEvent(Vec3 location) {
        float[] microLocation = TmModelManager.getInstance().getLocationInMicrometers(location.getX(),
                location.getY(), location.getZ());
        TmModelManager.getInstance().getCurrentView().setCameraFocusX(location.getX());
        TmModelManager.getInstance().getCurrentView().setCameraFocusY(location.getY());
        TmModelManager.getInstance().getCurrentView().setCameraFocusZ(location.getZ());
        TmModelManager.getInstance().getCurrentView().setZoomLevel(500);
        ViewEvent event = new ViewEvent(this,microLocation[0],
                microLocation[1],
                microLocation[2],
        500, null, false);
        ViewerEventBus.postEvent(event);
    }

    private void onNeuronDoubleClicked(TmNeuronMetadata neuron) {
        if (neuron.getFirstRoot()!=null) {
            TmGeoAnnotation root = neuron.getFirstRoot();
            sendViewEvent(new Vec3(root.getX(), root.getY(), root.getZ()));
        }
    }

    public void neuronTagsChanged(Collection<TmNeuronMetadata> neuronList) {
        saveSelection();
        updateTagMenu();
        neuronTableModel.updateNeurons(neuronList);
        restoreSelection();
    }

    public void neuronsVisibilityChanged(Collection<TmNeuronMetadata> neuronList) {
        saveSelection();
        neuronTableModel.updateNeurons(neuronList);
        restoreSelection();
    }
    
    /**
     * return the list of currently visible neurons in the UI
     */
    public List<TmNeuronMetadata> getNeuronList() {
        List<TmNeuronMetadata> neuronList = new ArrayList<>();
        for (int i=0; i<neuronTable.getRowCount(); i++) {
            int index = neuronTable.convertRowIndexToModel(i);
            neuronList.add((TmNeuronMetadata) neuronTableModel.getNeuronAtRow(index));
        }
        return neuronList;
    }

    /**
     * return list of neurons that aren't currently visible
     * (useful for "hide others")
     */
    public List<TmNeuronMetadata> getUnshownNeuronList() {
        // get all indices, then remove the one that are visible
        Set<Integer> neuronIndexSet = new HashSet<>();
        for (int i=0; i<neuronTableModel.getRowCount(); i++) {
            neuronIndexSet.add(i);
        }
        for (int i=0; i<neuronTable.getRowCount(); i++) {
            int index = neuronTable.convertRowIndexToModel(i);
            neuronIndexSet.remove(index);
        }
        List<TmNeuronMetadata> neuronList = new ArrayList<>();
        for (Integer index: neuronIndexSet) {
            neuronList.add(neuronTableModel.getNeuronAtRow(index));
        }
        return neuronList;
    }

}

class NeuronTableModel extends AbstractTableModel {

    public enum NeuronTagMode {NONE, INCLUDE, EXCLUDE};

    public static int SYNC_WARN_LEVEL = 2;

    // note: creation date column and owner name column will be hidden
    // column names, tooltips, and associated methods should probably be static
    private String[] columnNames = {"Name", "O", "C", "V", "Creation Date", "Owner Name"};
    private String[] columnTooltips = {"Name", "Owner", "Color", "Visibility", "Creation Date", "Owner Name"};

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_OWNER_ICON = 1;
    public static final int COLUMN_COLOR = 2;
    public static final int COLUMN_VISIBILITY = 3;
    public static final int COLUMN_CREATION_DATE = 4;
    public static final int COLUMN_OWNER_NAME = 5;

    // this is the username for neurons that don't belong to anyone
    private static final String TRACERS_GROUP = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();

    private ArrayList<TmNeuronMetadata> neurons = new ArrayList<>();
    private ArrayList<TmNeuronMetadata> matchedNeurons = new ArrayList<>();
    private ArrayList<TmNeuronMetadata> unmatchedNeurons = new ArrayList<>();

    private String tagFilter = "";
    private NeuronTagMode tagMode = NeuronTagMode.NONE;

    // need this to retrieve colors, tags
    private NeuronManager annotationModel;

    // icons
    private ImageIcon visibleIcon;
    private ImageIcon invisibleIcon;
    private ImageIcon peopleIcon;
    private ImageIcon commonIcon;
    private TmModelManager modelManager;

    public void setAnnotationModel(NeuronManager annotationModel) {
        this.annotationModel = annotationModel;
        modelManager = TmModelManager.getInstance();
        // set up icons; yes, we have multiple storage places for icons...
        visibleIcon = SimpleIcons.getIcon("eye.png");
        invisibleIcon = SimpleIcons.getIcon("closed_eye.png");

        // I'm not super fond of either of these icons; user and computer
        //  are both kind of busy, even after I removed the color from them
        peopleIcon = Icons.getIcon("user_bw.png");
        commonIcon = Icons.getIcon("computer_bw.png");
    }
    
    public void clear() {
        neurons.clear();
        matchedNeurons.clear();
        unmatchedNeurons.clear();
        fireTableDataChanged();
    }

    public String getColumnTooltip(int column) {
        if (column >= 0 && column < getColumnCount()) {
            return columnTooltips[column];
        } else {
            return "";
        }
    }

    public void addNeurons(Collection<TmNeuronMetadata> neuronList) {
        neurons.addAll(neuronList);
        if (hasFilter()) {
            for (TmNeuronMetadata neuron: neuronList) {
                if (annotationModel.hasNeuronTag(neuron, tagFilter)) {
                    matchedNeurons.add(neuron);
                } else {
                    unmatchedNeurons.add(neuron);
                }
            }
        }
        fireTableDataChanged();
    }
    
    public void deleteNeuron(TmNeuronMetadata neuron) {
        // can't assume the neuron object is the same; this is
        //  inefficient, and I wish I'd stored the IDs with a
        //  map to the objects instead, but not worth changing right now:
        TmNeuronMetadata foundNeuron = null;
        Long neuronID = neuron.getId();
        for (TmNeuronMetadata testNeuron: neurons) {
            if (testNeuron.getId().equals(neuronID)) {
                foundNeuron = testNeuron;
                break;
            }
        }
        if (foundNeuron != null) {
            neurons.remove(foundNeuron);
            matchedNeurons.remove(foundNeuron);
            unmatchedNeurons.remove(foundNeuron);
            fireTableDataChanged();
        }
    }

    public void updateNeuron(TmNeuronMetadata neuron) {
        updateNeurons(Arrays.asList(neuron));
        // To optimize for performance, it would be better to do targeted updates:
        // fireTableRowsUpdated(firstRow, lastRow);
    }

    public void updateNeurons(Collection<TmNeuronMetadata> neuronList) {
        fireTableDataChanged();
    }
    
    // filter stuff

    public void setTagMode(NeuronTagMode mode) {
        setTagFilter(tagFilter, mode);
    }

    public void setTagFilter(String tag) {
        setTagFilter(tag, tagMode);
    }

    public void setTagFilter(String tag, NeuronTagMode mode) {
        // mode switch is cheap, but don't iterate over the
        //  neurons again if the tag didn't change
        tagMode = mode;
        if (!Objects.equals(tag,tagFilter)) {
            tagFilter = tag;
            matchedNeurons.clear();
            unmatchedNeurons.clear();
            for (TmNeuronMetadata neuron : neurons) {
                if (matchesTagFilter(neuron)) {
                    matchedNeurons.add(neuron);
                } else {
                    unmatchedNeurons.add(neuron);
                }
            }
        }
        fireTableDataChanged();
    }

    public boolean hasFilter() {
        return (tagMode != NeuronTagMode.NONE && !StringUtils.isEmpty(tagFilter));
    }

    private boolean matchesTagFilter(TmNeuronMetadata neuron) {
        return annotationModel.hasNeuronTag(neuron, tagFilter);
    }

    // boilerplate stuff
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public List<TmNeuronMetadata> getCurrentNeuronList() {
        if (hasFilter()) {
            if (tagMode == NeuronTagMode.INCLUDE) {
                return matchedNeurons;
            } else {
                return unmatchedNeurons;
            }
        } else {
            return neurons;
        }
    }
    
    public int getRowCount() {
        return getCurrentNeuronList().size();
    }

    public int getTotalNeuronCount() {
        return neurons.size();
    }

    public int getRowForNeuron(TmNeuronMetadata neuron) {
        return getIndexForNeuron(neuron, getCurrentNeuronList());
    }
    
    public TmNeuronMetadata getNeuronAtRow(int row) {
        if (row>=getCurrentNeuronList().size())
            return null;
        return getCurrentNeuronList().get(row);
    }

    private void replaceNeuron(TmNeuronMetadata neuron, List<TmNeuronMetadata> neuronList) {
        int index = getIndexForNeuron(neuron, neuronList);
        if (index >= 0) {
            // should always be present, but if it isn't,
            //  nothing we can do
            neuronList.set(index, neuron);
        }
    }

    private int getIndexForNeuron(TmNeuronMetadata neuron, List<TmNeuronMetadata> neuronList) {
        int i=0;
        for (TmNeuronMetadata n: neuronList) {
            if (n.getId().equals(neuron.getId())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    // needed to get color to work right; make sure classes match what getValueAt() returns!
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case COLUMN_NAME:
                // neuron
                return Integer.class;
            case COLUMN_OWNER_ICON:
                // owner
                // old: the string
                // return String.class;
                // new: an icon
                return ImageIcon.class;
            case COLUMN_COLOR:
                // color
                return Color.class;
            case COLUMN_VISIBILITY:
                // visibility; we use an image to indicate that:
                return ImageIcon.class;
            case COLUMN_CREATION_DATE:
                // creation date
                return Date.class;
            case COLUMN_OWNER_NAME:
                // OWNER NAME
                return String.class;
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
        }
    }
    
    public Object getValueAt(int row, int column) {
        TmNeuronMetadata targetNeuron = getNeuronAtRow(row);
        switch (column) {
            case COLUMN_NAME:
                // neuron name
                return targetNeuron.getName();
            case COLUMN_OWNER_ICON:
                // owner
                // old: string name
                // return targetNeuron.getOwnerName();
                // new, first alternative: icon
                String ownerKey = targetNeuron.getOwnerKey();
                if (ownerKey.equals(AccessManager.getAccessManager().getActualSubject().getKey())) {
                    // no icon if you're the owner
                    return null;
                } else if (ownerKey.equals(TRACERS_GROUP)) {
                    return commonIcon;
                } else {
                    // owned by someone else
                    return peopleIcon;
                }
            case COLUMN_COLOR:
                Color color = TmViewState.getColorForNeuron(targetNeuron.getId());
                if (color == null) {
                    if (targetNeuron.getColor()==null) {
                        color = TmViewState.generateNewColor(targetNeuron.getId());
                    } else {
                        color = targetNeuron.getColor();
                    }
                }
                return color;
            case COLUMN_VISIBILITY:
                if (!modelManager.getCurrentView().isHidden(targetNeuron.getId())) {
                    return visibleIcon;
                } else {
                    return invisibleIcon;
                }
            case COLUMN_CREATION_DATE:
                // creation date, hidden, but there for sorting
                return targetNeuron.getCreationDate();
            case COLUMN_OWNER_NAME:
                // owner name, hidden, but there for sorting
                return targetNeuron.getOwnerName();
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
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

        if (newColor!=null) {
            setBackground(newColor);
        }
        return this;
    }

}

class SyncLevelRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable tableData,
                                                   Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component cellComponent = super.getTableCellRendererComponent(
                tableData, value, isSelected, hasFocus, row, column);

        // draw attention to the neuron if its sync level gets too far from zero:
        int modelRow = tableData.convertRowIndexToModel(row);
        TmNeuronMetadata targetNeuron = ((NeuronTableModel) tableData.getModel()).getNeuronAtRow(modelRow);
        if (targetNeuron.getSyncLevel() < NeuronTableModel.SYNC_WARN_LEVEL) {
            cellComponent.setFont(cellComponent.getFont().deriveFont(Font.PLAIN));
        } else {
            cellComponent.setFont(cellComponent.getFont().deriveFont(Font.ITALIC));
        }
        return cellComponent;
    }
}