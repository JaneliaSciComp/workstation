/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Help",
        id = "ReportABugMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_ReportABugMenuAction"
)
@ActionReference(path = "Menu/Help", position = 110)
@Messages("CTL_ReportABugMenuAction=Report A Bug")
public final class ReportABugMenuAction extends AbstractAction implements Presenter.Menu {

    JMenuItem bugReport = new JMenuItem("Report A Bug");

    public ReportABugMenuAction() {
        bugReport.addActionListener( this );
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        new ReportABugDelegate().actOnCallDeveloper();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return bugReport;
    }
}
