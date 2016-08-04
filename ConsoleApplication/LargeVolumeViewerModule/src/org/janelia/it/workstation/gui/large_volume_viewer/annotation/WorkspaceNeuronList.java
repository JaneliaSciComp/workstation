package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NeuronSelectedListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * this widget displays a list of neurons in a workspace
 *
 * djo, 12/13
 */
public class WorkspaceNeuronList extends JPanel {

    private JTable neuronTable;
    private NeuronTableModel neuronTableModel;
    private DefaultRowSorter<TableModel, String> sorter;
    private JTextField filterField;
    private JComboBox tagModeMenu;
    private JComboBox tagMenu;

    private AnnotationManager annotationManager;
    private AnnotationModel annotationModel;
    private CameraPanToListener panListener;
    private NeuronSelectedListener neuronSelectedListener;

    private int width;
    private static final int height = 2 * AnnotationPanel.SUBPANEL_STD_HEIGHT;

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


        // tag filter
        JPanel tagFilterPanel = new JPanel();
        tagFilterPanel.setLayout(new BoxLayout(tagFilterPanel, BoxLayout.LINE_AXIS));
        tagFilterPanel.add(new JLabel("Tag filter:"));

        String [] modeStrings = {"(none)", "include", "exclude"};
        tagModeMenu = new JComboBox(modeStrings);
        tagModeMenu.setSelectedIndex(0);
        tagModeMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                String mode = (String) box.getSelectedItem();
                tagModeChanged(mode);
            }
        });
        tagFilterPanel.add(tagModeMenu);
        tagMenu = new JComboBox();
        // add items later (they aren't available now)
        tagMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                String tag = (String) box.getSelectedItem();
                tagFilterChanged(tag);
            }
        });

        tagFilterPanel.add(tagMenu);

        // same packing behavior as text filter panel
        add(tagFilterPanel, c3);

        loadWorkspace(null);
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

        Set<String> tagSet = annotationModel.getAllTags();
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
        RowFilter<TableModel, String> rowFilter = null;
        try {
            rowFilter = RowFilter.regexFilter(filterField.getText());
        } catch (java.util.regex.PatternSyntaxException e) {
            // if the regex doesn't parse, don't update the filter
            return;
        }
        sorter.setRowFilter(rowFilter);
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
        if (mode == "(none)") {
            tagMode = NeuronTableModel.NeuronTagMode.NONE;
        } else if (mode == "include") {
            tagMode = NeuronTableModel.NeuronTagMode.INCLUDE;
        } else if (mode == "exclude") {
            tagMode = NeuronTableModel.NeuronTagMode.EXCLUDE;
        } else {
            // should never happen
            tagMode = NeuronTableModel.NeuronTagMode.NONE;
        }
        neuronTableModel.setTagMode(tagMode);
    }

    public void tagFilterChanged(String tag) {
        // note: if you set the tag filter to empty string,
        //  we explicitly change the mode to NONE as well
        if (tag == "") {
            tagModeMenu.setSelectedItem("(none)");
            neuronTableModel.setTagFilter(tag, NeuronTableModel.NeuronTagMode.NONE);
        } else {
            neuronTableModel.setTagFilter(tag);
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
        if (workspace != null) {
            updateTagMenu();
        }
    }

    /**
     * update the table model given a new workspace
     */
    private void updateModel(TmWorkspace workspace) {
        neuronTableModel.clear();
        if (workspace != null) {
            neuronTableModel.addNeurons(workspace.getNeuronList());
        }
    }

    /**
     * update the table neuron with a new version of an
     * existing neuron (replaces in place)
     */
    private void updateModel(TmNeuron neuron) {
        neuronTableModel.updateNeuron(neuron);
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

    public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style) {
        updateModel(neuron);
    }

    public void neuronStylesChanged(List<TmNeuron> neuronList, NeuronStyle style) {
        for (TmNeuron neuron: neuronList) {
            updateModel(neuron);
        }
    }

    public void neuronTagsChanged(List<TmNeuron> neuronList) {
        updateTagMenu();
        for (TmNeuron neuron: neuronList) {
            neuronTableModel.updateNeuron(neuron);
        }
    }

}

class NeuronTableModel extends AbstractTableModel {

    public enum NeuronTagMode {NONE, INCLUDE, EXCLUDE};

    // note: creation date column will be hidden
    private String[] columnNames = {"Name", "Style", "Creation Date"};

    private ArrayList<TmNeuron> neurons = new ArrayList<>();
    private ArrayList<TmNeuron> matchedNeurons = new ArrayList<>();
    private ArrayList<TmNeuron> unmatchedNeurons = new ArrayList<>();

    private String tagFilter = "";
    private NeuronTagMode tagMode = NeuronTagMode.NONE;

    // need this to retrieve colors, tags
    private AnnotationModel annotationModel;

    public void setAnnotationModel(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }

    public void clear() {
        neurons.clear();
        matchedNeurons.clear();
        unmatchedNeurons.clear();
        fireTableDataChanged();
    }

    public void addNeuron(TmNeuron neuron) {
        neurons.add(neuron);
        if (hasFilter()) {
            if (annotationModel.hasTag(neuron, tagFilter)) {
                matchedNeurons.add(neuron);
            }
        }
        fireTableDataChanged();
    }

    public void addNeurons(List<TmNeuron> neuronList) {
        neurons.addAll(neuronList);
        if (hasFilter()) {
            for (TmNeuron neuron: neuronList) {
                if (annotationModel.hasTag(neuron, tagFilter)) {
                    matchedNeurons.add(neuron);
                } else {
                    unmatchedNeurons.add(neuron);
                }
            }
        }
        fireTableDataChanged();
    }

    public void updateNeuron(TmNeuron neuron) {
        replaceNeuron(neuron, neurons);
        if (hasFilter()) {
            if (matchesTagFilter(neuron)) {
                replaceNeuron(neuron, matchedNeurons);
            } else {
                replaceNeuron(neuron, unmatchedNeurons);
            }
        }
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
        if (tag != tagFilter) {
            tagFilter = tag;
            matchedNeurons.clear();
            unmatchedNeurons.clear();
            for (TmNeuron neuron : neurons) {
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
        return (tagMode != NeuronTagMode.NONE && tagFilter != "");
    }

    private boolean matchesTagFilter(TmNeuron neuron) {
        return annotationModel.hasTag(neuron, tagFilter);
    }

    // boilerplate stuff
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (hasFilter()) {
            if (tagMode == NeuronTagMode.INCLUDE) {
                return matchedNeurons.size();
            } else {
                return unmatchedNeurons.size();
            }
        } else {
            return neurons.size();
        }
    }

    public TmNeuron getNeuronAtRow(int row) {
        if (hasFilter()) {
            if (tagMode == NeuronTagMode.INCLUDE) {
                return matchedNeurons.get(row);
            } else {
                return unmatchedNeurons.get(row);
            }
        } else {
            return neurons.get(row);
        }
    }

    private void replaceNeuron(TmNeuron neuron, List<TmNeuron> neuronList) {
        int index = getIndexForNeuron(neuron, neuronList);
        if (index >= 0) {
            neuronList.set(index, neuron);
        } else {


            // error, should never happen; what to do?



        }
    }

    public int getIndexForNeuron(TmNeuron neuron, List<TmNeuron> neuronList) {
        TmNeuron foundNeuron = null;
        for (TmNeuron n: neuronList) {
            if (n.getId().equals(neuron.getId())) {
                foundNeuron = n;
                break;
            }
        }
        if (foundNeuron != null) {
            return neuronList.indexOf(foundNeuron);
        } else {
            return -1;
        }
    }

    public int getRowForNeuron(TmNeuron neuron) {
        List<TmNeuron> neuronList;
        if (hasFilter()) {
            if (tagMode == NeuronTagMode.INCLUDE) {
                neuronList = matchedNeurons;
            } else {
                // EXCLUDE:
                neuronList = unmatchedNeurons;
            }
        } else {
            neuronList = neurons;
        }
        return getIndexForNeuron(neuron, neuronList);
    }

    // needed to get color to work right; make sure classes match what getValueAt() returns!
    public Class getColumnClass(int column) {
        switch (column) {
            case 0:
                // neuron
                return TmNeuron.class;
            case 1:
                // color
                return Color.class;
            case 2:
                // creation date
                return Date.class;
            default:
                return Object.class;
        }
    }

    public Object getValueAt(int row, int column) {
        TmNeuron targetNeuron;
        if (hasFilter()) {
            if (tagMode == NeuronTagMode.INCLUDE) {
                targetNeuron = matchedNeurons.get(row);
            } else {
                targetNeuron = unmatchedNeurons.get(row);
            }
        } else {
            targetNeuron = neurons.get(row);
        }

        switch (column) {
            case 0:
                // neuron itself, which will display as name
                return targetNeuron;
            case 1:
                // color, from style
                return annotationModel.getNeuronStyle(targetNeuron).getColor();
            case 2:
                // creation date, hidden, but there for sorting
                return targetNeuron.getCreationDate();
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

