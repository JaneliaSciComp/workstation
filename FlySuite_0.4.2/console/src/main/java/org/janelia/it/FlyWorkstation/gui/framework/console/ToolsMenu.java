package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class ToolsMenu extends JMenu {
    private JMenuItem vaa3dMenuItem;
    private JMenuItem fijiMenuItem;
    private JFrame parentFrame;

    public ToolsMenu(Browser console) {
        super("Tools");
        try {
            this.parentFrame = console;
            // todo This needs to be customized
            vaa3dMenuItem = new JMenuItem("Vaa3D - NeuroAnnotator", Utils.getClasspathImage("v3d_16x16x32.png"));
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        // todo This needs to be a custom user setting.
                        Runtime.getRuntime().exec("/Users/" + SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME) + "/Dev/v3d/v3d/v3d64.app/Contents/MacOS/v3d64");
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(vaa3dMenuItem.getParent(), "Could not launch Vaa3D - NeuroAnnotator", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            ImageIcon fijiImageIcon = Utils.getClasspathImage("fijiicon.png");
            Image img = fijiImageIcon.getImage();
            Image newimg = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            fijiImageIcon = new ImageIcon(newimg);
            fijiMenuItem = new JMenuItem("FIJI", fijiImageIcon);
            fijiMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        // todo This needs to be a custom user setting.
                        Runtime.getRuntime().exec("/Applications/Fiji.app/Contents/MacOS/fiji-macosx");
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(fijiMenuItem.getParent(), "Could not launch Fiji", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            JMenuItem toolsConfiguration = new JMenuItem("Configure Tools...");
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

            // Add the tools
            add(fijiMenuItem);
            add(vaa3dMenuItem);
            addSeparator();
            add(toolsConfiguration);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
