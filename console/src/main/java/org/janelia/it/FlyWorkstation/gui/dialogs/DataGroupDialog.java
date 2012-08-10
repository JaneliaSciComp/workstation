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
import java.util.ArrayList;
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


    private JPanel backgroundPanel = new JPanel();
    private DefaultTableModel userListModel;
    private DefaultTableModel dataCircleModel;
    final DefaultTableModel dataGroups = new DefaultTableModel();
    TableRowSorter<DefaultTableModel> userListSorter;
    TableRowSorter<DefaultTableModel> dataGroupSorter;
    TreeSet<DataGroup> userGroups = new TreeSet<DataGroup>();
    HashSet<User> usersInGroup = new HashSet<User>();
    List<User> userList = new ArrayList<User>();

    public DataGroupDialog(){
        super(SessionMgr.getBrowser(),"Data Groups", true);

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

        dataCircleModel = new DefaultTableModel();
        dataGroupSorter = new TableRowSorter<DefaultTableModel>(dataCircleModel);
        dataCircleModel.addColumn("Users in your Group");


        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataGroupDialog.this.setVisible(false);
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




        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,getDataGroupsTablePanel(),getCircleSetUpPanel());
        splitPane.setMaximumSize(new Dimension(1000,500));
        backgroundPanel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setMaximumSize(new Dimension(1000,500));
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(doneButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(Box.createHorizontalGlue());
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);
        backgroundPanel.setMaximumSize(new Dimension(1000,500));
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

    public JPanel getDataGroupsTablePanel() {
        final JTextField dataGroupName = new JTextField(40);
        TitledBorder dataGroupNameBorder = BorderFactory.createTitledBorder("Data Group Name");
        dataGroupName.setBorder(dataGroupNameBorder);
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.add(dataGroupName);
        namePanel.add(Box.createHorizontalGlue());

        dataGroups.addColumn("Existing Groups");
        final JTable dataGroupsTable = new JTable(dataGroups);
        dataGroupsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane dataGroupsTablePane = new JScrollPane(dataGroupsTable);
        dataGroupsTablePane.setMaximumSize(new Dimension(250,500));

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

        JPanel dataGroupsTablePanel = new JPanel();
        dataGroupsTablePanel.setLayout(new BoxLayout(dataGroupsTablePanel, BoxLayout.PAGE_AXIS));
        dataGroupsTablePanel.add(namePanel);
        dataGroupsTablePanel.add(dataGroupsTablePane);
        dataGroupsTablePanel.add(dataGroupsButtonPanel);

        return dataGroupsTablePanel;
    }

    public JPanel getCircleSetUpPanel() {

        final JTable userTable = new JTable(userListModel);
        userTable.setRowSorter(userListSorter);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        final JTable dataCircleTable = new JTable(dataCircleModel);
        dataCircleTable.setRowSorter(dataGroupSorter);
        dataCircleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        backgroundPanel.setLayout(new BorderLayout());

        final JButton addButton = new JButton("<<<");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] fetchedRows = userTable.getSelectedRows();
                for(int i : fetchedRows){
                    Object[] userToAdd = {userListModel.getValueAt(i, 0)};
                    dataCircleModel.addRow(userToAdd);
                    if (userList != null) {
                        usersInGroup.add(userList.get(i));
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

        JScrollPane dataCirclePane = new JScrollPane(dataCircleTable);
        dataCirclePane.setMaximumSize(new Dimension(250,500));
        JScrollPane userListPane = new JScrollPane(userTable);
        userListPane.setMaximumSize(new Dimension(250,500));
        JPanel middleButtonPanel = new JPanel();
        middleButtonPanel.setLayout(new BoxLayout(middleButtonPanel, BoxLayout.PAGE_AXIS));
        middleButtonPanel.add(addButton);
        middleButtonPanel.add(removeButton);

        JPanel circleSetUpPanel = new JPanel();
        circleSetUpPanel.setLayout(new BoxLayout(circleSetUpPanel, BoxLayout.X_AXIS));
        circleSetUpPanel.setSize(750,500);
        circleSetUpPanel.add(dataCirclePane);
        circleSetUpPanel.add(middleButtonPanel);
        circleSetUpPanel.add(userListPane);

        return circleSetUpPanel;
    }
}