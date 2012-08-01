package org.janelia.it.FlyWorkstation.gui.dialogs;


import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.user_data.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 7/19/12
 * Time: 1:34 PM
 */
public class DataGroupDialog extends JDialog {

//    private DualTable _dualTable;
    private JPanel backgroundPanel = new JPanel();
    private DefaultTableModel userListModel;
    private DefaultTableModel dataCircleModel;

    public DataGroupDialog(){
        super(SessionMgr.getBrowser(),"Data Groups", true);
        List<User> userList = null;
        try {
            userList = ModelMgr.getModelMgr().getUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
            SessionMgr.getSessionMgr().handleException(e);
        }

        userListModel = new DefaultTableModel();
        userListModel.addColumn("Users");

        for (int i = 0; i < userList.size(); i++) {
            Object[] userString = {userList.get(i).getUserLogin()};
            userListModel.addRow(userString);
        }

        final JTable userTable = new JTable(userListModel);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        dataCircleModel = new DefaultTableModel();
        dataCircleModel.addColumn("Users in your Circle");
        final JTable dataCircleTable = new JTable(dataCircleModel);
        dataCircleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        backgroundPanel.setLayout(new BoxLayout(backgroundPanel, BoxLayout.PAGE_AXIS));

        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataGroupDialog.this.setVisible(false);
            }
        });

        final JButton addButton = new JButton("<<<");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] userToAdd = {userListModel.getValueAt(userTable.getSelectedRow(), 0)};
                dataCircleModel.addRow(userToAdd);
                userListModel.removeRow(userTable.getSelectedRow());
            }
        });

        final JButton removeButton = new JButton(">>>");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] userToRemove = {dataCircleModel.getValueAt(dataCircleTable.getSelectedRow(), 0)};
                userListModel.addRow(userToRemove);
                dataCircleModel.removeRow(dataCircleTable.getSelectedRow());
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (dataCircleModel.getValueAt(0,0)!=null){
                    Object[] moveUser = {dataCircleModel.getValueAt(0,0)};
                    userListModel.addRow(moveUser);
                    dataCircleModel.removeRow(0);
                }
            }
        });

        JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataGroupDialog.this.setVisible(false);
            }
        });
        JPanel circleSetUpPanel = new JPanel();
        circleSetUpPanel.setLayout(new BoxLayout(circleSetUpPanel, BoxLayout.X_AXIS));
        JPanel circleListPanel = new JPanel();
        JScrollPane dataCirclePane = new JScrollPane(dataCircleTable);
        circleListPanel.add(dataCirclePane);
        JPanel userListPanel = new JPanel();
        JScrollPane userListPane = new JScrollPane(userTable);
        userListPanel.add(userListPane);
        JPanel middleButtonPanel = new JPanel();
        TitledBorder circleListBorder = BorderFactory.createTitledBorder("Users in your circle");
        TitledBorder userListBorder = BorderFactory.createTitledBorder("Users");
        circleListPanel.setBorder(circleListBorder);
        userListPanel.setBorder(userListBorder);
        middleButtonPanel.setLayout(new BoxLayout(middleButtonPanel, BoxLayout.PAGE_AXIS));
        middleButtonPanel.add(addButton);
        middleButtonPanel.add(removeButton);
        circleSetUpPanel.add(circleListPanel);
        circleSetUpPanel.add(middleButtonPanel);
        circleSetUpPanel.add(userListPanel);
//        backgroundPanel.add(circleSetUpPanel);

        final DefaultTableModel dataGroups = new DefaultTableModel();
        final JTable dataGroupsTable = new JTable(dataGroups);
        JPanel dataGroupsTablePanel = new JPanel();
        dataGroupsTablePanel.setLayout(new BoxLayout(dataGroupsTablePanel, BoxLayout.PAGE_AXIS));
        dataGroupsTablePanel.add(dataGroupsTable);

        final JButton addGroupButton = new JButton("Add Group");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataGroups.addRow(new Object[]{""});
            }
        });

        final JButton removeGroupButton = new JButton("Remove Group");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataGroups.removeRow(dataGroupsTable.getSelectedRow());
            }
        });

        dataGroupsTablePanel.add(addGroupButton);
        dataGroupsTablePanel.add(removeGroupButton);

//        java.util.List taskList = createTaskList(userList);
//        TableModelAdapter tableModelAdapter = new TaskTableModelAdapter();
//        _dualTable = new DualTable(taskList, tableModelAdapter);
//        _dualTable.setDoubleClickEnabled(true);
//        JPanel panel = new JPanel(new BorderLayout(4, 4));
//        panel.add(_dualTable, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,dataGroupsTablePanel,circleSetUpPanel);
        backgroundPanel.add(splitPane);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(doneButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        backgroundPanel.add(buttonPanel);



        createAndShowGUI();
    }

    private void createAndShowGUI() {
        //Create and set up the window.

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(true);
        this.setIconImage(SessionMgr.getBrowser().getIconImage());
        this.setLocationRelativeTo(SessionMgr.getBrowser());
        backgroundPanel.setOpaque(true); //content panes must be opaque
        this.setContentPane(backgroundPanel);

        //Display the window.
        this.pack();
    }

//    private java.util.List createTaskList(List<User> userList) {
//        java.util.List<Task> rows = new ArrayList<Task>();
//        for( User user: userList){
//            rows.add(new Task(user.getUserLogin()));
//        }
//        return rows;
//    }

//    private static class Task extends DefaultExpandableRow {
//        String name;
//
//
//        public Task() {
//        }
//
//
//        public Task(String name) {
//            this.name = name;
//
//        }
//
//        public Object getValueAt(int columnIndex) {
//            switch (columnIndex) {
//                case 0:
//                    return name;
//
//            }
//            return null;
//        }
//
//        @Override
//        public void setValueAt(Object value, int columnIndex) {
//            switch (columnIndex) {
//                case 0:
//                    name = "" + value;
//                    break;
//            }
//            super.setValueAt(value, columnIndex);
//        }
//    }
//
//    private static class TaskTableModelAdapter implements TableModelAdapter {
//        public TaskTableModelAdapter() {
//        }
//
//        public int getColumnCount() {
//            return 1;
//        }
//
//        public Class<?> getColumnClass(int columnIndex) {
//            switch (columnIndex) {
//                case 0:
//                    return String.class;
//            }
//            return Object.class;
//        }
//
//        public String getColumnName(int column) {
//            switch (column) {
//                case 0:
//                    return "User Name";
//            }
//            return null;
//        }
//    }
}