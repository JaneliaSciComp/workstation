package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 */
public class ViewMenu extends JMenu {
    Browser console;
    JMenuItem ontologyMenuItem;
    JMenuItem dataMenuItem;
    JMenuItem alignBoardItem;

    public ViewMenu(Browser console) {
        super("View");
        this.setMnemonic('V');
        this.console = console;

        ontologyMenuItem = new JCheckBoxMenuItem("Ontology Editor", true);
        ontologyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_ONTOLOGY);
            }
        });
        ontologyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.Event.META_MASK));

        dataMenuItem = new JCheckBoxMenuItem("Data Panel", true);
        dataMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_OUTLINES);
            }
        });
        dataMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, java.awt.Event.META_MASK));

        /*
          LLF: commenting until full functionality achieved.

        alignBoardItem = new JCheckBoxMenuItem("Alignment Board", true);
        alignBoardItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_ALIGNBOARD);
            }
        });
        alignBoardItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, java.awt.Event.META_MASK));
        */

        addMenuItems();
    }

    private void addMenuItems() {
        removeAll();
        add(dataMenuItem);
        add(ontologyMenuItem);
        /*
        LLF: commenting until full functionality achieved.
        add(alignBoardItem);
        */
    }

    private void viewActionPerformed(String viewComponent) {
        console.toggleViewComponentState(viewComponent);
    }

}
