package org.janelia.it.FlyWorkstation.gui;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettingsPanel;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;

import javax.swing.*;

/**
 * Helps mock out the Workstation well enough to carry out tests. Logs in, etc.
 * Created by fosterl on 1/27/14.
 */
public class WorkstationEnvironment {
    public void invoke() {
        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();

        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");

        // Assuming that the user has entered the login/password information, now validate
        String username = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
        String email = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL);

        if (username==null || email==null) {
            Object[] options = {"Enter Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                PrefController.getPrefController().getPrefInterface(DataSourceSettingsPanel.class, null);
            }
            else {
                SessionMgr.getSessionMgr().systemExit();
            }
        }

        SessionMgr.getSessionMgr().loginSubject();
        SessionMgr.getSessionMgr().newBrowser();
    }
}

