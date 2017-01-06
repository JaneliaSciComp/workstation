package org.janelia.it.workstation.access.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.MembershipTablePanel;
import org.janelia.it.workstation.browser.gui.support.MembershipTablePanel.ColumnRenderer;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for editing group membership and other group attributes. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupEditorPanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(GroupEditorPanel.class);

    private JLabel keyInput;
    private JTextField nameInput;
    private MembershipTablePanel<User> membersPanel;
    private JButton saveButton;
    
    private List<Subject> subjects;
    private Group group;
    
    public GroupEditorPanel(List<Subject> subjects) {

        this.subjects = subjects;
        
        setLayout(new BorderLayout());
        
        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();
        
        attrPanel.addSeparator("Group Attributes");
        
        keyInput = new JLabel();
        attrPanel.addItem("Group Name", keyInput);
        
        nameInput = new JTextField(30);
        nameInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }
            public void removeUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }
            public void insertUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }
        });
        attrPanel.addItem("Full Name", nameInput);
        
        attrPanel.addSeparator("Group Membership");
        
        membersPanel = new MembershipTablePanel<User>() {
            
            protected User showAddItemDialog() {
                UserRoleDialog<Group> dialog = new UserRoleDialog<Group>(subjects, group);
                dialog.showForNewRole();
                saveButton.setEnabled(true);
                return null;
            }
            
            public void populatePopupMenu(JPopupMenu menu, User user) {

                JMenuItem changeRoleItem = new JMenuItem("  Change Role");
                changeRoleItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // TODO: show edit dialog
                        saveButton.setEnabled(true);
                    }
                });
                menu.add(changeRoleItem);

                JMenuItem removeItem = new JMenuItem("  Remove From Group");
                removeItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        User selectedUser = getSelectedItem();
                        removeItemFromList(selectedUser);
                        saveButton.setEnabled(true);
                    }
                });
                menu.add(removeItem);
            }
            
        };
        
        membersPanel.getAddItemButton().setText("Add user");
        
        membersPanel.addColumn("Username", new ColumnRenderer<User>() {
            public Object render(User user) {
                return user.getKey();
            }
        });
        membersPanel.addColumn("Full Name", new ColumnRenderer<User>() {
            public Object render(User user) {
                return user.getFullName();
            }
        });
        membersPanel.addColumn("Role", new ColumnRenderer<User>() {
            public Object render(User user) {
                UserGroupRole ugr = user.getRole(group.getKey());
                return ugr.getRole().getLabel();
            }
        });
        
        //membersPanel.updateView();
        
        attrPanel.addItem(membersPanel, "filly");

        saveButton = new JButton("Save Changes");
        saveButton.setEnabled(false);
        saveButton.setToolTipText("Save changes");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(saveButton);
        buttonPane.add(Box.createHorizontalGlue());
        
        add(attrPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    private boolean isEditable() {
        if (AccessManager.isAdmin()) return true;
        Subject subject = AccessManager.getAccessManager().getSubject();
        if (subject instanceof User) {
            User user = (User)subject;
            UserGroupRole ugr = user.getRole(group.getKey());
            log.info("Found role: {}",ugr);
            return ugr!=null && ugr.getRole().isAdmin();
        }
        return false;
    }

    public void loadGroup(final Group group) {

        this.group = group;
        Utils.setWaitingCursor(GroupEditorPanel.this);

        boolean editable = isEditable();
        saveButton.setEnabled(editable);
        membersPanel.setEditable(editable);
        
        SimpleWorker worker = new SimpleWorker() {

            private final List<Subject> subjects = new ArrayList<>();
            private final List<User> groupSubjects = new ArrayList<>();
            
            @Override
            protected void doStuff() throws Exception {
                
                for (Subject subject : DomainMgr.getDomainMgr().getSubjects()) {
                    if (subject instanceof User) {
                        User user = (User)subject;
                        if (user.hasGroupRead(group.getKey())) {
                            groupSubjects.add(user);
                        }
                        else {
                            subjects.add(subject);        
                        }
                    }
                }

                DomainUtils.sortSubjects(subjects);
                DomainUtils.sortSubjects(groupSubjects);
            }

            @Override
            protected void hadSuccess() {
                
                keyInput.setText(group.getKey());
                nameInput.setText(group.getFullName());
                for (User user : groupSubjects) {
                    membersPanel.addItemToList(user);
                }
                membersPanel.updateView();
                
                Utils.setDefaultCursor(GroupEditorPanel.this);
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(GroupEditorPanel.this);
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();        
    }
    
    private void saveChanges() {

        if (!isEditable()) {
            throw new IllegalStateException("Current user does not have permission to edit this group");
        }
        
        
        group.setFullName(nameInput.getText());
        // TODO: persist group
        
        for(Subject groupMember : membersPanel.getItemsInList()) {
//            groupMember.getGroups().add(group.getKey());
            // TODO: persist group member
        }
        
    }
}
