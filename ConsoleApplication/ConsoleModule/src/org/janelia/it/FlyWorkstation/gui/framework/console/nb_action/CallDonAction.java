/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.CallDonAction"
)
@ActionRegistration(
        displayName = "#CTL_CallDonAction"
)
@ActionReference(path = "Menu/Help", position = 1000)
@Messages("CTL_CallDonAction=Call Don         - x4656")
public final class CallDonAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        new CallDeveloperDelegate().actOnCallDeveloper();
    }
}
