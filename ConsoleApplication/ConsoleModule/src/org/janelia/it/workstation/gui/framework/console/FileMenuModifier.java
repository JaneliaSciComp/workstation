/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.console;

import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.janelia.it.workstation.gui.framework.console.nb_action.ExitAction;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes changes to file menu, depending on operating system.
 *
 * @author fosterl
 */
public class FileMenuModifier {
    private Logger logger = LoggerFactory.getLogger(FileMenuModifier.class);
    public FileMenuModifier(){}
    
    public void rebuildMenu() {
        if (SystemInfo.isMac) {
            // Establish exit strategy.
            JMenuItem exitMenuItem = new JMenuItem("Exit");
            exitMenuItem.addActionListener(new ExitAction());
            
            // Add this appropriately.
            JFrame frame = WindowLocator.getMainFrame();
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar != null) {
                JMenu fileMenu = null;
                for (int i = 0; i < menuBar.getMenuCount() && fileMenu == null; i++) {
                    JMenu menu = menuBar.getMenu(i);
                    if (menu.getText().trim().equalsIgnoreCase("file")) {
                        fileMenu = menu;
                        menu.add(exitMenuItem);
                        break;
                    }
                }
                if (fileMenu == null) {
                    logger.warn("Failed to find file menu");
                }
            } else {
                logger.warn("Failed to find menu bar from main frame.");
            }
        }
    }
    
}
