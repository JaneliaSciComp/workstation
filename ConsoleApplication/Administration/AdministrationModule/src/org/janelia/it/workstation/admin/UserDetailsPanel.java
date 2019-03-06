package org.janelia.it.workstation.admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.model.security.GroupRole;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserDetailsPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    private int COLUMN_NAME = 1;
    private int COLUMN_VALUE = 2;
    
    public void setupUI() {
        // display table of user editable attributes
        
        // show group edit table with permissions for that group
        
        // add groups pulldown selection

    }
    
    class UserDetailsTableModel extends AbstractTableModel implements ActionListener {
        String[] columnNames = {"Property","Value"};
        User user;
        String[] editProperties = {"Name", "Email"};
        String[] values = new String[editProperties.length];
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return editProperties.length;
        }
        
        public void loadUser(User user) throws Exception {
            this.user = user;
            for (int i=0; i<editProperties.length; i++) {
               String value = (String)new PropertyDescriptor(editProperties[i], User.class).getReadMethod().invoke(user);
               values[i] = value;                        
            }
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {
                return editProperties[row];               
            } else {                
                JTextField editBox = new JTextField();
                editBox.setActionCommand(editProperties[row]);
                editBox.addActionListener(this);
                return editBox;
            }            
        }

        public Class getColumnClass(int c) {
            if (c==COLUMN_NAME) {
                return String.class;               
            } else {
                return JTextField.class;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String newValue = ((JTextField)e.getSource()).getText();
            if (newValue!=null) {
                try {
                    new PropertyDescriptor(e.getActionCommand(), User.class).getWriteMethod().invoke(user, newValue);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMessage = "Problem updating user information using reflection";
                    log.error(errorMessage);
                }
            }
        }
        
    }
    
    class GroupRolesModel extends AbstractTableModel implements ActionListener {
        String[] columnNames = {"Group Name","Role"};
        String[] roleOptions = { "Owner", "Admin", "Write", "Read"};
        List<UserGroupRole> data = new ArrayList<>();
        User user;
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return data.size();
        }
        
        public void loadGroupRoles(User user) {
            this.user = user;
            data.addAll(user.getUserGroupRoles());
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            if (col==COLUMN_NAME) {                
                return data.get(row).getGroupKey();
            } else {
                JComboBox roleSelection = new JComboBox(roleOptions);
                roleSelection.setActionCommand(data.get(row).getGroupKey());
                roleSelection.addActionListener(this);
                roleSelection.setSelectedItem(data.get(row).getRole().toString());
                return roleSelection;
            }
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UserGroupRole groupRole = user.getRole(e.getActionCommand());
            groupRole.setRole(GroupRole.valueOf((String)((JComboBox)e.getSource()).getSelectedItem()));                                    
        }
        
    }
}
