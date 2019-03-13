package org.janelia.it.workstation.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Component;
import org.janelia.model.security.User;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;


/**
 * Top component which displays something.
 */


@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.admin//AdministrationTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = AdministrationTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.admin.AdministrationTopComponent")
@ActionReference(path = "Menu/Window/Administration" /*, position = 111 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AdministrationTopComponentAction",
        preferredID = AdministrationTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_AdministrationTopComponentAction=Administration GUI",
    "CTL_AdministrationTopComponent=" + AdministrationTopComponent.LABEL_TEXT,
    "HINT_AdministrationTopComponent=Administration GUI"
})
public final class AdministrationTopComponent extends TopComponent {
    public static final String PREFERRED_ID = "AdministrationTopComponent";
    public static final String LABEL_TEXT = "Administration Tool";

    public AdministrationTopComponent() {
        setupGUI();
        setName(Bundle.CTL_AdministrationTopComponent());
        setToolTipText(Bundle.HINT_AdministrationTopComponent());

    }
    
    public static final AdministrationTopComponent getInstance() {
        return (AdministrationTopComponent)WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }
    
    private void setupGUI() {
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      
        // top level buttons
        JButton listUsersButton = new JButton("List Users");
        listUsersButton.addActionListener(event -> viewUserList());
        add(listUsersButton);        
        listUsersButton.setToolTipText("View Users visible to you");
        JButton listGroupsButton = new JButton("List Groups");
        add(listGroupsButton);        
        listGroupsButton.setToolTipText("View groups visible to you");

    }
    
    public void viewUserList() {
        UserManagementPanel panel = new UserManagementPanel(this);
        removeAll();
        add(panel);
        revalidate();
    }
    
    public void viewUserDetails(User user) {         
        try {
            UserDetailsPanel panel = new UserDetailsPanel(this);
            panel.editUserDetails(user);
            removeAll();
            add(panel);
            revalidate();
        } catch (Exception e) {
            e.printStackTrace();
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
