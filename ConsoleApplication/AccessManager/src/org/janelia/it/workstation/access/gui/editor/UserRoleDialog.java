package org.janelia.it.workstation.access.gui.editor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.browser.model.DomainObjectPermission;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for viewing and editing a user's role in a group.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserRoleDialog<T extends Subject> extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final JPanel attrPanel;
    private final JComboBox<Subject> subjectCombobox;
    private final JComboBox<String> roleCombobox;

    private List<Subject> allSubjects;
    private T subject;
    private boolean isUser;
    private Subject roleSubject;

    public UserRoleDialog(List<Subject> allSubjects, T subject) {

        this.allSubjects = allSubjects;
        this.subject = subject;
        this.isUser = subject instanceof User;
        
        setTitle("Set Group Role");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        addSeparator(attrPanel, subject instanceof User ? "User" : "Group");

        subjectCombobox = new JComboBox<>();
        subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a "+ (isUser?"group":"user"));
        subjectCombobox.setRenderer(new SubjectComboBoxRenderer());
        subjectCombobox.setMaximumRowCount(20);

        attrPanel.add(subjectCombobox, "gap para, span 2");

        roleCombobox = new JComboBox<>();
        roleCombobox.setEditable(false);
        roleCombobox.setToolTipText("Choose a role");
        roleCombobox.setMaximumRowCount(20);

        attrPanel.add(subjectCombobox, "gap para, span 2");
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    private void addSeparator(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span, gaptop 10lp");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    public void showForNewRole() {
        showDialog();
    }
    
    public void showForExistingRole(Subject roleSubject) {
        this.roleSubject = roleSubject;
        showDialog();
    }
    
    private void showDialog() {

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

        DefaultComboBoxModel<String> model2 = (DefaultComboBoxModel<String>) roleCombobox.getModel();
        model2.removeAllElements();
        for (GroupRole role : GroupRole.values()) {
            model2.addElement(role.getLabel());
        }
        if (currRole != null) {
            model2.setSelectedItem(currRole);
        }
        
        ActivityLogHelper.logUserAction("UserRoleDialog.showDialog", roleSubject);
        packAndShow();
    }

    private void saveAndClose() {

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        final String role = (String) roleCombobox.getSelectedItem();
        
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
//                if (dop == null) {
//                    dop = new DomainObjectPermission(domainObject, subject.getKey());    
//                }
//                dop.setRead(readCheckbox.isSelected());
//                dop.setWrite(writeCheckbox.isSelected());
//                model.changePermissions(domainObject, dop.getSubjectKey(), dop.getPermissions());
            }

            @Override
            protected void hadSuccess() {
//                parent.refresh();
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        //worker.setProgressMonitor(new IndeterminateProgressMonitor(parent, "Granting permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
