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
public class DataCircleDialog extends JDialog {

    private JPanel backgroundPanel = new JPanel();
    private DefaultTableModel userListModel;
    private DefaultTableModel dataCircleModel;

    public DataCircleDialog(){
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
        userListModel.addRow(userList.toArray());
        JTable userTable = new JTable(userListModel);

        dataCircleModel = new DefaultTableModel();
        dataCircleModel.addColumn("Users in your Circle");
        JTable dataCircleTable = new JTable(dataCircleModel);

        backgroundPanel.setLayout(new BoxLayout(backgroundPanel, BoxLayout.PAGE_AXIS));

        final JButton doneButton = new JButton("Save");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        final JButton addButton = new JButton("<<<");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        final JButton removeButton = new JButton(">>>");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DataCircleDialog.this.setVisible(false);
            }
        });
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        JPanel circleListPanel = new JPanel();
        JPanel userListPanel = new JPanel();
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
        topPanel.add(circleListPanel);
        topPanel.add(middleButtonPanel);
        topPanel.add(userListPanel);
        backgroundPanel.add(topPanel);
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