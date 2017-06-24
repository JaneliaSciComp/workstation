package org.janelia.it.workstation.access.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.model.domain.subjects.UserGroupRole;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.access.model.UserGroupRoleTemplate;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.AbstractChooser;
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
    
    private List<Subject> subjects;
    private Group group;
    
    public GroupEditorPanel(final List<Subject> subjects) {

        this.subjects = subjects;
        
        setLayout(new BorderLayout());
        
        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel("[][][][][][growprio 200]");
        
        attrPanel.addSeparator("Group Attributes"); // Row #1
        
        keyInput = new JLabel();
        attrPanel.addItem("Group Name", keyInput); // Row #2
        
        nameInput = new JTextField(30);
        attrPanel.addItem("Full Name", nameInput); // Row #3
        

        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Save changes");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    group.setFullName(nameInput.getText());
                    group = DomainMgr.getDomainMgr().saveSubject(group);
                }
                catch (Exception ex) {
                    FrameworkImplProvider.handleException(ex);
                }
            }
        });
        attrPanel.addItem(saveButton, "span 2"); // Row #4
        
        attrPanel.addSeparator("Group Membership"); // Row #5
        
        membersPanel = new MembershipTablePanel<User>() {
            
            @Override
            protected User showAddItemDialog() {
                showRoleDialog();
                // It's okay to return null here. 
                // We'll update the table asynchronously, after any database operations.
                return null; 
            }

            @Override
            public void populatePopupMenu(JPopupMenu menu, final User user) {

                JMenuItem changeRoleItem = new JMenuItem("  Change Role");
                changeRoleItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showRoleDialog();
                    }
                });
                menu.add(changeRoleItem);

                JMenuItem removeItem = new JMenuItem("  Remove From Group");
                removeItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeRoles(getSelectedItem());
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
        
        attrPanel.addItem(membersPanel, ""); // Row #5
        
        add(attrPanel, BorderLayout.CENTER);
    }
    
    private boolean isEditable() {
        Subject subject = AccessManager.getAccessManager().getSubject();
        if (subject instanceof User) {    
            User user = (User)subject;
            return (DomainUtils.hasAdminAccess(user, group));
        }
        return false;
    }

    public void loadGroup(final Group group) {

        this.group = group;
        Utils.setWaitingCursor(GroupEditorPanel.this);

        boolean editable = isEditable();
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
    
    private void showRoleDialog() {
        UserRoleDialog<Group, User> dialog = new UserRoleDialog<Group, User>(subjects, group, null, null);
        if (dialog.showDialog() == AbstractChooser.CHOOSE_OPTION) {
            final UserGroupRoleTemplate roleTemplate = dialog.getRoleTemplate();

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    DomainMgr.getDomainMgr().setUserGroupRole(roleTemplate.getUser(), group, roleTemplate.getRole());                            
                }

                @Override
                protected void hadSuccess() {
                    loadGroup(group);
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }
            };

            worker.execute();
        }
    }

    private void removeRoles(final User user) {
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainMgr.getDomainMgr().removeUserGroupRoles(user, group);                            
            }

            @Override
            protected void hadSuccess() {
                loadGroup(group);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
}
