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

    private JPanel backgroundPanel = new JPanel();
    private DefaultTableModel userListModel;
    private DefaultTableModel dataCircleModel;

    public DataGroupDialog(){
        super(SessionMgr.getBrowser(),"Data Circles", true);
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
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(doneButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);
        circleSetUpPanel.add(circleListPanel);
        circleSetUpPanel.add(middleButtonPanel);
        circleSetUpPanel.add(userListPanel);
        backgroundPanel.add(circleSetUpPanel);
        backgroundPanel.add(buttonPanel);

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