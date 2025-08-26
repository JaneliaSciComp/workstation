package org.janelia.workstation.admin;

import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.security.Group;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.facade.interfaces.SubjectFacade;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Messages({
        "CTL_AdministrationTopComponentAction=Administration Tool",
        "CTL_AdministrationTopComponent=" + AdministrationTopComponent.LABEL_TEXT,
        "HINT_AdministrationTopComponent=Administration Tool"
})
public final class AdministrationTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(AdministrationTopComponent.class);

    public static final String PREFERRED_ID = "AdministrationTopComponent";
    public static final String LABEL_TEXT = "Administration Tool";

    private JPanel topMenu;
    private Refreshable currentView;

    public AdministrationTopComponent() {
        setupGUI();
        setName(Bundle.CTL_AdministrationTopComponent());
        setToolTipText(Bundle.HINT_AdministrationTopComponent());
        boolean enabled = AccessManager.getAccessManager().isAdmin();
        setEnabled(enabled);
    }

    public static AdministrationTopComponent getInstance() {
        return (AdministrationTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }

    private void setupGUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.topMenu = new JPanel();
        topMenu.setLayout(new BoxLayout(topMenu, BoxLayout.Y_AXIS)); // Main panel with vertical layout

        // First row with "Users" and "Groups"
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        // top level buttons
        JButton listUsersButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/user.png"));
        listUsersButton.setText("Users");
        listUsersButton.setToolTipText("Show all users");
        listUsersButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        listUsersButton.setHorizontalTextPosition(SwingConstants.CENTER);
        listUsersButton.addActionListener(event -> viewUserList());
        row1.add(listUsersButton);

        row1.add(Box.createHorizontalStrut(20)); // Add space between buttons

        JButton listGroupsButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/group.png"));
        listGroupsButton.setText("Groups");
        listGroupsButton.addActionListener(event -> viewGroupList());
        listGroupsButton.setToolTipText("Show all groups");
        listGroupsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        listGroupsButton.setHorizontalTextPosition(SwingConstants.CENTER);
        listGroupsButton.addActionListener(event -> viewGroupList());
        row1.add(listGroupsButton);

        row1.add(Box.createHorizontalStrut(20));
        JButton getLogsButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/logs.png"));
        getLogsButton.setText("Logs");
        getLogsButton.setToolTipText("Retrieve Logs");
        getLogsButton.addActionListener(event -> getLogs());
        getLogsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        getLogsButton.setHorizontalTextPosition(SwingConstants.CENTER);
        row1.add(getLogsButton);

        row1.add(Box.createHorizontalStrut(20));

        JButton workspaceCleanupButton = new JButton(UIUtils.getClasspathImage(this.getClass(), "/org/janelia/workstation/admin/images/clean.png"));
        workspaceCleanupButton.setText("Cleanup Db");
        workspaceCleanupButton.setToolTipText("Manage and delete large workspaces");
        workspaceCleanupButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        workspaceCleanupButton.setHorizontalTextPosition(SwingConstants.CENTER);
        workspaceCleanupButton.addActionListener(event -> databaseCleanup());
        row1.add(workspaceCleanupButton);

        // Add both rows to the main menu
        topMenu.add(row1);

        add(topMenu);
        add(Box.createVerticalGlue());

        revalidate();
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        if (currentView != null) currentView.refresh();
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            if (currentView != null) currentView.refresh();
        }
    }

    void databaseCleanup() {
        DatabaseCleanupPanel panel = new DatabaseCleanupPanel(this);
        removeAll();
        add(panel);
        revalidate();
        repaint();
        this.currentView = panel;
    }

    void viewUserList() {
        UserManagementPanel panel = new UserManagementPanel(this);
        removeAll();
        add(panel);
        revalidate();
        repaint();
        this.currentView = panel;
    }
    void getLogs() {
        RetrieveLogsPanel panel = new RetrieveLogsPanel(this);
        removeAll();
        add(panel);
        revalidate();
        repaint();
        this.currentView = panel;
    }

    void viewTopMenu() {
        removeAll();
        add(topMenu);
        revalidate();
        repaint();
        this.currentView = null;
    }

    void viewUserDetails(User user) {
        UserDetailsPanel panel = new UserDetailsPanel(this, user);
        removeAll();
        add(panel);
        revalidate();
        this.currentView = panel;
    }

    void createNewGroup() {
        NewGroupPanel panel = new NewGroupPanel(this);
        removeAll();
        add(panel);
        revalidate();
        this.currentView = panel;
    }

    void viewGroupList() {
        GroupManagementPanel panel = new GroupManagementPanel(this);
        removeAll();
        add(panel);
        revalidate();
        this.currentView = panel;
    }

    void viewGroupDetails(String groupKey) {
        GroupDetailsPanel panel = new GroupDetailsPanel(this, groupKey);
        removeAll();
        add(panel);
        revalidate();
        this.currentView = panel;
    }

    /**
     * Persistence section bubbling up from all the panels
     */
    void saveUserRoles(User user, Set<UserGroupRole> userGroupRoles) {
        try {
            log.info("Saving user roles for "+user);
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            subjectFacade.updateUserRoles(user.getKey(), userGroupRoles);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    User saveUser(User user, String plaintextPassword) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            log.info("Saving user {}", user);
            User updatedUser = user.getId()==null ? subjectFacade.createUser(user) : subjectFacade.updateUser(user);
            log.info("Updated user: {}", updatedUser);
            if (plaintextPassword != null) {
                updatedUser = subjectFacade.changeUserPassword(user.getName(), plaintextPassword);
                log.info("Updated user password: {}", updatedUser);
            }
            return updatedUser;
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        return null;
    }

    void createGroup(Group group) {
        try {
            SubjectFacade subjectFacade = DomainMgr.getDomainMgr().getSubjectFacade();
            subjectFacade.createGroup(group);
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Override
    public void componentOpened() {
        Events.getInstance().registerOnEventBus(this);
    }

    @Override
    public void componentClosed() {
        Events.getInstance().unregisterOnEventBus(this);
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

}
