package org.janelia.workstation.admin;

import org.janelia.model.security.Group;
import org.janelia.model.security.User;
import org.janelia.model.security.dto.AuthenticationRequest;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.facade.interfaces.SubjectFacade;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.util.List;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.admin//AdministrationTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = AdministrationTopComponent.PREFERRED_ID,
        iconBase = "org/janelia/workstation/admin/images/group16.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.workstation.admin.AdministrationTopComponent")
@ActionReference(path = "Menu/Window/Core" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AdministrationTopComponentAction",
        preferredID = AdministrationTopComponent.PREFERRED_ID
)
@Messages({
        "CTL_AdministrationTopComponentAction=Administration Tool",
        "CTL_AdministrationTopComponent=" + AdministrationTopComponent.LABEL_TEXT,
        "HINT_AdministrationTopComponent=Administration Tool"
})
public final class AdministrationTopComponent extends TopComponent {
    public static final String PREFERRED_ID = "AdministrationTopComponent";
    public static final String LABEL_TEXT = "Administration Tool";

    private JPanel topMenu;

    public AdministrationTopComponent() {
        setupGUI();
        setName(Bundle.CTL_AdministrationTopComponent());
        setToolTipText(Bundle.HINT_AdministrationTopComponent());

    }

    public static final AdministrationTopComponent getInstance() {
        return (AdministrationTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }

    private void setupGUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.topMenu = new JPanel();
        BoxLayout layout = new BoxLayout(topMenu, BoxLayout.X_AXIS);
        topMenu.setLayout(layout);

        // top level buttons
        JButton listUsersButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/user.png"));
        listUsersButton.setText("Users");
        listUsersButton.setToolTipText("Show all users");
        listUsersButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        listUsersButton.setHorizontalTextPosition(SwingConstants.CENTER);
        listUsersButton.addActionListener(event -> viewUserList());
        topMenu.add(listUsersButton);

        topMenu.add(Box.createHorizontalStrut(20));

        JButton listGroupsButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/group.png"));
        listGroupsButton.setText("Groups");
        listGroupsButton.addActionListener(event -> viewGroupList());
        listGroupsButton.setToolTipText("Show all groups");
        listGroupsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        listGroupsButton.setHorizontalTextPosition(SwingConstants.CENTER);
        topMenu.add(listGroupsButton);

        add(topMenu);
        add(Box.createVerticalGlue());

        revalidate();
    }

    public void viewUserList() {
        UserManagementPanel panel = new UserManagementPanel(this);
        removeAll();
        add(panel);
        revalidate();
        repaint();
    }

    public void viewTopMenu() {
        removeAll();
        add(topMenu);
        revalidate();
        repaint();
    }

    public void viewUserDetails(User user) {
        try {
            UserDetailsPanel panel = new UserDetailsPanel(this);
            panel.editUserDetails(user);
            removeAll();
            add(panel);
            revalidate();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public void createNewGroup() {
        try {
            NewGroupPanel panel = new NewGroupPanel(this);
            panel.initNewGroup();
            removeAll();
            add(panel);
            revalidate();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public void viewGroupList() {
        GroupManagementPanel panel = new GroupManagementPanel(this);
        removeAll();
        add(panel);
        revalidate();
    }

    public void viewGroupDetails(String groupKey) {
        try {
            GroupDetailsPanel panel = new GroupDetailsPanel(this, groupKey);
            List<User> users = DomainMgr.getDomainMgr().getUsersInGroup(groupKey);
            panel.editGroupDetails(groupKey, users);
            removeAll();
            add(panel);
            revalidate();
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    /**
     * Persistence section bubbling up from all the panels
     */
    public void saveUserRoles(User user) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            subjectFacade.updateUserRoles(user.getKey(), user.getUserGroupRoles());
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public User createUser(User user) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            AuthenticationRequest message = new AuthenticationRequest();
            message.setUsername(user.getName());
            message.setPassword(user.getPassword());

            User newUser = subjectFacade.updateUser(user);

            // make sure to change password
            subjectFacade.changeUserPassword(message);

            // set up a user default directory

            // if mail set up, register the user's email address
            return newUser;
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        return null;
    }

    public void saveUser(User user, boolean passwordChange) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            if (user.getId() == null) {
                AuthenticationRequest message = new AuthenticationRequest();
                message.setUsername(user.getName());
                message.setPassword(user.getPassword());

                user = subjectFacade.updateUser(user);

                // make sure to change password
                subjectFacade.changeUserPassword(message);
            } else {
                subjectFacade.updateUser(user);
                if (passwordChange) {
                    AuthenticationRequest message = new AuthenticationRequest();
                    message.setUsername(user.getName());
                    message.setPassword(user.getPassword());
                    subjectFacade.changeUserPassword(message);
                }
            }
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public void createGroup(Group group) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            subjectFacade.createGroup(group);
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Override
    public void componentOpened() {

    }

    @Override
    public void componentClosed() {
    }


    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

}
