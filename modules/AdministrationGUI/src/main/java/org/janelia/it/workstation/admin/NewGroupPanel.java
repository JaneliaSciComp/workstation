package org.janelia.it.workstation.admin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.model.security.Group;
import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class NewGroupPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(NewGroupPanel.class);
    
    AdministrationTopComponent parent;
    NewGroupTableModel newGroupTableModel;
    JTable newGroupTable;
    Group currentGroup;
    private int COLUMN_NAME = 0;
    private int COLUMN_VALUE = 1;
    
    public NewGroupPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
    }
    
    public void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        JPanel titlePanel = new JPanel();
        JButton returnHome = new JButton("return to group list");
        returnHome.setActionCommand("ReturnHome");
        returnHome.addActionListener(event -> returnHome());
        returnHome.setBorderPainted(false);
        returnHome.setOpaque(false);
        titlePanel.add(returnHome);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        JLabel titleLabel = new JLabel("Group Details");  
        titlePanel.add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        titlePanel.add(titleLabel);
        add(titlePanel);
        
        newGroupTableModel = new NewGroupTableModel();
        newGroupTable = new JTable(newGroupTableModel);
        TableFieldEditor editor = new TableFieldEditor();  
        newGroupTable.setRowHeight(25);
        JScrollPane userTableScroll = new JScrollPane(newGroupTable);
        add(newGroupTable); 
        
       
        // add groups pulldown selection for groups this person is a member of
        JButton saveUserButton = new JButton("Create New Group");
        saveUserButton.addActionListener(event -> createGroup());
        JPanel actionPanel = new JPanel();
//        newGroupPanel.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        actionPanel.add(saveUserButton);
        add(actionPanel);               
    }
    
    public void createGroup () {
        parent.createGroup(currentGroup);
        returnHome();
    }
    
    public void initNewGroup() throws Exception {
        currentGroup = new Group();
        Subject rawAdmin = AccessManager.getAccessManager().getActualSubject();
        if (rawAdmin instanceof User) {                
            User admin = (User) rawAdmin;
            
            // load new group
            newGroupTableModel.loadGroup(currentGroup);                      
        }
    }
                
    public void returnHome() {
        parent.viewGroupList();
    }
    
    class NewGroupTableModel extends AbstractTableModel {
        String[] columnNames = {"Property","Value"};
        Group group;
        String password;
        String[] editProperties = {"LdapGroupName", "Key", "FullName"};
        String[] values = new String[editProperties.length];
                
        @Override
        public boolean isCellEditable(int row, int col) {
            if (col!=COLUMN_NAME)
                return true;
            return false;
        }
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return editProperties.length;
        }
        
        public void loadGroup(Group group) throws Exception {
            this.group = group;
            for (int i=0; i<editProperties.length; i++) {
                String value;
                value = (String) new PropertyDescriptor(editProperties[i], Group.class).getReadMethod().invoke(group);
                values[i] = value;                        
            }
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {            
            return columnNames[col];            
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) 
                return editProperties[row]; 
            else 
                return values[row];
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            try {
                String finalVal = (String)value;
                if (editProperties[row].equals("Key") && !finalVal.startsWith("group")) {
                    finalVal = "group:" + value;
                }
                new PropertyDescriptor(editProperties[row], Group.class).getWriteMethod().invoke(group, finalVal);
                values[row] = (String)finalVal;
                this.fireTableCellUpdated(row, col);
            } catch (Exception ex) {
                ex.printStackTrace();
                String errorMessage = "Problem updating user information using reflection";
                log.error(errorMessage);
            }
        }

        public Class getColumnClass(int c) {
            return String.class;               
        }

       
        
    }
    
    private static class TableFieldEditor extends AbstractCellEditor implements TableCellEditor {
        JTextField field;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {             
            field = new JTextField((String) value);

            return field;
        }     
                    

        @Override
        public Object getCellEditorValue() {
            return field.getText();
        }
         
    }
}
