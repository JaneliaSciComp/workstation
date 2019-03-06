package org.janelia.it.workstation.admin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import org.janelia.model.domain.tiledMicroscope.TmReviewItem;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.janelia.model.security.Subject;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserManagementPanel extends JPanel implements ActionListener {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    
    private JLabel titleLabel;
    private JLabel workspaceNameLabel;
    private JLabel sampleNameLabel;
    private JTable userManagementTable;   
    private int COLUMN_EDIT = 2;
    private int COLUMN_DELETE = 3;

    public UserManagementPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        titleLabel = new JLabel("User Management", JLabel.LEADING);       
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    
        UserManagementTableModel userManagementTableModel = new UserManagementTableModel();
        userManagementTableModel.setParentManager(this);
        userManagementTable = new JTable(userManagementTableModel);
        userManagementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);   
        userManagementTable.addMouseListener(new MouseHandler() {
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
                        Subject user = ((UserManagementTableModel) userManagementTable.getModel()).getUserAtRow(viewRow);
                        editUserProfile(user);
                    } else if (viewColumn == COLUMN_DELETE) {
                        Subject user = ((UserManagementTableModel) userManagementTable.getModel()).getUserAtRow(viewRow);
                        deleteUser(user);
                    }
                }
                me.consume();
            }
        });
        JScrollPane tableScroll = new JScrollPane(userManagementTable);
        add(tableScroll);
    }
    
    /*
      loads users based off your permissions.. 
    If you are a workstation admin, you see everybody
    Group admins only see users in groups in which they are the owners.  
    */
    private void loadUsers () {
        try {
            if (AccessManager.getAccessManager().isAdmin()) {
                List<Subject> userList = DomainMgr.getDomainMgr().getSubjects();
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
    
    // hide this panel and open up the User Details Panel
    private void editUserProfile(Subject user) {
        
    }
    
    // remove the User from the  as well as the table model
    private void deleteUser(Subject user) {
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("Edit".equals(command)) {
            
        } else if ("Delete".equals(command)) {
            
        } else if ("Add".equals(command)) {
            
        }
    }
    
    class UserManagementTableModel extends AbstractTableModel {
        String[] columnNames = {"Username",
                                "", ""};
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Subject> users = new ArrayList<>();
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
        
        public void addUsers(List<Subject> userList) {
            data = new ArrayList<List<Object>>();
            users = new ArrayList<>();
        }
        
        public void addUser (Subject user) {
            ArrayList userRow = new ArrayList<>();
            userRow.add(user.getFullName());
            users.add(user);
            fireTableRowsInserted(data.size()-1, data.size()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_EDIT) {
                JButton editUser = new JButton("Edit");                
                editUser.setActionCommand("Edit");
                editUser.addActionListener(manager);
                return editUser;
            } else if (col==COLUMN_DELETE) {
                JButton deleteUser = new JButton("Delete");
                deleteUser.setActionCommand("Delete");
                deleteUser.addActionListener(manager);
                return deleteUser;
            }
            return data.get(row).get(col);
        }
        
        // kludgy way to store the Subject at the end of the row in a hidden column
        public Subject getUserAtRow(int row) {
            return (Subject)data.get(row).get(1);
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
