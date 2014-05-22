/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "ExitAction"
)
@ActionRegistration(
        displayName = "#CTL_ExitAction"
)
@ActionReference(path = "Menu/File", position = 3333)
@Messages("CTL_ExitAction=Exit")
public final class ExitAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame mainFrame = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame();
        WindowEvent wev = new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
    }
}
