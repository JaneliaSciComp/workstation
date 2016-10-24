package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.MailDialogueBox;
import org.janelia.it.workstation.gui.util.WindowLocator;

import javax.swing.*;

/**
 *
 * @author fosterl
 */
public class ReportABugDelegate {
    public void actOnCallDeveloper() {
        JFrame parentFrame = WindowLocator.getMainFrame();
        MailDialogueBox popup = new MailDialogueBox(parentFrame,
                (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
                "Bug report");
        popup.showPopupThenSendEmail();
    }
}
