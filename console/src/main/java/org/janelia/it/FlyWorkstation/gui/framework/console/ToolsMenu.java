package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ToolsMenu extends JMenu {
    private JMenuItem v3dMenuItem;
    private JMenuItem toolsConfiguration;
    private JFrame parentFrame;

    public ToolsMenu(ConsoleFrame console) {
        super("Tools");
        this.parentFrame = console;
        // todo This needs to be customized
        v3dMenuItem = new JMenuItem("V3D - NeuroAnnotator", new ImageIcon("/Users/saffordt/Dev/FlyWorkstation/console/target/classes/org/janelia/it/FlyWorkstation/gui/application/v3d_16x16x32.png"));
        v3dMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    // todo This needs to be a custom user setting.
                    Runtime.getRuntime().exec("/Users/saffordt/Dev/NeuroAnnotator/v3d/v3d64.app/Contents/MacOS/v3d64");
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(v3dMenuItem.getParent(),"Could not launch V3D - NeuroAnnotator",
                            "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);                }
            }
        });
        add(v3dMenuItem);

        toolsConfiguration = new JMenuItem("Configure Tools...");
        toolsConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    new ToolConfigurationDialog(parentFrame);
                }
                catch (BackingStoreException e) {
                    e.printStackTrace();
                }
            }
        });
        addSeparator();
        add(toolsConfiguration);
    }
}
