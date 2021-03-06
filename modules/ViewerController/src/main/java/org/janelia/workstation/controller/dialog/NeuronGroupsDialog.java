package org.janelia.workstation.controller.dialog;

import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.keybind.ShortcutTextField;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.*;

/**
 * Dialog for managing properties on groups of neurons
 * 
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronGroupsDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(NeuronGroupsDialog.class);
  
    private final JButton cancelButton;
    private final JButton okButton;
    private final JPanel groupsPanel;
    private final JPanel buttonPane;
    private final JTable bindingsTable;
    private final int COL_KEYBIND = 2;
    private final int COL_PROPTOGGLE = 3;
    
    public static final String PROPERTY_VISIBILITY = "Visibility";
    public static final String PROPERTY_RADIUS = "Radius";
    public static final String PROPERTY_READONLY = "Background";
    public static final String PROPERTY_CROSSCHECK = "Crosscheck";
    
   // private final AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
       
    public NeuronGroupsDialog() {
    	super(FrameworkAccess.getMainFrame());
    	
        setTitle("Edit Neuron Groups");

        groupsPanel = new JPanel();
        add(groupsPanel, BorderLayout.CENTER);
        
        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this window");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        okButton = new JButton("Save");
        okButton.setToolTipText("Adjust neuron group properties");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (update()) {
                        setVisible(false);
                    }
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException(ex);
                }
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        
        NeuronGroupsTableModel tableModel = new NeuronGroupsTableModel();
        NeuronManager neuronManager = NeuronManager.getInstance();
        TmNeuronTagMap currentTagMap = TmModelManager.getInstance().getCurrentTagMap();
        tableModel.loadTable(neuronManager.getAllNeuronTags(), currentTagMap.getAllTagGroupMappings());
        bindingsTable = new JTable(tableModel); 
        bindingsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        TableColumn col = bindingsTable.getColumnModel().getColumn(COL_KEYBIND);
        col.setCellEditor(new KeymapCellEditor());        
        col = bindingsTable.getColumnModel().getColumn(COL_PROPTOGGLE);
        col.setCellEditor(new KeymapCellEditor());
        groupsPanel.add(new JScrollPane(bindingsTable));
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    
    public void showDialog() {   

        
        packAndShow();
    }
    
    private boolean update() throws Exception {
        // get the tag metadata and update the model
        NeuronManager neuronManager = NeuronManager.getInstance();
        neuronManager.saveTagMeta(((NeuronGroupsTableModel)bindingsTable.getModel()).saveTable());
       
        log.info("Neuron Group properties updated");
        return true;
    }
    
    class NeuronGroupsTableModel extends AbstractTableModel {
        String[] columnNames = {"Neuron Group",
                                "# of members",
                                "Toggle Hot-key",
                                "Property To Toggle"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        Map<String,Map<String,Object>> metaData;
        
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }
        
        public void loadTable(Set<String> groupNames, Map<String,Map<String,Object>> tagMeta) {
            metaData = tagMeta;
            Iterator<String> neuronGroupNames = groupNames.iterator(); 
            while (neuronGroupNames.hasNext()) {
                 String groupName = neuronGroupNames.next();
                 List row = new ArrayList<Object>();
                 row.add(groupName);
                 NeuronManager neuronManager = NeuronManager.getInstance();
                 row.add(neuronManager.getNeuronsForTag(groupName).size());
                 Map<String,Object> tagMappings = metaData.get(groupName);
                 if (tagMappings!=null && tagMappings.get("keymap")!=null) {
                     row.add(tagMappings.get("keymap"));
                     row.add(tagMappings.get("toggleprop"));
                 } else {
                     row.add("");
                     row.add("");
                 }
                 data.add(row);
             }
        }
        
        public Map<String, Map<String,Object>> saveTable() {
            return metaData;
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 2) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            String groupName = (String)data.get(row).get(0);
            Map<String,Object> meta = metaData.get(groupName);
            
                    System.out.println (value +"," + row + "," + col);
            if (meta==null)
                meta = new HashMap<String,Object>();
            switch (col) {
                case COL_KEYBIND:
                    meta.put("keymap", value);
                    data.get(row).set(col, value);
                    break;
                case COL_PROPTOGGLE:
                    meta.put("toggleprop", value);
                    data.get(row).set(col, value);
                    break;
            }
            metaData.put(groupName, meta);
            fireTableCellUpdated(row, col);
        }
    }
    

    class KeymapCellEditor extends AbstractCellEditor implements TableCellEditor {
        // This is the component that will handle the editing of the cell value

        JComponent component;
        int cellType;

        // This method is called when a cell value is edited by the user.
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int rowIndex, int vColIndex) {
            cellType = vColIndex;
            // Configure the component with the specified value
            switch (vColIndex) {
                case COL_KEYBIND:
                    component = new ShortcutTextField();
                    ((ShortcutTextField) component).setText((String) value);
                     return ((ShortcutTextField)component);
                case COL_PROPTOGGLE:
                    String[] propertyStrings = { PROPERTY_RADIUS, PROPERTY_VISIBILITY, PROPERTY_READONLY, PROPERTY_CROSSCHECK };
                    component = new JComboBox(propertyStrings);
                    ((JComboBox)component).addItemListener(new ItemChangeListener(table.getModel(), rowIndex, vColIndex));
                    if (value==PROPERTY_VISIBILITY) {
                        ((JComboBox)component).setSelectedIndex(0);
                    } else if (value==PROPERTY_RADIUS) {
                        ((JComboBox)component).setSelectedIndex(1);
                    } else if (value==PROPERTY_READONLY) {
                        ((JComboBox)component).setSelectedIndex(2);
                    }  else if (value==PROPERTY_CROSSCHECK) {
                        ((JComboBox)component).setSelectedIndex(3);
                    } 
                    return ((JComboBox)component);
            }
            return null;
        }

        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() {
             switch (cellType) {
                case COL_KEYBIND:
                    return ((ShortcutTextField) component).getText();
                case COL_PROPTOGGLE:
                    return ((JComboBox) component).getSelectedItem();
            }
            return null;
        }
    }
    
    // extra for cases where the save button is pressed without clicking on another table cell first
    class ItemChangeListener implements ItemListener {
        TableModel model;
        int row;
        int col;
        
        public ItemChangeListener(TableModel model, int row, int col) {
            this.model = model;
            this.row = row;
            this.col = col;
        }

        @Override
        public void itemStateChanged(ItemEvent event) {
           Object item = event.getItem();
           model.setValueAt(item, row, col);
        }     
    }
}
