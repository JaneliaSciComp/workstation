package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 */
public class ViewMenu extends JMenu {

    public ViewMenu(final Browser browser) {
        super("View");
        this.setMnemonic('V');
        
        JMenuItem dataMenuItem = new JCheckBoxMenuItem("Data Panel", true);
        dataMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browser.toggleViewComponentState(Browser.VIEW_OUTLINES);
            }
        });
        dataMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, SystemInfo.isMac?Event.META_MASK:Event.CTRL_MASK));
        add(dataMenuItem);

        JMenuItem ontologyMenuItem = new JCheckBoxMenuItem("Ontology Editor", true);
        ontologyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browser.toggleViewComponentState(Browser.VIEW_ONTOLOGY);
            }
        });
        ontologyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, SystemInfo.isMac?Event.META_MASK:Event.CTRL_MASK));
        add(ontologyMenuItem);
        

        JMenuItem perspectivesMenu = new JMenu("Open Perspective");
        
        for(final Perspective perspective : Perspective.values()) {
        	if (perspective==Perspective.AlignmentBoard) continue;

            JMenuItem perspectiveMenuItem = new JMenuItem(perspective.getName());
            perspectiveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    browser.setPerspective(perspective);
                }
            });
            perspectivesMenu.add(perspectiveMenuItem);
        }
        
        add(perspectivesMenu);
        
        JMenuItem resetWindow = new JMenuItem("Reset Window");
        resetWindow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getBrowser().resetWindow();
            }
        });
        resetWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, SystemInfo.isMac?Event.META_MASK:Event.CTRL_MASK));
        add(resetWindow);
    }
}
