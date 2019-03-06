/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.admin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.model.security.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class GroupManagementPanel extends JPanel implements ActionListener { 
    private static final Logger log = LoggerFactory.getLogger(GroupManagementPanel.class);
    
    private JTable groupManagementTable;
    private int COLUMN_EDIT = 2;
    private int COLUMN_DELETE = 3;
    private JLabel titleLabel;

    public GroupManagementPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        titleLabel = new JLabel("Groups Management", JLabel.LEADING);       
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    
        GroupManagementTableModel groupManagementTableModel = new GroupManagementTableModel();
        groupManagementTableModel.setParentManager(this);
        groupManagementTable = new JTable(groupManagementTableModel);
        groupManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);   
        groupManagementTable.addMouseListener(new MouseHandler() {
            @Override
            protected void singleLeftClicked(MouseEvent me) {
                if (me.isConsumed()) {
                    return;
                }
                JTable table = (JTable) me.getSource();
                int viewRow = table.rowAtPoint(me.getPoint());
                if (viewRow >= 0) {
                    int viewColumn = table.columnAtPoint(me.getPoint());
                    if (viewColumn == COLUMN_EDIT) {
                        Subject group = ((GroupManagementTableModel) groupManagementTable.getModel()).getGroupAtRow(viewRow);
                        editGroup(group);
                    } else if (viewColumn == COLUMN_DELETE) {
                        Subject group = ((GroupManagementTableModel) groupManagementTable.getModel()).getGroupAtRow(viewRow);
                        deleteGroup(group);
                    }
                }
                me.consume();
            }
        });
        JScrollPane tableScroll = new JScrollPane(groupManagementTable);
        add(tableScroll);
    }
    
    private void loadGroups () {
        try {
            if (AccessManager.getAccessManager().isAdmin()) {
                //List<Subject> rawList = DomainMgr.getDomainMgr().getSubjects();                                
            }
        } catch (Exception e) {
            String errorMessage = "Problem retrieving group information";
            log.error(errorMessage);
            e.printStackTrace();
        }
    }
        
    // loads the Group Details page in add mode
    private void addGroup() {
        
    }
    
    // hide this panel and open up the Group Details Panel
    private void editGroup(Subject group) {
        
    }
    
    // remove the Group from the  as well as the table model
    private void deleteGroup(Subject group) {
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("Edit".equals(command)) {
            
        } else if ("Delete".equals(command)) {
            
        } else if ("Add".equals(command)) {
            
        }
    }
    
    class GroupManagementTableModel extends AbstractTableModel {
        String[] columnNames = {"Name","Number of Users",
                                "", ""};
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Subject> groups = new ArrayList<>();
        private GroupManagementPanel manager;
        
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
        
        public void loadGroups(List<Subject> groupList) {
            data = new ArrayList<List<Object>>();
            groups = new ArrayList<>();
            for (Subject group: groupList) {
                addGroup(group);
            }
        }
        
        public void addGroup (Subject group) {
            ArrayList groupRow = new ArrayList<>();
            groupRow.add(group.getFullName());
            groups.add(group);
            fireTableRowsInserted(data.size()-1, data.size()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_EDIT) {
                JButton editGroup = new JButton("Edit");                
                editGroup.setActionCommand("Edit");
                editGroup.addActionListener(manager);
                return editGroup;
            } else if (col==COLUMN_DELETE) {
                JButton deleteGroup = new JButton("Delete");
                deleteGroup.setActionCommand("Delete");
                deleteGroup.addActionListener(manager);
                return deleteGroup;
            }
            return data.get(row).get(col);
        }
        
        // kludgy way to store the Subject at the end of the row in a hidden column
        public Subject getGroupAtRow(int row) {
            return (Subject)groups.get(row);
        }
        
        public void removeGroup(int row) {
            data.remove(row);
            groups.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        private void setParentManager(GroupManagementPanel parentManager) {
           manager = parentManager;
        }
        
    }
}
