package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class SearchMenu extends JMenu {
    private JMenuItem searchMenuItem;
    private JMenuItem patternSearchMenuItem;
    private JMenuItem giantFiberSearchMenuItem;

    public SearchMenu(Browser console) {
        super("Search");

        searchMenuItem = new JMenuItem("Search");
        searchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	SessionMgr.getSessionMgr().getActiveBrowser().getGeneralSearchDialog().showDialog();
            }
        });

        patternSearchMenuItem = new JMenuItem("Pattern Annotation Search");
        patternSearchMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getPatternSearchDialog().showDialog();
            }
        });

        giantFiberSearchMenuItem = new JMenuItem("Giant Fiber Mask Search");
        giantFiberSearchMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SessionMgr.getSessionMgr().getActiveBrowser().getGiantFiberSearchDialog().showDialog();
            }
        });
        
        searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, SystemInfo.isMac?java.awt.Event.META_MASK:java.awt.Event.CTRL_MASK));
        
        // Add the tools
        add(searchMenuItem);
        add(patternSearchMenuItem);
        add(giantFiberSearchMenuItem);
    }
}
