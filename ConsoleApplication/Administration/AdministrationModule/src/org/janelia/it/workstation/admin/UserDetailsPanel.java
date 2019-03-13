package org.janelia.it.workstation.admin;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
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
public class UserDetailsPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    
    AdministrationTopComponent parent;
    JComboBox newGroupSelector;
    UserDetailsTableModel userDetailsTableModel;
    JTable userDetailsTable;
    GroupRolesModel groupRolesModel;
    JTable groupRolesTable;
    private int COLUMN_NAME = 0;
    private int COLUMN_VALUE = 1;
    
    public UserDetailsPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
    }
    
    public void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        // display table of user editable attributes
        userDetailsTableModel = new UserDetailsTableModel();
        userDetailsTable = new JTable(userDetailsTableModel);
        TableFieldEditor editor = new TableFieldEditor();
        userDetailsTable.getColumn("Property").setCellEditor(editor);       
        JScrollPane userTableScroll = new JScrollPane(userDetailsTable);
        add(userTableScroll); 
        
        
        // show group edit table with permissions for that group
        groupRolesModel = new GroupRolesModel();
        groupRolesTable = new JTable(groupRolesModel);
        TableSelectBox groupSelectBox = new TableSelectBox();
        groupRolesTable.getColumn("Role").setCellEditor(groupSelectBox);
        groupRolesTable.getColumn("Role").setCellRenderer(groupSelectBox);    
        JScrollPane groupRolesScroll = new JScrollPane(groupRolesTable);
        add(groupRolesScroll); 
        
        // add groups pulldown selection for groups this person is a member of
        newGroupSelector = new JComboBox();
        JButton newGroupButton = new JButton("Add New Group");
        newGroupButton.addActionListener(event -> addNewGroup());
        JPanel newGroupPanel = new JPanel();
//        newGroupPanel.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        newGroupPanel.add(newGroupSelector);
        newGroupPanel.add(newGroupButton);
        add(newGroupPanel);               
    }
    
    public void editUserDetails(User user) throws Exception {
        Subject rawAdmin = AccessManager.getAccessManager().getActualSubject();
        if (rawAdmin instanceof User) {                
            User admin = (User) rawAdmin;
            
            // load user details
            userDetailsTableModel.loadUser(user);           
            groupRolesModel.loadGroupRoles(user);
            
            // load up the new groups available to add to this user
            newGroupSelector.removeAllItems();
            Iterator<UserGroupRole> groupRoles = admin.getUserGroupRoles().iterator();
            while (groupRoles.hasNext()) {
                UserGroupRole groupRole = groupRoles.next();
                if (groupRole.getRole() == GroupRole.Admin
                        || groupRole.getRole() == GroupRole.Owner 
                        || AccessManager.getAccessManager().isAdmin()) {
                    if (user.getRole(groupRole.getGroupKey()) == null) {
                        newGroupSelector.addItem(groupRole.getGroupKey());
                    }
                }
            }            
        }
    }
                
    
    // adds the user to a new group   
    private void addNewGroup() {
        String groupKey = (String)newGroupSelector.getSelectedItem();
        groupRolesModel.addNewGroup(groupKey);        
    }
    
    class UserDetailsTableModel extends AbstractTableModel implements ActionListener {
        String[] columnNames = {"Property","Value"};
        User user;
        String[] editProperties = {"Name", "Email"};
        String[] values = new String[editProperties.length];
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return editProperties.length;
        }
        
        public void loadUser(User user) throws Exception {
            this.user = user;
            for (int i=0; i<editProperties.length; i++) {
               String value = (String)new PropertyDescriptor(editProperties[i], User.class).getReadMethod().invoke(user);
               values[i] = value;                        
            }
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {
                return editProperties[row];               
            } else 
                return values[row];
        }

        public Class getColumnClass(int c) {
            return String.class;               
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String newValue = ((JTextField)e.getSource()).getText();
            if (newValue!=null) {
                try {
                    new PropertyDescriptor(e.getActionCommand(), User.class).getWriteMethod().invoke(user, newValue);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMessage = "Problem updating user information using reflection";
                    log.error(errorMessage);
                }
            }
        }
        
    }
    
    class GroupRolesModel extends AbstractTableModel implements ActionListener {
        String[] columnNames = {"Group Name","Role"};
        String[] roleOptions = { "Owner", "Admin", "Writer", "Reader"};
        List<UserGroupRole> data = new ArrayList<>();
        User user;
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            if (col!=COLUMN_NAME)
                return true;
            return false;
        }
        
        public void loadGroupRoles(User user) {
            data = new ArrayList<>();
            this.user = user;
            data.addAll(user.getUserGroupRoles());
            fireTableRowsInserted(0, getRowCount()-1);
        }
        
        public void addNewGroup(String groupKey) {
            user.setUserGroupRole(groupKey, GroupRole.Reader);
            data.add(user.getRole(groupKey));            
            fireTableRowsInserted(getRowCount()-2, getRowCount()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {                
                return data.get(row).getGroupKey();
            } else {
                JComboBox roleSelection = new JComboBox(roleOptions);
                roleSelection.setActionCommand(data.get(row).getGroupKey());
                roleSelection.addActionListener(this);
                roleSelection.setSelectedItem(data.get(row).getRole().toString());
                return roleSelection;
            }
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UserGroupRole groupRole = user.getRole(e.getActionCommand());
            groupRole.setRole(GroupRole.valueOf((String)((JComboBox)e.getSource()).getSelectedItem()));                                    
        }
        
    }
 
    private static class TableFieldEditor extends AbstractCellEditor implements TableCellEditor {
        JTextField field;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {             
            field = new JTextField((String)value);
            return field;
        }      

        @Override
        public Object getCellEditorValue() {
            return field.getText();
        }
         
    }
    
    private static class TableSelectBox extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        JComboBox selectBox;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {             
            selectBox = (JComboBox)value;
            return selectBox;
        }      

        @Override
        public Object getCellEditorValue() {
            return selectBox.getSelectedItem();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (JComboBox) value;
        }
         
    }
}
