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
    private JMenuItem searchMenuItem;
    private JFrame parentFrame;
    
    public ServicesMenu(Browser console) {
        super("Services");
        this.parentFrame = console;

        neuronSeparationMenuItem = new JMenuItem("Neuron Separation Service...");
        neuronSeparationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getRunNeuronSeparationDialog().showDialog();
            }
        });

        searchMenuItem = new JMenuItem("Search...");
        searchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	SessionMgr.getSessionMgr().getActiveBrowser().getSearchDialog().showDialog();
            }
        });
        
        searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.Event.META_MASK));
        
        // Add the tools
        add(neuronSeparationMenuItem);
        add(searchMenuItem);
    }
}
