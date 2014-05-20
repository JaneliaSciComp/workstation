/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import javax.swing.JFrame;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.MailDialogueBox;
import org.janelia.it.FlyWorkstation.gui.util.WindowLocator;

/**
 *
 * @author fosterl
 */
public class CallDeveloperDelegate {
    public void actOnCallDeveloper() {
        JFrame parentFrame = (JFrame)WindowLocator.getMainFrame();
        MailDialogueBox popup = new MailDialogueBox(parentFrame,
                (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
                "Bug report");
        popup.showPopupThenSendEmail();
    }
}
