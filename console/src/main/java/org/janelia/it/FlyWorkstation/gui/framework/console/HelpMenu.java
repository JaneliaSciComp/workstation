package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.util.MailDialogueBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class HelpMenu extends JMenu {

    JMenuItem bugReport = new JMenuItem("Report A Bug");

    public HelpMenu(Browser console) {
        super("Help");
        add(new JMenuItem("Call Christopher - x4662"));
        add(new JMenuItem("Call Konrad - x4242"));
        add(new JMenuItem("Call Ryan   - x4247"));
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
        MailDialogueBox popup = new MailDialogueBox("Bug report");
        popup.show();
    }
}
