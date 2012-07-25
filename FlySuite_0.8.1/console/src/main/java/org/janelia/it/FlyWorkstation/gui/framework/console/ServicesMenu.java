package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ServicesMenu extends JMenu {
    private JMenuItem neuronSeparationMenuItem;

    public ServicesMenu(Browser console) {
        super("Services");

        neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service...");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getRunNeuronSeparationDialog().showDialog();
            }
        });

        // Add the tools
        add(neuronSeparationMenuItem);
    }
}
