package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.janelia.model.security.Group;
import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserDetailsPanel extends JPanel implements Refreshable {
    private static final Logger LOG = LoggerFactory.getLogger(UserDetailsPanel.class);

    private AdministrationTopComponent parent;
    private JComboBox newGroupSelector;
    private JButton saveUserButton;
    private JButton removeGroupButton;
    private JButton newGroupButton;
    private UserDetailsTableModel userDetailsTableModel;
    private JTable userDetailsTable;
    private GroupRolesModel groupRolesModel;
    private JTable groupRolesTable;
    private User currentUser;
    private User admin;
    private boolean passwordChanged = false;
    private boolean dirtyFlag = false;
    private boolean canEdit = false;
    private int COLUMN_NAME = 0;
    private int COLUMN_VALUE = 1;
    
    public UserDetailsPanel(AdministrationTopComponent parent, User user) {
        this.parent = parent;
        this.currentUser = user;
        refresh();
    }
    
    private void setupUI() {

        setLayout(new BorderLayout());
        removeAll();

        JPanel titlePanel = new TitlePanel("User Details", "Return To User List",
                event -> refresh(),
                event -> returnHome());
        add(titlePanel, BorderLayout.PAGE_START);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        userDetailsTableModel = new UserDetailsTableModel();
        userDetailsTable = new JTable(userDetailsTableModel);
        TableFieldEditor editor = new TableFieldEditor();
        userDetailsTable.getColumn("Property").setCellEditor(editor);
        userDetailsTable.setRowHeight(25);
        JScrollPane userTableScroll = new JScrollPane(userDetailsTable);
        mainPanel.add(userTableScroll);


        // show group edit table with permissions for that group
        JLabel groupRolesLabel = new JLabel("Roles", SwingConstants.LEFT);
        add(groupRolesLabel);
        groupRolesModel = new GroupRolesModel();
        groupRolesTable = new JTable(groupRolesModel);
        TableSelectBox groupSelectBox = new TableSelectBox();
        groupRolesTable.getColumn("Role").setCellEditor(groupSelectBox);
        groupRolesTable.getColumn("Role").setCellRenderer(groupSelectBox);
        groupRolesTable.setRowHeight(25);
        groupRolesTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                    if (table.getSelectedRow() !=-1)
                        removeGroupButton.setEnabled(true);
            }
        });
        JScrollPane groupRolesScroll = new JScrollPane(groupRolesTable);
        mainPanel.add(groupRolesScroll);

        add(mainPanel, BorderLayout.CENTER);

        // add groups pulldown selection for groups this person is a member of
        newGroupSelector = new JComboBox();
        newGroupButton = new JButton("Add Group");
        newGroupButton.addActionListener(event -> addNewGroup());

        removeGroupButton = new JButton("Remove Group");
        removeGroupButton.setEnabled(false);
        removeGroupButton.addActionListener(event -> removeGroup());

        saveUserButton = new JButton("Save User");
        saveUserButton.addActionListener(event -> saveUser());
        saveUserButton.setEnabled(false);

        JPanel actionPanel = new ActionPanel();
        actionPanel.add(newGroupSelector);
        actionPanel.add(newGroupButton);
        actionPanel.add(removeGroupButton);
        actionPanel.add(saveUserButton);
        add(actionPanel, BorderLayout.PAGE_END);

        revalidate();
    }
    
    private void saveUser () {
        if (currentUser.getId() == null) {
            User newUser = parent.createUser(currentUser);
            if (newUser != null) {
                currentUser.setId(newUser.getId());
                currentUser.setKey(newUser.getKey());
            }
        }
        parent.saveUser(currentUser, passwordChanged);
        parent.saveUserRoles(currentUser);
        parent.viewUserList();
    }
    
    private void editUserDetails() {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (currentUser.getId() != null) {
                    // refresh user from database
                    currentUser = (User) DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(currentUser.getKey());
                }
            }

            @Override
            protected void hadSuccess() {
                Subject rawAdmin = AccessManager.getAccessManager().getActualSubject();
                if (rawAdmin instanceof User && AccessManager.getAccessManager().isAdmin()) {
                    admin = (User) rawAdmin;
                    canEdit = true;
                }
                if (rawAdmin.getKey().equals(currentUser.getKey()))
                    canEdit = true;

                // load user details
                userDetailsTableModel.loadUser(currentUser);
                groupRolesModel.loadGroupRoles(currentUser);
                refreshUserGroupsToAdd();
                revalidate();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private void refreshUserGroupsToAdd() {
        // load up the new groups available to add to this user
        newGroupSelector.removeAllItems();
        groupSelectionSource().forEach(g -> newGroupSelector.addItem(g));
        if (newGroupSelector.getItemCount() == 0) {
            // no groups to add
            newGroupButton.setEnabled(false);
        } else
            newGroupButton.setEnabled(true);
    }

    private Stream<String> groupSelectionSource() {
        Stream<String> selectionSource;
        if (AccessManager.getAccessManager().isAdmin()) {
            try {
                selectionSource = DomainMgr.getDomainMgr().getSubjects()
                        .stream()
                        .filter(s -> s instanceof Group)
                        .map(s -> s.getKey());
            } catch (Exception e) {
                LOG.error("Error retrieving group subjects for setting user groups", e);
                selectionSource = Stream.of();
            }
        } else {
            LOG.debug("Populate user groups from the current list");
            selectionSource = currentUser.getUserGroupRoles().stream()
                    .map(ug -> ug.getGroupKey());
        }

        return selectionSource
                .filter(g -> currentUser.getRole(g) == null);
    }

    // adds the user to a new group   
    private void addNewGroup() {
        String groupKey = (String)newGroupSelector.getSelectedItem();
        groupRolesModel.addNewGroup(groupKey);
    }

    // removes the user from a group
    private void removeGroup() {
        int row = groupRolesTable.getSelectedRow();
        if (row != -1) {
            groupRolesModel.removeGroup(row);
            removeGroupButton.setEnabled(false);
            saveUserButton.setEnabled(true);
            refreshUserGroupsToAdd();
            dirtyFlag = true;
        }
    }

    @Override
    public void refresh() {
        setupUI();
        editUserDetails();
    }

    private void returnHome() {
        parent.viewUserList();         
    }
    
    class UserDetailsTableModel extends AbstractTableModel {
        String[] columnNames = {"Property","Value"};
        User user;
        String password;
        String[] editProperties = {"Name", "FullName", "Email", "Password"};
        String[] editLabels = {"Name", "Full Name", "Email", "Password"};
        String[] values = new String[editProperties.length];
                
        @Override
        public boolean isCellEditable(int row, int col) {
            // if not admin return false
            if (!canEdit)
                return false;
            // can't change username if user already exists
            if (user.getKey()!=null && row==0)
                return false;
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
        
        public void loadUser(User user) {
            this.user = user;
            for (int i=0; i<editProperties.length; i++) {
                String value;
                String editProperty = editProperties[i];
                if (editProperty.equals("Password")) {
                    values[i] ="**********";
                } else {
                    try {
                        values[i] =(String) new PropertyDescriptor(editProperty, User.class).getReadMethod().invoke(user);
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("Error getting "+editProperty+" from "+user, e);
                    }
                }
            }
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {            
            return columnNames[col];            
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {
                return editLabels[row];
            } else 
                if (editProperties[row].equals("Password")) {
                    return "*********";
                }
                return values[row];
        }

        public Class getColumnClass(int c) {
            return String.class;
        }

        private void validateUserChanges() {
            if (user.getName()!=null && dirtyFlag)
                saveUserButton.setEnabled(true);
            else
                saveUserButton.setEnabled(false);
        }
        
         @Override
        public void setValueAt(Object value, int row, int col) {
            try {
                if (editProperties[row].equals("Password")) {
                    password = (String)value;
                    user.setPassword(password);
                    passwordChanged = true;
                } else {
                    new PropertyDescriptor(editProperties[row], User.class).getWriteMethod().invoke(user, (String)value);
                }

                if (values[row]==null ||
                        !values[row].equals((String)value))
                    dirtyFlag = true;
                values[row] = (String)value;
                validateUserChanges();
                this.fireTableCellUpdated(row, col);
            } catch (Exception ex) {
                FrameworkAccess.handleException("Problem updating user information using reflection", ex);
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
            // if not admin return false
            if (admin == null)
                return false;
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
            Set<UserGroupRole> roles = user.getUserGroupRoles();
            UserGroupRole newRole = new UserGroupRole(groupKey, GroupRole.Reader);
            roles.add(newRole);
            user.setUserGroupRoles(roles);
            data.add(user.getRole(groupKey));
            dirtyFlag = true;
            validateUserChanges();
            fireTableRowsInserted(getRowCount()-1, getRowCount()-1);
        }

        public void removeGroup(int row) {
            String groupKey = data.get(row).getGroupKey();
            UserGroupRole deleteRole = user.getRole(groupKey);
            Set<UserGroupRole> roles = user.getUserGroupRoles();
            roles.remove(deleteRole);
            user.setUserGroupRoles(roles);
            data.remove(row);
            validateUserChanges();
            fireTableRowsDeleted(row, row);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {                
                String groupKey = data.get(row).getGroupKey();
                return SubjectUtils.getSubjectName(groupKey);
            }
            else {
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
            GroupRole newRole = GroupRole.valueOf((String)((JComboBox)e.getSource()).getSelectedItem());
            if (groupRole.getRole()!=newRole) {
                dirtyFlag = true;
            }
            groupRole.setRole(newRole);
            validateUserChanges();
        }

        private void validateUserChanges() {
            if (user.getName()!=null && dirtyFlag)
                saveUserButton.setEnabled(true);
            else
                saveUserButton.setEnabled(false);
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
}
