package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.dialogs.DataCircleDialog;
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
    private JMenuItem dataCircleMenuItem;

    public ServicesMenu(Browser console) {
        super("Services");

        neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service...");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getRunNeuronSeparationDialog().showDialog();
            }
        });

        dataCircleMenuItem = new JMenuItem("Data Circle Manager...");
        dataCircleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new DataCircleDialog();
                dialog.setVisible(true);
            }
        });

        // Add the tools
        add(dataCircleMenuItem);
        add(neuronSeparationMenuItem);
    }
}
