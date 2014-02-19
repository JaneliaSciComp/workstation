/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Simple class to launch the console NetBeans' way.
 * 
 * @author fosterl
 */
@ActionID(id="org.janelia.it.FlyWorkstation.nb_action.LaunchAppAction",category="Window")
@ActionRegistration(displayName = "#CTL_LaunchAppAction")
@ActionReferences({
    @ActionReference(path = "Menu/Window", position = 10)
})
@Messages("CTL_LaunchAppAction=Open Console App")
public class LaunchAppAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ConsoleApp().newBrowser();
            }
        });

    }
    
}
