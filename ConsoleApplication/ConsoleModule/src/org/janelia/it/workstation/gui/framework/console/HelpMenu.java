package org.janelia.it.workstation.gui.framework.console;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 * @deprecated moved to NetBeans main frame.
 */
public class HelpMenu extends JMenu {

    JMenuItem bugReport = new JMenuItem("Report A Bug");
    private JFrame parentFrame;

    public HelpMenu(Browser console) {
        super("Help");
        this.setMnemonic('H');
        parentFrame = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame();
        add(new JMenuItem("Call Christopher - x4662"));
        add(new JMenuItem("Call Don    - x4656"));
        add(new JMenuItem("Call Eric   - x4655"));
        add(new JMenuItem("Call Konrad - x4242"));
        add(new JMenuItem("Call Les    - x4680"));
        add(new JMenuItem("Call Sean   - x4324"));
        add(new JMenuItem("Call Todd   - x4696"));
        add(new JMenuItem("Call Yang   - x4626"));
        add(new JSeparator());
        add(bugReport);
        bugReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bugReport_actionPerformed();
            }
        });
    }

    private void bugReport_actionPerformed(){
        org.janelia.it.workstation.gui.util.MailDialogueBox popup = new org.janelia.it.workstation.gui.util.MailDialogueBox(parentFrame,
                (String) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.USER_EMAIL),
                "Bug report");
        popup.showPopupThenSendEmail();
    }
}
