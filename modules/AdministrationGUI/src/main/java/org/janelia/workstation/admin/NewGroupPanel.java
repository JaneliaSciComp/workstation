package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.janelia.model.security.*;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class NewGroupPanel extends JPanel implements Refreshable {
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
        refresh();
    }
    
    public void setupUI() {

        setLayout(new BorderLayout());
        removeAll();

        JPanel titlePanel = new TitlePanel("New Group", "Return To Group List",
                event -> refresh(),
                event -> returnHome());
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

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(new JLabel("* Required fields"), BorderLayout.NORTH);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.PAGE_END);


        revalidate();
    }
    
    private void createGroup() {
        // create key from name
        currentGroup.setKey("group:"+currentGroup.getName());
        log.info(currentGroup.toString());
        parent.createGroup(currentGroup);

        // set the current user as the owner of the group
        Subject rawAdmin = AccessManager.getAccessManager().getActualSubject();
        if (rawAdmin instanceof User) {
            User admin = (User) rawAdmin;
            Set<UserGroupRole> roles = new HashSet<>(admin.getUserGroupRoles());
            roles.add(new UserGroupRole(currentGroup.getKey(), GroupRole.Owner));
            parent.saveUserRoles(admin, roles);
        }
        else {
            log.warn("Cannot set group {} as owner of another group", rawAdmin);
        }

        returnHome();
    }
    
    private void initNewGroup() {
        this.currentGroup = new Group();
        // load new group
        newGroupTableModel.loadGroup(currentGroup);
        revalidate();
    }

    @Override
    public void refresh() {
        setupUI();
        initNewGroup();
    }

    private void returnHome() {
        parent.viewGroupList();
    }
    
    class NewGroupTableModel extends AbstractTableModel {
        String[] columnNames = {"Property","Value"};
        Group group;
        String[] editProperties = {"Name", "FullName", "LdapGroupName"};
        String[] editLabels = {"Name *", "Full Name *", "LDAP/AD Group Name"};
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
        
        void loadGroup(Group group) {
            this.group = group;
            for (int i=0; i<editProperties.length; i++) {
                String value;
                String editProperty = editProperties[i];
                try {
                    values[i] = (String) new PropertyDescriptor(editProperty, Group.class).getReadMethod().invoke(group);
                }
                catch (Exception e) {
                    throw new IllegalStateException("Error getting "+editProperty+" from "+group, e);
                }
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
