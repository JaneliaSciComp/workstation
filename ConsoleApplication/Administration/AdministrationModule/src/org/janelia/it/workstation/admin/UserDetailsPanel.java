package org.janelia.it.workstation.admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schauderd
 */
public class UserDetailsPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    private int COLUMN_PROPERTYNAME = 1;
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
            if (col==COLUMN_PROPERTYNAME) {
                return editProperties[row];               
            } else {                
                JTextField editBox = new JTextField();
                editBox.setActionCommand(editProperties[row]);
                editBox.addActionListener(this);
                return editBox;
            }            
        }

        public Class getColumnClass(int c) {
            if (c==COLUMN_PROPERTYNAME) {
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
    
    /* class GroupRolesModel extends AbstractTableModel {
        String[] columnNames = {"Group Name","Role"};    
        List<GroupRole> groupRoles
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return editProperties.length;
        }
        
        public void loadGroupRoles(List<r user) {
            this.user = user;
            for (int i=0; i<editProperties.length; i++) {
                PropertyDescriptor pd = new PropertyDescriptor(editProperties[i], )
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
        
    }*/
}
