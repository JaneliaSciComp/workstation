package org.janelia.workstation.admin;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.janelia.model.security.*;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class GroupManagementPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(GroupManagementPanel.class);

    private AdministrationTopComponent parent;
    private JTable groupManagementTable;
    private GroupManagementTableModel groupManagementTableModel;
    private int COLUMN_EDIT = 2;
    private int COLUMN_DELETE = 3;
    private JLabel titleLabel;
    private JButton editGroupButton;

    public GroupManagementPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
        loadGroups();
    }

    private void setupUI() {

        setLayout(new BorderLayout());

        JPanel titlePanel = new TitlePanel("Group List", "Return To Top Menu", event -> returnHome());
        add(titlePanel, BorderLayout.PAGE_START);

        groupManagementTableModel = new GroupManagementTableModel();
        groupManagementTable = new JTable(groupManagementTableModel);
        groupManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupManagementTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editGroup();
                } else {
                    if (table.getSelectedRow() != -1)
                        editGroupButton.setEnabled(true);
                }
            }
        });

        // hide the column we use to hold the Group object
        groupManagementTable.removeColumn(groupManagementTable.getColumnModel().getColumn(GroupManagementTableModel.COLUMN_SUBJECT));

        JScrollPane tableScroll = new JScrollPane(groupManagementTable);
        add(tableScroll, BorderLayout.CENTER);
        
        // add groups pulldown selection for groups this person is a member of
        editGroupButton = new JButton("Edit Group");
        editGroupButton.addActionListener(event -> editGroup());
        editGroupButton.setEnabled(false);
        JButton newGroupButton = new JButton("New Group");
        newGroupButton.addActionListener(event -> newGroup());

        JPanel actionPanel = new ActionPanel();
        actionPanel.add(newGroupButton);
        actionPanel.add(editGroupButton);
        add(actionPanel, BorderLayout.PAGE_END);
    }

    public void editGroup() {
        int groupRow = groupManagementTable.getSelectedRow();
        Group group = groupManagementTableModel.getGroupAtRow(groupRow);
        parent.viewGroupDetails(group.getKey());
    }

    public void newGroup() {
        parent.createNewGroup();
    }

    public void returnHome() {
        parent.viewTopMenu();
    }

    private void loadGroups() {
        try {
            Map<String, Integer> groupTotals = new HashMap<>();
            List<Group> groupList = new ArrayList<>();
            List<Subject> rawList = DomainMgr.getDomainMgr().getSubjects();
            for (Subject subject : rawList) {
                if (subject instanceof Group)
                    groupList.add((Group) subject);
                else {
                    User user = (User) subject;
                    for (UserGroupRole groupRole : user.getUserGroupRoles()) {
                        String groupKey = groupRole.getGroupKey();
                        if (groupTotals.containsKey(groupKey))
                            groupTotals.put(groupKey, groupTotals.get(groupKey) + 1);
                        else
                            groupTotals.put(groupKey, 1);
                    }
                }
            }
            if (!AccessManager.getAccessManager().isAdmin()) {
                // only keep groups where user is an admin or owner
                User loginUser = (User)AccessManager.getSubjectByNameOrKey(AccessManager.getSubjectKey());

                if (loginUser!=null) {
                    Map<String, Integer> filterTotals = new HashMap<>();
                    List<Group> filterList = new ArrayList<>();
                    for (UserGroupRole groupRole : loginUser.getUserGroupRoles()) {
                        String groupKey = groupRole.getGroupKey();
                        if (groupRole.getRole().equals(GroupRole.Admin) ||
                                groupRole.getRole().equals(GroupRole.Owner)) {
                            filterTotals.put(groupKey, groupTotals.get(groupKey));
                            Group group = (Group) AccessManager.getSubjectByNameOrKey(groupRole.getGroupKey());
                            if (group != null)
                                filterList.add(group);
                        }
                    }
                    groupList = filterList;
                    groupTotals = filterTotals;
                } else {
                    groupList = new ArrayList<>();
                    groupTotals = new HashMap<String,Integer>();
                }
            }

            groupManagementTableModel.loadGroups(groupList, groupTotals);
        } catch (Exception e) {
            FrameworkAccess.handleException("Problem retrieving group information", e);
        }
    }

    // add a group
    private void addGroup() {
    }

    class GroupManagementTableModel extends AbstractTableModel {
        String[] columnNames = {"Full group name", "Group name", "Number of users", "Subject"};
        public static final int COLUMN_FULLNAME = 0;
        public static final int COLUMN_GROUPNAME = 1;
        public static final int COLUMN_NUMUSERS = 2;
        public static final int COLUMN_SUBJECT = 3;

        List<Subject> groups = new ArrayList<>();
        Map<String, Integer> groupCounts = new HashMap<>();

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return groups.size();
        }

        public void clear() {
            groups = new ArrayList<>();
        }

        public void loadGroups(List<Group> groupList, Map<String, Integer> groupTotals) {
            groups = new ArrayList<>();
            for (Group group : groupList) {
                Integer total = groupTotals.get(group.getKey());
                if (total != null)
                    addGroup(group, total);
                else
                    addGroup(group, 0);
            }
        }

        public void addGroup(Subject group, int total) {
            groups.add(group);
            groupCounts.put(group.getKey(), total);
            fireTableRowsInserted(groups.size() - 1, groups.size() - 1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case COLUMN_FULLNAME:
                    // name
                    return groups.get(row).getFullName();
                case COLUMN_GROUPNAME:
                    // group name
                    return groups.get(row).getName();
                case COLUMN_NUMUSERS:
                    // number of groups
                    return groupCounts.get(groups.get(row).getKey());
                case COLUMN_SUBJECT:
                    return groups.get(row);
                default:
                    throw new IllegalStateException("column " + col + "does not exist");
            }
        }

        // kludgy way to store the Subject at the end of the row in a hidden column
        public Group getGroupAtRow(int row) {
            return (Group) groups.get(row);
        }

        public void removeGroup(int row) {
            groupCounts.remove(groups.get(row).getKey());
            groups.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c) == null ? String.class : getValueAt(0, c).getClass());
        }
    }
}
