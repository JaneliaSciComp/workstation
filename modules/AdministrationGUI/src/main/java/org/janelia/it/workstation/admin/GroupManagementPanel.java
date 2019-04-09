/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.admin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.Group;
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
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel titlePanel = new JPanel();

        titlePanel.setLayout(new BorderLayout());
        titleLabel = new JLabel("Groups Management", JLabel.LEADING);  
        titleLabel.setFont(new Font("Serif", Font.PLAIN, 14));
        JButton returnHome = new JButton("return to top");
        returnHome.setActionCommand("ReturnHome");
        returnHome.addActionListener(event -> returnHome());

        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(returnHome);
        horizontalBox.add(Box.createGlue());
        horizontalBox.add(titleLabel);
        horizontalBox.add(Box.createGlue());
        horizontalBox.add(Box.createHorizontalStrut(returnHome.getWidth()));
        titlePanel.add(horizontalBox);
        add(titlePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    
        groupManagementTableModel = new GroupManagementTableModel();
        groupManagementTable = new JTable(groupManagementTableModel);
        groupManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupManagementTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editGroup();
                } else {
                    if (table.getSelectedRow() !=-1)
                        editGroupButton.setEnabled(true);
                }
            }
        });
        JScrollPane tableScroll = new JScrollPane(groupManagementTable);
        add(tableScroll);
        
        // add groups pulldown selection for groups this person is a member of 
        editGroupButton = new JButton("Edit Group");
        editGroupButton.addActionListener(event -> editGroup());
        editGroupButton.setEnabled(false);
        JButton newGroupButton = new JButton("New Group");
        newGroupButton.addActionListener(event -> newGroup());
        JPanel actionPanel = new JPanel();
//        newGroupPanel.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));   
        actionPanel.add(newGroupButton);
        actionPanel.add(editGroupButton);
        add(actionPanel);
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
    
    private void loadGroups () {
        try {
            if (AccessManager.getAccessManager().isAdmin()) {
                List<Subject> rawList = DomainMgr.getDomainMgr().getSubjects();
                List<Group> groupList = new ArrayList<>();
                Map<String, Integer> groupTotals = new HashMap<>();
                for (Subject subject: rawList) {
                    if (subject instanceof Group)
                        groupList.add((Group)subject);
                    else {
                        User user = (User)subject;
                        for (UserGroupRole groupRole: user.getUserGroupRoles()) {
                            String groupKey = groupRole.getGroupKey();
                            if (groupTotals.containsKey(groupKey))
                                groupTotals.put(groupKey, groupTotals.get(groupKey) + 1);
                            else 
                                groupTotals.put(groupKey, 1);
                        }
                    }
                }

                groupManagementTableModel.loadGroups(groupList, groupTotals);
            }
        } catch (Exception e) {
            String errorMessage = "Problem retrieving group information";
            log.error(errorMessage);
            e.printStackTrace();
        }
    }
        
    // add a group
    private void addGroup() {
    }
    
    class GroupManagementTableModel extends AbstractTableModel {
        String[] columnNames = {"Name","Number of Users"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Subject> groups = new ArrayList<>();
        
        public int getColumnCount() {
            return columnNames.length;
        }        

        public int getRowCount() {
            return data.size();
        }
        
        public void clear() {
            data = new ArrayList<List<Object>>();
            groups = new ArrayList<>();
        }
        
        public void loadGroups(List<Group> groupList, Map<String,Integer> groupTotals) {
            data = new ArrayList<List<Object>>();
            groups = new ArrayList<>();
            for (Group group: groupList) {
                Integer total = groupTotals.get(group.getKey());
                if (total!=null)
                    addGroup(group, total);
                else 
                    addGroup(group, 0);
            }
        }
        
        public void addGroup (Subject group, int total) {
            ArrayList groupRow = new ArrayList<>();
            groupRow.add(group.getFullName());
            groupRow.add(total);
            groups.add(group);
            data.add(groupRow);
            fireTableRowsInserted(data.size()-1, data.size()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }
        
        // kludgy way to store the Subject at the end of the row in a hidden column
        public Group getGroupAtRow(int row) {
            return (Group)groups.get(row);
        }
        
        public void removeGroup(int row) {
            data.remove(row);
            groups.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }  
    }
}
