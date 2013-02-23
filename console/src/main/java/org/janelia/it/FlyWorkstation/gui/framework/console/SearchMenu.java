package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.dialogs.MAASearchDialog;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;

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

    public SearchMenu(final Browser browser) {
        super("Search");

        JMenuItem searchMenuItem = new JMenuItem("Search");
        searchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	browser.getGeneralSearchDialog().showDialog();
            }
        });
        searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, SystemInfo.isMac?java.awt.Event.META_MASK:java.awt.Event.CTRL_MASK));
        add(searchMenuItem);
        
        JMenuItem patternSearchMenuItem = new JMenuItem("Pattern Annotation Search");
        patternSearchMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                browser.getPatternSearchDialog().showDialog();
            }
        });
        add(patternSearchMenuItem);

        JMenuItem giantFiberSearchMenuItem = new JMenuItem("Giant Fiber Mask Search");
        giantFiberSearchMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                browser.getGiantFiberSearchDialog().showDialog();
            }
        });
        add(giantFiberSearchMenuItem);

        final MAASearchDialog maaSearchDialog = browser.getMAASearchDialog();
        if (maaSearchDialog!=null && maaSearchDialog.isAccessible()) {
        	JMenuItem menuItem = new JMenuItem("MAA Screen Search");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	maaSearchDialog.showDialog();
                }
            });
            add(menuItem);
        }
    }
}
