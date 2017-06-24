package org.janelia.it.workstation.access.gui.editor;

import java.awt.Component;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.workstation.access.model.UserGroupRoleTemplate;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.support.AbstractChooser;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;

/**
 * A dialog for viewing and editing a user's role in a group.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserRoleDialog<T extends Subject, S extends Subject> extends AbstractChooser {

    private final GroupedKeyValuePanel attrPanel;
    private final JLabel subjectLabel;
    private final JComboBox<Subject> subjectCombobox;
    private final JComboBox<GroupRole> roleCombobox;

    private List<Subject> allSubjects;
    private T subject;
    private S roleSubject;
    private boolean isUser;
    private UserGroupRoleTemplate roleTemplate;

    public UserRoleDialog(List<Subject> allSubjects, T subject, S roleSubject, GroupRole role) {

        this.allSubjects = allSubjects;
        this.subject = subject;
        this.roleSubject = roleSubject;
        this.isUser = subject instanceof User;
        
        setTitle("Set Group Role");

        attrPanel = new GroupedKeyValuePanel();

        subjectLabel = new JLabel();
        
        subjectCombobox = new JComboBox<>();
        subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a "+ (isUser?"group":"user"));
        subjectCombobox.setRenderer(new SubjectComboBoxRenderer());
        subjectCombobox.setMaximumRowCount(20);

        if (isUser) {
            attrPanel.addItem("User", subjectLabel);
            attrPanel.addItem("Group", subjectCombobox);    
        }
        else {
            attrPanel.addItem("User", subjectCombobox);
            attrPanel.addItem("Group", subjectLabel);
        }
        
        roleCombobox = new JComboBox<>();
        roleCombobox.setEditable(false);
        roleCombobox.setToolTipText("Choose a role");
        roleCombobox.setMaximumRowCount(20);

        roleCombobox.setRenderer(new ListCellRenderer<GroupRole>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends GroupRole> list, 
                    GroupRole value, int index, boolean isSelected, boolean cellHasFocus) {
                return new JLabel(value.getLabel());
            }
        });
        
        attrPanel.addItem("Role", roleCombobox);
        
        addChooser(attrPanel);
    }
    
    public int showDialog() {

        subjectLabel.setText(subject.getFullName());
        
        DefaultComboBoxModel<Subject> model = (DefaultComboBoxModel<Subject>) subjectCombobox.getModel();
        model.removeAllElements();
        Subject currSubject = null;
        for (Subject subject : allSubjects) {
            if (isUser && subject instanceof User) continue;
            if (!isUser && subject instanceof Group) continue;
            model.addElement(subject);
            if (roleSubject != null && roleSubject.getId().equals(subject.getId())) {
                currSubject = subject;
            }
        }
        if (currSubject != null) {
            model.setSelectedItem(currSubject);
        }

        GroupRole currRole = null;
        if (roleSubject != null) {
            // Find current role
            User user = isUser ? (User)subject : (User)roleSubject;
            Group group = !isUser ? (Group)subject : (Group)roleSubject;
            UserGroupRole ugr = user.getRole(group.getKey());
            currRole = ugr.getRole();
        }

        DefaultComboBoxModel<GroupRole> model2 = (DefaultComboBoxModel<GroupRole>) roleCombobox.getModel();
        model2.removeAllElements();
        for (GroupRole role : GroupRole.values()) {
            model2.addElement(role);
        }
        
        if (currRole != null) {
            model2.setSelectedItem(currRole);
        }
        else {
            model2.setSelectedItem(GroupRole.Reader);
        }
        
        ActivityLogHelper.logUserAction("UserRoleDialog.showDialog", roleSubject);
        packAndShow();
        return getReturnValue();
    }

    @Override
    protected void choosePressed() {

        final Subject targetSubject = (Subject) subjectCombobox.getSelectedItem();
        final GroupRole role = (GroupRole) roleCombobox.getSelectedItem();
        
        if (isUser) {
            this.roleTemplate = new UserGroupRoleTemplate(
                    (User)subject, (Group)targetSubject, role);
        }
        else {
            this.roleTemplate = new UserGroupRoleTemplate(
                    (User)targetSubject, (Group)subject, role);
        }
        
        setVisible(false);
    }

    public UserGroupRoleTemplate getRoleTemplate() {
        return roleTemplate;
    }
}
