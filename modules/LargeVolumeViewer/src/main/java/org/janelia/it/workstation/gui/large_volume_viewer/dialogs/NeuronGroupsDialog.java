package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractCellEditor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.keybind.ShortcutTextField;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private final AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
       
    public NeuronGroupsDialog() {
    	super(FrameworkImplProvider.getMainFrame());
    	
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
                    FrameworkImplProvider.handleException(ex);
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
        AnnotationModel annModel = annotationMgr.getAnnotationModel();
        tableModel.loadTable(annModel.getAllNeuronTags(), annModel.getTagGroupMappings());
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
        AnnotationModel annModel = annotationMgr.getAnnotationModel();
        annModel.saveTagMeta(((NeuronGroupsTableModel)bindingsTable.getModel()).saveTable());
       
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
                 AnnotationModel annModel = annotationMgr.getAnnotationModel();
                 row.add(annModel.getNeuronsForTag(groupName).size());
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
