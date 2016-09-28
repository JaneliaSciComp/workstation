package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraPanToListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NeuronSelectedListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.util.MouseHandler;

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
    private JComboBox<String> tagModeMenu;
    private JComboBox<String> tagMenu;

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
        neuronLabel =new JLabel("Neurons", JLabel.LEADING);
        add(neuronLabel, c);


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

        neuronTable.addMouseListener(new MouseHandler() {
            
            @Override
            protected void popupTriggered(MouseEvent me) {
                if (me.isConsumed()) return;
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int modelRow = neuronTable.convertRowIndexToModel(viewRow);
                    TmNeuronMetadata selectedNeuron = neuronTableModel.getNeuronAtRow(modelRow);
                    // select neuron
                    if (neuronSelectedListener != null) {
                        neuronSelectedListener.selectNeuron(selectedNeuron);
                        // show popup menu for the selected neuron
                        JPopupMenu popupMenu = createPopupMenu(me);
                        if (popupMenu!=null) {
                            popupMenu.show(me.getComponent(), me.getX(), me.getY());
                        }
                        me.consume();
                    }
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
                    if (modelColumn == 0) {
                        // single click name, select neuron
                        if (neuronSelectedListener != null)
                            neuronSelectedListener.selectNeuron(selectedNeuron);
                    } 
                    else if (modelColumn == 1) {
                        // single click color, edit style
                        annotationManager.chooseNeuronStyle(selectedNeuron);
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
        tagModeMenu = new JComboBox<String>(modeStrings);
        tagModeMenu.setSelectedIndex(0);
        tagModeMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> box = (JComboBox) e.getSource();
                String mode = (String) box.getSelectedItem();
                tagModeChanged(mode);
            }
        });
        tagFilterPanel.add(tagModeMenu);
        tagMenu = new JComboBox<String>();
        // add items later (they aren't available now)
        tagMenu.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> box = (JComboBox) e.getSource();
                String tag = (String) box.getSelectedItem();
                tagFilterChanged(tag);
            }
        });

        tagFilterPanel.add(tagMenu);

        // same packing behavior as text filter panel
        add(tagFilterPanel, c3);

        loadWorkspace(null);
    }

    protected JPopupMenu createPopupMenu(MouseEvent me) {
        NeuronContextMenu menu = new NeuronContextMenu(annotationManager, annotationManager.getAnnotationModel().getCurrentNeuron());
//        return menu.getMenu();
        menu.addMenuItems();
        return menu;
    }

    private int savedSelection;
    
    public void saveSelection() {
        this.savedSelection = neuronTable.getSelectedRow();
    }
    
    public void restoreSelection() {
        if (savedSelection>0  && savedSelection<neuronTable.getRowCount()) {
            neuronTable.setRowSelectionInterval(savedSelection, savedSelection);
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
        RowFilter<TableModel, String> rowFilter = null;
        try {
            rowFilter = RowFilter.regexFilter(filterField.getText());
        } catch (java.util.regex.PatternSyntaxException e) {
            // if the regex doesn't parse, don't update the filter
            return;
        }
        sorter.setRowFilter(rowFilter);
        updateNeuronLabel();
    }

    private void updateNeuronLabel() {
        neuronLabel.setText(String.format("Neurons (showing %s/%s)",
            neuronTable.getRowCount(), neuronTableModel.getTotalNeuronCount()));
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
        int neuronModelRow = neuronTableModel.getRowForNeuron(tmNeuronMetadata);

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
            neuronTableModel.addNeurons(annotationModel.getNeuronList());
        }
        updateNeuronLabel();
    }

    /**
     * update the table neuron with a new version of an
     * existing neuron (replaces in place)
     */
    private void updateModel(TmNeuronMetadata neuron) {
        neuronTableModel.updateNeuron(neuron);
    }

    private void onNeuronDoubleClicked(TmNeuronMetadata neuron) {
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

    public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
        saveSelection();
        neuronTableModel.updateNeuron(neuron);
        updateNeuronLabel();
        restoreSelection();
    }

    public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
        saveSelection();
        List<TmNeuronMetadata> neuronList = new ArrayList<>();
        for (TmNeuronMetadata tmNeuronMetadata: neuronStyleMap.keySet()) {
            neuronList.add(annotationModel.getNeuronFromNeuronID(tmNeuronMetadata.getId()));
        }
        neuronTableModel.updateNeurons(neuronList);
        updateNeuronLabel();
        restoreSelection();
    }

    public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {
        saveSelection();
        updateTagMenu();
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
}

class NeuronTableModel extends AbstractTableModel {

    public enum NeuronTagMode {NONE, INCLUDE, EXCLUDE};

    // note: creation date column will be hidden
    private String[] columnNames = {"Name", "Style", "Creation Date"};

    private ArrayList<TmNeuronMetadata> neurons = new ArrayList<>();
    private ArrayList<TmNeuronMetadata> matchedNeurons = new ArrayList<>();
    private ArrayList<TmNeuronMetadata> unmatchedNeurons = new ArrayList<>();

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

    public void addNeurons(List<TmNeuronMetadata> neuronList) {
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

    public void updateNeuron(TmNeuronMetadata neuron) {
        updateNeurons(Arrays.asList(neuron));
    }

    public void updateNeurons(List<TmNeuronMetadata> neuronList) {
        if (hasFilter()) {
            for (TmNeuronMetadata neuron: neuronList) {
                replaceNeuron(neuron, neurons);
                if (matchesTagFilter(neuron)) {
                    replaceNeuron(neuron, matchedNeurons);
                } else {
                    replaceNeuron(neuron, unmatchedNeurons);
                }
            }
        }
        else {
            for (TmNeuronMetadata neuron: neuronList) {
                replaceNeuron(neuron, neurons);
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
            case 0:
                // neuron
                return TmNeuronMetadata.class;
            case 1:
                // color
                return Color.class;
            case 2:
                // creation date
                return Date.class;
            default:
                throw new IllegalStateException("Table column is not configured: "+column);
        }
    }
    
    public Object getValueAt(int row, int column) {
        TmNeuronMetadata targetNeuron = getNeuronAtRow(row);
        switch (column) {
            case 0:
                // neuron name
                return targetNeuron.getName();
            case 1:
                // Note that is not the same as targetNeuron.getColor(). If the persisted color is null, it picks a default.
                return annotationModel.getNeuronStyle(targetNeuron).getColor();
            case 2:
                // creation date, hidden, but there for sorting
                return targetNeuron.getCreationDate();
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
            setToolTipText("RGB value: " + newColor.getRed() + ", "
                    + newColor.getGreen() + ", "
                    + newColor.getBlue());
        }
        return this;
    }

}

