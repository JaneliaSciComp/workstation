package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyDescriptor;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.janelia.model.security.*;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class NewGroupPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(NewGroupPanel.class);
    
    private AdministrationTopComponent parent;
    private NewGroupTableModel newGroupTableModel;
    private JTable newGroupTable;
    private Group currentGroup;
    private JButton saveUserButton;
    private int COLUMN_NAME = 0;
    private int COLUMN_VALUE = 1;
    
    public NewGroupPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
    }
    
    public void setupUI() {

        setLayout(new BorderLayout());

        JPanel titlePanel = new TitlePanel("User List", "Return To Group List", event -> returnHome());
        add(titlePanel, BorderLayout.PAGE_START);
        
        newGroupTableModel = new NewGroupTableModel();
        newGroupTable = new JTable(newGroupTableModel);
        TableFieldEditor editor = new TableFieldEditor();  
        newGroupTable.setRowHeight(25);
        JScrollPane userTableScroll = new JScrollPane(newGroupTable);
        add(newGroupTable, BorderLayout.CENTER);

        // add groups pulldown selection for groups this person is a member of
        saveUserButton = new JButton("Create New Group");
        saveUserButton.addActionListener(event -> createGroup());
        saveUserButton.setEnabled(false);

        JPanel actionPanel = new ActionPanel();
        actionPanel.add(saveUserButton);
        add(actionPanel, BorderLayout.PAGE_END);
    }
    
    public void createGroup () {
        // create key from name
        currentGroup.setKey("group:"+currentGroup.getName());
        log.info(currentGroup.toString());
        parent.createGroup(currentGroup);

        // set the current user as the owner of the group
        Subject rawAdmin = AccessManager.getAccessManager().getActualSubject();
        if (rawAdmin instanceof User) {
            User admin = (User) rawAdmin;
            Set<UserGroupRole> roles = admin.getUserGroupRoles();
            UserGroupRole newRole = new UserGroupRole(currentGroup.getKey(), GroupRole.Owner);
            roles.add(newRole);
            admin.setUserGroupRoles(roles);
            parent.saveUserRoles(admin);
        }
        returnHome();
    }
    
    public void initNewGroup() throws Exception {
        currentGroup = new Group();
        // load new group
        newGroupTableModel.loadGroup(currentGroup);
    }
                
    public void returnHome() {
        parent.viewGroupList();
    }
    
    class NewGroupTableModel extends AbstractTableModel {
        String[] columnNames = {"Property","Value"};
        Group group;
        String[] editProperties = {"LdapGroupName", "Name", "FullName"};
        String[] editLabels = {"LdapGroupName", "Name", "FullName"};
        String[] values = new String[editProperties.length];
                
        @Override
        public boolean isCellEditable(int row, int col) {
            return col != COLUMN_NAME;
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

        private void validateGroupChanges() {
            if (group.getFullName()!=null && group.getName()!=null)
                saveUserButton.setEnabled(true);
            else
                saveUserButton.setEnabled(false);
        }

        public String getColumnName(int col) {            
            return columnNames[col];            
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) 
                return editLabels[row];
            else 
                return values[row];
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            try {
                String finalVal = (String)value;
                new PropertyDescriptor(editProperties[row], Group.class).getWriteMethod().invoke(group, finalVal);
                values[row] = finalVal;
                validateGroupChanges();
                this.fireTableCellUpdated(row, col);
            }
            catch (Exception ex) {
                FrameworkAccess.handleException("Problem updating user information using reflection", ex);
            }
        }

        public Class getColumnClass(int c) {
            return String.class;               
        }
    }
    
    private static class TableFieldEditor extends AbstractCellEditor implements TableCellEditor {

        private JTextField field;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.field = new JTextField((String) value);
            return field;
        }     

        @Override
        public Object getCellEditorValue() {
            return field.getText();
        }
         
    }
}
