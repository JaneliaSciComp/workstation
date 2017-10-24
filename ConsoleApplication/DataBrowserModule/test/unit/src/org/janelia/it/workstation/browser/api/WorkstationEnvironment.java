package org.janelia.it.workstation.browser.api;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.openide.LifecycleManager;

/**
 * Helps mock out the Workstation well enough to carry out tests. Logs in, etc.
 * Created by fosterl on 1/27/14.
 */
public class WorkstationEnvironment {
    public void invoke() {
        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();

        // Assuming that the user has entered the login/password information, now validate
        String username = (String) FrameworkImplProvider.getModelProperty(AccessManager.USER_NAME);
        String password = (String) FrameworkImplProvider.getModelProperty(AccessManager.USER_PASSWORD);
        String runAsUser = (String) FrameworkImplProvider.getModelProperty(AccessManager.RUN_AS_USER);

        if (username==null) {
            Object[] options = {"Enter Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                throw new IllegalStateException("Please enter your login information");
            }
            else {
                LifecycleManager.getDefault().exit(0);
            }
        }

        AccessManager.getAccessManager().loginSubject(username, password);
        AccessManager.getAccessManager().setRunAsUser(runAsUser);
    }
}

