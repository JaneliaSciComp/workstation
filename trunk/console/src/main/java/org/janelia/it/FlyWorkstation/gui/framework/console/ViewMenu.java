package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:16 PM
 */
public class ViewMenu extends JMenu {
    Browser console;
    JMenuItem searchMenuItem;
    JMenuItem ontologyMenuItem;
    JMenuItem outlinesMenuItem;

    public ViewMenu(Browser console) {
        super("View");
        this.setMnemonic('V');
        this.console = console;

        searchMenuItem = new JCheckBoxMenuItem("Search", false);
        searchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_SEARCH);
            }
        });

        ontologyMenuItem = new JCheckBoxMenuItem("Ontology Editor", true);
        ontologyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_ONTOLOGY);
            }
        });

        outlinesMenuItem = new JCheckBoxMenuItem("Outlines", true);
        outlinesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewActionPerformed(Browser.VIEW_OUTLINES);
            }
        });

        addMenuItems();
    }

    private void addMenuItems() {
        removeAll();

        add(searchMenuItem);
        add(ontologyMenuItem);
        add(outlinesMenuItem);

    }

    private void viewActionPerformed(String viewComponent) {
        console.toggleViewComponentState(viewComponent);
    }

}
