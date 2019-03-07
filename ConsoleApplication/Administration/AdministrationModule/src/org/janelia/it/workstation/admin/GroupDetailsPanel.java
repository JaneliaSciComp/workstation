package org.janelia.it.workstation.admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.model.security.Group;
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
public class GroupDetailsPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    UserListTableModel userListModel;
    JTable userListTable;
    
    private int COLUMN_NAME = 1;
    private int COLUMN_VALUE = 2;
    
    public GroupDetailsPanel() {
        setupUI();
    }
    
    public void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        // display table listing users
        userListModel = new UserListTableModel();
        userListTable = new JTable(userListModel);
        add (userListTable);
       
    }
    
    public void viewGroupDetails(Group group) throws Exception {
         //DomainMgr.getDomainMgr().getSubjectFacade().getGroups();
    }
     
    
    class UserListTableModel extends AbstractTableModel {
        String[] columnNames = {"User","Role"};
        Group group;
        List<String[]> members = new ArrayList<>();
        
        public int getColumnCount() {
            return 2;
        }        

        public int getRowCount() {
            return members.size();
        }
        
        public void loadGroup(Group group, List<String[]> members) throws Exception {
            this.group = group;
            members = members;            
            fireTableRowsInserted(0, getRowCount()-1);
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return members.get(row)[col];                                   
        }

        public Class getColumnClass(int c) {
            return String.class;               
        }
    }
    
}
