package org.janelia.workstation.admin;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.janelia.workstation.common.gui.support.WrapLayout;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class GroupDetailsPanel extends JPanel implements ActionListener {
    private static final Logger log = LoggerFactory.getLogger(GroupDetailsPanel.class);

    private AdministrationTopComponent parent;
    private String groupKey;
    private GroupRolesModel groupRolesModel;
    private JTable groupRolesTable;
    private JComboBox addUserBox;
    private Map<String, User> lookupUser;
    private int COLUMN_NAME = 0;
    private int COLUMN_ROLE = 1;

    public GroupDetailsPanel(AdministrationTopComponent parent, String group) {
        this.parent = parent;
        groupKey = group;
        setupUI();
    }

    public void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        JLabel titleLabel = new JLabel("Edit Group", JLabel.LEADING);  
        titleLabel.setFont(new Font("Serif", Font.PLAIN, 14));
        JButton returnHome = new JButton("return to grouplist");
        returnHome.setActionCommand("ReturnHome");
        returnHome.addActionListener(this);
        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(returnHome);
        horizontalBox.add(Box.createGlue());
        horizontalBox.add(titleLabel);
        horizontalBox.add(Box.createGlue());
        horizontalBox.add(Box.createGlue());
        titlePanel.add(horizontalBox);
        add(titlePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // show group edit table with permissions for that group
        groupRolesModel = new GroupRolesModel(this);
        groupRolesTable = new JTable(groupRolesModel);
        TableSelectBox groupSelectBox = new TableSelectBox();
        groupRolesTable.getColumn("Role").setCellEditor(groupSelectBox);
        groupRolesTable.getColumn("Role").setCellRenderer(groupSelectBox);         
        groupRolesTable.setRowHeight(25);
        JScrollPane groupRolesScroll = new JScrollPane(groupRolesTable);
        add(groupRolesScroll);
        
        addUserBox = new JComboBox();
        JButton newUserButton = new JButton("Add User");
        newUserButton.addActionListener(event -> addUser());
        JButton removeUserButton = new JButton("Remove User");
        removeUserButton.addActionListener(event -> removeUser());
        JButton saveUserButton = new JButton("Save User");
        saveUserButton.addActionListener(event -> updateUser());

        JPanel actionPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 2));
        actionPanel.add(addUserBox);
        actionPanel.add(newUserButton);
        actionPanel.add(removeUserButton);
        actionPanel.add(saveUserButton);
        add(actionPanel);
    }

    public void editGroupDetails(String groupKey, List<User> userList) throws Exception {
        Set<String> currentUsers = userList.stream().map(user -> user.getKey()).
                collect(Collectors.toSet());
        if (AccessManager.getAccessManager().isAdmin()) {
            groupRolesModel.loadGroupRoles(groupKey, userList);
            List<Subject> subjectList = DomainMgr.getDomainMgr().getSubjects();
            lookupUser = new HashMap<>();
            for (Subject subject : subjectList) {
                if (subject instanceof User) {
                    User user = (User)subject;
                    lookupUser.put(user.getFullName(), user);
                    if (!currentUsers.contains(user.getKey()))
                        addUserBox.addItem(user.getFullName());

                }
            }
        }
    }

    public void addUser() {
        String name = (String)addUserBox.getSelectedItem();
        User newUser = lookupUser.get(name);
        if (newUser!=null) {
            Set<UserGroupRole> roles = newUser.getUserGroupRoles();
            UserGroupRole newRole = new UserGroupRole(groupKey, GroupRole.Reader);
            roles.add(newRole);
            newUser.setUserGroupRoles(roles);
            parent.saveUserRoles(newUser);
            groupRolesModel.addUser(newUser);
        }
    }
    
    public void removeUser() {
        int row = groupRolesTable.getSelectedRow();
        User user = groupRolesModel.getUserAtRow(row);
        groupRolesModel.removeUser(row);
        parent.saveUserRoles(user);
    }
        
    public void updateUser() {
        int row = groupRolesTable.getSelectedRow();
        User user = groupRolesModel.getUserAtRow(row);
        parent.saveUserRoles(user);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("ReturnHome".equals(e.getActionCommand())) {
            parent.viewGroupList();
        }
    }

    class GroupRolesModel extends AbstractTableModel implements ActionListener {
        String[] columnNames = {"User","Role"};
        String[] roleOptions = { "Owner", "Admin", "Writer", "Reader"};
        List<User> data = new ArrayList<>();
        String group;
        GroupDetailsPanel manager;
        
        public GroupRolesModel(GroupDetailsPanel manager) {
            this.manager = manager;
        }

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

        public void loadGroupRoles(String group, List<User> users) {
            data = new ArrayList<>();
            this.group = group;
            data.addAll(users);
            fireTableRowsInserted(0, getRowCount()-1);
        }
        
        public void addUser(User user) {
            data.add(user);
            fireTableRowsInserted(getRowCount()-1, getRowCount()-1);
        }
        
        public void removeUser (int row) {
            User user = data.get(row);
            UserGroupRole roleToRemove = user.getRole(group);
            Set<UserGroupRole> roles = user.getUserGroupRoles();
            roles.remove(roleToRemove);
            user.setUserGroupRoles(roles);
            data.remove(row);
            fireTableRowsDeleted(getRowCount() - 1, getRowCount() - 1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            User user = data.get(row);
            if (col==COLUMN_NAME) {
                return user.getFullName();
            } else {               
                JComboBox roleSelection = new JComboBox(roleOptions);
                roleSelection.addActionListener(this);
                roleSelection.putClientProperty("row", row);
                roleSelection.setSelectedItem(user.getUserGroupRole(group).getLabel());
                return roleSelection;
            }
        }
        
        private User getUserAtRow(int row) {
            return data.get(row);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox role = (JComboBox)e.getSource();
            int row = (Integer)role.getClientProperty("row");
            User user = getUserAtRow(row);
            if (user!=null) {
                user.setUserGroupRole(group, GroupRole.valueOf((String)role.getSelectedItem()));
            }
        }

    }

}
