package org.janelia.it.workstation.gui;

import javax.swing.*;

/**
 * Helps mock out the Workstation well enough to carry out tests. Logs in, etc.
 * Created by fosterl on 1/27/14.
 */
public class WorkstationEnvironment {
    public void invoke() {
        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        org.janelia.it.workstation.shared.util.ConsoleProperties.load();

        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager.registerFacade(org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager.getEJBProtocolString(), org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager.class, "JACS EJB Facade Manager");

        // Assuming that the user has entered the login/password information, now validate
        String username = (String) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.USER_NAME);
        String email = (String) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.USER_EMAIL);

        if (username==null || email==null) {
            Object[] options = {"Enter Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                org.janelia.it.workstation.gui.framework.pref_controller.PrefController.getPrefController().getPrefInterface(org.janelia.it.workstation.gui.util.panels.DataSourceSettingsPanel.class, null);
            }
            else {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().systemExit();
            }
        }

        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().loginSubject();
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().newBrowser();
    }
}

