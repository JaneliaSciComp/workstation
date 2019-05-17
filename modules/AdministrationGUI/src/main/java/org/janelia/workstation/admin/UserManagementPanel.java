package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserManagementPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);

    private AdministrationTopComponent parent;
    private UserManagementTableModel userManagementTableModel;
    private JTable userManagementTable;
    private JButton editUserButton;

    public UserManagementPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
        loadUsers();
    }

    private void setupUI() {

        setLayout(new BorderLayout());
    
        JPanel titlePanel = new TitlePanel("User List", "Return To Top Menu", event -> returnHome());
        add(titlePanel, BorderLayout.PAGE_START);

        userManagementTableModel = new UserManagementTableModel();
        userManagementTableModel.setParentManager(this);
        userManagementTable = new JTable(userManagementTableModel);
        userManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userManagementTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editUser();
                } else {
                    if (table.getSelectedRow() != -1)
                        editUserButton.setEnabled(true);
                }
            }
        });

        // hide the column we use to hold the User object
        userManagementTable.removeColumn(userManagementTable.getColumnModel().getColumn(UserManagementTableModel.COLUMN_SUBJECT));


        JScrollPane tableScroll = new JScrollPane(userManagementTable);
        add(tableScroll, BorderLayout.CENTER);

        // add groups pulldown selection for groups this person is a member of
        editUserButton = new JButton("Edit User");
        editUserButton.addActionListener(event -> editUser());
        editUserButton.setEnabled(false);
        JButton newUserButton = new JButton("New User");
        newUserButton.addActionListener(event -> newUser());

        JPanel actionPanel = new ActionPanel();
        actionPanel.add(editUserButton);
        actionPanel.add(newUserButton);
        add(actionPanel, BorderLayout.PAGE_END);
    }

    public void editUser() {
        User user = userManagementTableModel.getUserAtRow(userManagementTable.getSelectedRow());
        parent.viewUserDetails(user);
    }

    public void newUser() {
        User newUser = new User();
        // add the workstation role by default
        Set<UserGroupRole> roles = newUser.getUserGroupRoles();
        UserGroupRole newRole = new UserGroupRole("group:workstation_users", GroupRole.Reader);
        roles.add(newRole);
        newUser.setUserGroupRoles(roles);
        parent.viewUserDetails(newUser);
    }

    public void returnHome() {
        parent.viewTopMenu();
    }

    /**
     * Loads users based off your permissions.
     * If you are a workstation admin, you see everybody.
     * Group admins only see users in groups in which they are the owners.
     */
    private void loadUsers () {
        try {
            if (AccessManager.getAccessManager().isAdmin()) {
                List<Subject> subjectList = DomainMgr.getDomainMgr().getSubjects();
                List<User> userList = new ArrayList<>();
                for (Subject subject : subjectList) {
                    if (subject instanceof User)
                        userList.add((User) subject);
                }
                userManagementTableModel.addUsers(userList);
            }
        } catch (Exception e) {
            FrameworkAccess.handleException("Problem retrieving user information", e);
        }
    }

    class UserManagementTableModel extends AbstractTableModel {

        private String[] columnNames = {"Full name", "Username", "Groups", "Subject"};
        public static final int COLUMN_FULLNAME = 0;
        public static final int COLUMN_USERNAME = 1;
        public static final int COLUMN_GROUPS = 2;
        public static final int COLUMN_SUBJECT = 3;

        private List<User> users = new ArrayList<>();
        private UserManagementPanel manager;

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return users.size();
        }

        public void clear() {
            users = new ArrayList<>();
        }

        public void addUsers(List<User> userList) {
            // add users in bulk, replacing existing
            users = new ArrayList<>(userList);
            fireTableRowsInserted(0, users.size() - 1);
        }

        public void addUser(User user) {
            users.add(user);
            fireTableRowsInserted(users.size() - 1, users.size() - 1);
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
                case COLUMN_FULLNAME:
                    // full name
                    return users.get(row).getFullName();
                case COLUMN_USERNAME:
                    // username
                    return users.get(row).getName();
                case COLUMN_GROUPS:
                    // groups
                    return users.get(row).getReadGroups()
                            .stream()
                            .map(SubjectUtils::getSubjectName)
                            .sorted()
                            .collect(Collectors.joining(", "));
                case COLUMN_SUBJECT:
                    // subject (User) object:
                    return users.get(row);
                default:
                    throw new IllegalStateException("column " + col + "does not exist");
            }
        }

        // we store the Subject at the end of the row in a hidden column
        public User getUserAtRow(int row) {
            return users.get(row);
        }

        public void removeUser(int row) {
            users.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        @Override
        public Class getColumnClass(int c) {
            return (getValueAt(0, c) == null ? String.class : getValueAt(0, c).getClass());
        }

        private void setParentManager(UserManagementPanel parentManager) {
            manager = parentManager;
        }
    }
}
