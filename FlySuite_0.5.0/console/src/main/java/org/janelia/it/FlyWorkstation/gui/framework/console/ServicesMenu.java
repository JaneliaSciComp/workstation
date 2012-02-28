package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.dialogs.RunNeuronSeparationDialog;

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
    private JFrame parentFrame;
    private static RunNeuronSeparationDialog runNeuronSeparationDialog = new RunNeuronSeparationDialog();

    public ServicesMenu(Browser console) {
        super("Services");
        this.parentFrame = console;

        neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service...");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                runNeuronSeparationDialog.showDialog();
            }
        });

        // Add the tools
        add(neuronSeparationMenuItem);
    }
}
