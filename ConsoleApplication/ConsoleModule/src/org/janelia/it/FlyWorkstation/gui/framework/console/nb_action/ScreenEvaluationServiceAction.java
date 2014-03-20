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

//@ActionID(
//        category = "Services",
//        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.ScreenEvaluationServiceAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_ScreenEvaluationServiceAction"
//)
//@ActionReference(path = "Menu/Services", position = 1100)
//@Messages("CTL_ScreenEvaluationServiceAction=Screen Evaluation")
public final class ScreenEvaluationServiceAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new ServicesActionDelegate().presentScreenEvalDialog();
    }
}
