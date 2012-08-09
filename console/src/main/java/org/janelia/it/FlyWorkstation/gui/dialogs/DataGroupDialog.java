package org.janelia.it.FlyWorkstation.gui.dialogs;


import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.user_data.DataGroup;
import org.janelia.it.jacs.model.user_data.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

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
    final DefaultTableModel dataGroups;
    TableRowSorter<DefaultTableModel> userListSorter;
    TableRowSorter<DefaultTableModel> dataGroupSorter;
    TreeSet<DataGroup> userGroups = new TreeSet<DataGroup>();
    HashSet<User> usersInGroup = new HashSet<User>();

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

        TreeSet<String> userSet = new TreeSet<String>();
        if (userList != null) {
            for(User user : userList){
                userSet.add(user.getUserLogin());
            }
        }

        userListModel = new DefaultTableModel();
        userListSorter = new TableRowSorter<DefaultTableModel>(userListModel);
        userListModel.addColumn("Users");

        for (String userLogin: userSet) {
            Object[] userString = {userLogin};
            userListModel.addRow(userString);
        }

        final JTable userTable = new JTable(userListModel);
        userTable.setRowSorter(userListSorter);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        dataCircleModel = new DefaultTableModel();
        dataGroupSorter = new TableRowSorter<DefaultTableModel>(dataCircleModel);
        dataCircleModel.addColumn("Users in your Group");
        final JTable dataCircleTable = new JTable(dataCircleModel);
        dataCircleTable.setRowSorter(dataGroupSorter);
        dataCircleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        backgroundPanel.setLayout(new BorderLayout());

        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataGroupDialog.this.setVisible(false);
            }
        });

        final JButton addButton = new JButton("<<<");
        final List<User> finalUserList = userList;
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] fetchedRows = userTable.getSelectedRows();
                for(int i : fetchedRows){
                    Object[] userToAdd = {userListModel.getValueAt(i, 0)};
                    dataCircleModel.addRow(userToAdd);
                    if (finalUserList != null) {
                        usersInGroup.add(finalUserList.get(i));
                    }
                }
                for(int i = fetchedRows.length-1; i >=0 ; i--){
                    userListModel.removeRow(fetchedRows[i]);
                }
            }
        });

        final JButton removeButton = new JButton(">>>");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] fetchedRows = dataCircleTable.getSelectedRows();
                for(int i : fetchedRows){
                    Object[] userToRemove = {dataCircleModel.getValueAt(i, 0)};
                    userListModel.addRow(userToRemove);
                }
                for(int i = fetchedRows.length-1; i >=0 ; i--){
                    dataCircleModel.removeRow(fetchedRows[i]);
                }
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

        final JTextField dataGroupName = new JTextField(40);
        TitledBorder dataGroupNameBorder = BorderFactory.createTitledBorder("Data Group Name");
        dataGroupName.setBorder(dataGroupNameBorder);
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.add(dataGroupName);
        namePanel.add(Box.createHorizontalGlue());

        JPanel circleSetUpPanel = new JPanel();
        circleSetUpPanel.setLayout(new BoxLayout(circleSetUpPanel, BoxLayout.X_AXIS));
        JScrollPane dataCirclePane = new JScrollPane(dataCircleTable);
        JScrollPane userListPane = new JScrollPane(userTable);
        JPanel middleButtonPanel = new JPanel();
        middleButtonPanel.setLayout(new BoxLayout(middleButtonPanel, BoxLayout.PAGE_AXIS));
        middleButtonPanel.add(addButton);
        middleButtonPanel.add(removeButton);
        circleSetUpPanel.add(dataCirclePane);
        circleSetUpPanel.add(middleButtonPanel);
        circleSetUpPanel.add(userListPane);
//        backgroundPanel.add(circleSetUpPanel);
        dataGroups = new DefaultTableModel();
        dataGroups.addColumn("Existing Groups");
        final JTable dataGroupsTable = new JTable(dataGroups);
        dataGroupsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane dataGroupsTablePane = new JScrollPane(dataGroupsTable);
        JPanel dataGroupsTablePanel = new JPanel();
        dataGroupsTablePanel.setLayout(new BoxLayout(dataGroupsTablePanel, BoxLayout.PAGE_AXIS));
        dataGroupsTablePanel.add(namePanel);
        dataGroupsTablePanel.add(dataGroupsTablePane);

        final JButton addGroupButton = new JButton("+");
        addGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!dataGroupName.getText().equals("") && null!=dataGroupName.getText()){
                    dataGroups.addRow(new Object[]{dataGroupName.getText()});
//                    userGroups.add(new DataGroup(dataGroupName.getText(), ))
                }
                dataGroupName.setText("");
            }
        });

        final JButton removeGroupButton = new JButton("-");
        removeGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataGroups.removeRow(dataGroupsTable.getSelectedRow());
                dataGroupName.setText("");
            }
        });
        JPanel dataGroupsButtonPanel = new JPanel();
        dataGroupsButtonPanel.setLayout(new BoxLayout(dataGroupsButtonPanel, BoxLayout.X_AXIS));
        dataGroupsButtonPanel.add(addGroupButton);
        dataGroupsButtonPanel.add(removeGroupButton);
        dataGroupsTablePanel.add(dataGroupsButtonPanel);

//        java.util.List taskList = createTaskList(userList);
//        TableModelAdapter tableModelAdapter = new TaskTableModelAdapter();
//        _dualTable = new DualTable(taskList, tableModelAdapter);
//        _dualTable.setDoubleClickEnabled(true);
//        JPanel panel = new JPanel(new BorderLayout(4, 4));
//        panel.add(_dualTable, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,dataGroupsTablePanel,circleSetUpPanel);
        backgroundPanel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(doneButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(Box.createHorizontalGlue());
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);

        createAndShowGUI();
    }

    private void createAndShowGUI() {
        //Create and set up the window.

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setIconImage(SessionMgr.getBrowser().getIconImage());
        this.setLocationRelativeTo(SessionMgr.getBrowser());
        backgroundPanel.setOpaque(true); //content panes must be opaque
        this.setContentPane(backgroundPanel);

        //Display the window.
        this.pack();
    }

}