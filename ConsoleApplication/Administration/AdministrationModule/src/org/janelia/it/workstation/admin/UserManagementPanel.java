package org.janelia.it.workstation.admin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.model.domain.tiledMicroscope.TmReviewItem;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserManagementPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    
    private AdministrationTopComponent parent;
    private JLabel titleLabel;
    private JLabel workspaceNameLabel;
    private JLabel sampleNameLabel;
    UserManagementTableModel userManagementTableModel;
    private JTable userManagementTable;   
    private AbstractAction buttonAction;
    private int COLUMN_EDIT = 2;
    private int COLUMN_DELETE = 3;

    public UserManagementPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        setupUI();
        loadUsers();      
        
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        JButton returnHome = new JButton("return to top");
        returnHome.setActionCommand("ReturnHome");
        returnHome.addActionListener(event -> returnHome());
        returnHome.setBorderPainted(false);
        returnHome.setOpaque(false);
        titlePanel.add(returnHome);
        JLabel titleLabel = new JLabel("User List", JLabel.LEADING);
        titlePanel.add(titleLabel);
        
        add(titlePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    
        userManagementTableModel = new UserManagementTableModel();
        userManagementTableModel.setParentManager(this);
        userManagementTable = new JTable(userManagementTableModel);
        userManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = new JScrollPane(userManagementTable);
        add(tableScroll);

        // add groups pulldown selection for groups this person is a member of
        JButton editUserButton = new JButton("Edit User");
        editUserButton.addActionListener(event -> editUser());
        JButton newUserButton = new JButton("New User");
        newUserButton.addActionListener(event -> newUser());
        JPanel actionPanel = new JPanel();
        actionPanel.add(editUserButton);
        actionPanel.add(newUserButton);
        add(actionPanel);
    }

    public void editUser() {
        User user = userManagementTableModel.getUserAtRow(userManagementTable.getSelectedRow());
        parent.viewUserDetails(user);
    }

    public void newUser() {
        User newUser = new User();
        parent.viewUserDetails(newUser);
    }
    
    public void returnHome() {
        parent.viewTopMenu();
    }

    /*
      loads users based off your permissions.. 
    If you are a workstation admin, you see everybody
    Group admins only see users in groups in which they are the owners.  
    */
    private void loadUsers () {
        try {
            if (AccessManager.getAccessManager().isAdmin()) {
                List<Subject> subjectList = DomainMgr.getDomainMgr().getSubjects();
                List<User> userList = new ArrayList<>();
                for (Subject subject: subjectList) {
                    if (subject instanceof User)
                        userList.add((User)subject);
                }
                userManagementTableModel.addUsers(userList);
            }
        } catch (Exception e) {
            String errorMessage = "Problem retrieving user information";
            log.error(errorMessage);
            e.printStackTrace();
        }
    }
        
    // loads the User Details page in add mode
    private void addUser() {
        
    }
    
    // remove the User from the  as well as the table model
    private void deleteUser(Subject user) {
        
    }
    
    class UserManagementTableModel extends AbstractTableModel {
        String[] columnNames = {"Username", "Groups"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<User> users = new ArrayList<>();
        private UserManagementPanel manager;
        
        public int getColumnCount() {
            return columnNames.length;
        }        

        public int getRowCount() {
            return data.size();
        }
        
        public void clear() {
            data = new ArrayList<List<Object>>();
            users = new ArrayList<>();
        }
        
        public void addUsers(List<User> userList) {
            data = new ArrayList<List<Object>>();
            users = new ArrayList<>();
            ArrayList userRow;
            for (User user: userList) {
               userRow = new ArrayList<>();
               userRow.add(user.getFullName());
               String groups = user.getReadGroups().toString();
               groups = groups.replaceAll("group:", "");
               userRow.add(groups);
               data.add(userRow);
               users.add(user); 
            }            
            fireTableRowsInserted(0, data.size()-1);
        }
        
        public void addUser (User user) {
            ArrayList userRow = new ArrayList<>();
            userRow.add(user.getFullName());
            data.add(userRow);
            users.add(user);
            fireTableRowsInserted(data.size()-1, data.size()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }
        
        // kludgy way to store the Subject at the end of the row in a hidden column
        public User getUserAtRow(int row) {
            return users.get(row);
        }
        
        public void removeUser(int row) {
            data.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        private void setParentManager(UserManagementPanel parentManager) {
           manager = parentManager;
        }
        
    }

}
