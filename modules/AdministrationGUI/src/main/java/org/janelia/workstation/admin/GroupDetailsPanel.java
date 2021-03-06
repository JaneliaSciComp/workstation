package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.common.gui.support.SubjectComboBoxRenderer;
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
public class GroupDetailsPanel extends JPanel implements Refreshable {
    private static final Logger log = LoggerFactory.getLogger(GroupDetailsPanel.class);

    private AdministrationTopComponent parent;
    private String groupKey;
    private GroupRolesModel groupRolesModel;
    private JTable groupRolesTable;
    private JComboBox<Subject> addUserBox;
    private int COLUMN_NAME = 0;
    private int COLUMN_ROLE = 1;

    public GroupDetailsPanel(AdministrationTopComponent parent, String groupKey) {
        this.parent = parent;
        this.groupKey = groupKey;
        refresh();
    }

    private void setupUI() {

        setLayout(new BorderLayout());
        removeAll();

        String groupName = SubjectUtils.getSubjectName(groupKey);

        JPanel titlePanel = new TitlePanel("Edit Group "+groupName, "Return To Group List",
                event -> refresh(),
                event -> returnHome());
        add(titlePanel, BorderLayout.PAGE_START);

        // show group edit table with permissions for that group
        groupRolesModel = new GroupRolesModel(this);
        groupRolesTable = new JTable(groupRolesModel);
        TableSelectBox groupSelectBox = new TableSelectBox();
        groupRolesTable.getColumn("Role").setCellEditor(groupSelectBox);
        groupRolesTable.getColumn("Role").setCellRenderer(groupSelectBox);         
        groupRolesTable.setRowHeight(25);
        JScrollPane groupRolesScroll = new JScrollPane(groupRolesTable);
        add(groupRolesScroll, BorderLayout.CENTER);
        
        addUserBox = new JComboBox<>();
        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        addUserBox.setRenderer(renderer);
        addUserBox.setMaximumRowCount(20);

        JButton newUserButton = new JButton("Add User");
        newUserButton.addActionListener(event -> addUser());
        JButton removeUserButton = new JButton("Remove User");
        removeUserButton.addActionListener(event -> removeUser());
        JButton saveUserButton = new JButton("Save User");
        saveUserButton.addActionListener(event -> updateUser());

        JPanel actionPanel = new ActionPanel();
        actionPanel.add(addUserBox);
        actionPanel.add(newUserButton);
        actionPanel.add(removeUserButton);
        actionPanel.add(saveUserButton);
        add(actionPanel, BorderLayout.PAGE_END);

        revalidate();
    }

    private void editGroupDetails() {

        SimpleWorker worker = new SimpleWorker() {

            List<User> userList;
            List<Subject> subjectList;

            @Override
            protected void doStuff() throws Exception {
                userList = DomainMgr.getDomainMgr().getUsersInGroup(groupKey);
                subjectList = DomainMgr.getDomainMgr().getSubjects();
            }

            @Override
            protected void hadSuccess() {
                Set<String> currentUsers = userList.stream().map(Subject::getKey).collect(Collectors.toSet());
                groupRolesModel.loadGroupRoles(groupKey, userList);
                for (Subject subject : subjectList) {
                    if (subject instanceof User) {
                        User user = (User) subject;
                        if (!currentUsers.contains(user.getKey())) {
                            addUserBox.addItem(user);
                        }
                    }
                }
                revalidate();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private void addUser() {
        User newUser = (User)addUserBox.getSelectedItem();
        if (newUser!=null) {
            Set<UserGroupRole> roles = new HashSet<>(newUser.getUserGroupRoles());
            roles.add(new UserGroupRole(groupKey, GroupRole.Reader));
            parent.saveUserRoles(newUser, roles);
            groupRolesModel.addUser(newUser);
        }
    }
    
    private void removeUser() {
        int row = groupRolesTable.getSelectedRow();
        User user = groupRolesModel.getUserAtRow(row);
        groupRolesModel.removeUser(row);
        parent.saveUserRoles(user, user.getUserGroupRoles());
    }
        
    private void updateUser() {
        int row = groupRolesTable.getSelectedRow();
        User user = groupRolesModel.getUserAtRow(row);
        parent.saveUserRoles(user, user.getUserGroupRoles());
    }

    @Override
    public void refresh() {
        setupUI();
        editGroupDetails();
    }

    private void returnHome() {
        parent.viewGroupList();
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
        
        public void removeUser(int row) {
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
                return user.getFullName()+" ("+user.getName()+")";
            }
            else {
                JComboBox roleSelection = new JComboBox<>(roleOptions);
                roleSelection.addActionListener(this);
                roleSelection.putClientProperty("row", row);
                if (user.getUserGroupRole(group)!=null) {
                    roleSelection.setSelectedItem(user.getUserGroupRole(group).getLabel());
                }
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
