package org.janelia.it.workstation.gui.framework.console;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;

import org.janelia.it.workstation.gui.framework.progress_meter.WorkerProgressMeter;

/**
 * Created by IntelliJ IDEA. User: saffordt Date: 2/8/11 Time: 1:07 PM
 */
public class ConsoleMenuBar extends JMenuBar {
    
    protected Browser console; // protected so that when subclasses, the subclasses menus can have access to it.
    protected WorkerProgressMeter meter;

    protected BookmarkMenu bookmarkMenu;
    //protected JMenu servicesMenu;
    //protected JMenu ontologyMenu;
    //protected JMenu helpMenu;
    //protected JMenu viewMenu;
    protected Component menuGlue = Box.createHorizontalGlue();

    public ConsoleMenuBar(Browser console) {
        this.console = console;
        this.meter = WorkerProgressMeter.getProgressMeter();
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        constructMenus();
        addMenus();
    }

    private void constructMenus() {
        //bookmarkMenu = new BookmarkMenu(console);
        //servicesMenu = new ServicesMenu(console);
        //ontologyMenu = new OntologyMenu(console);
        //helpMenu = new HelpMenu(console);
        //viewMenu = new ViewMenu(console);
    }
    
    private void addMenus() {
        // add(bookmarkMenu);
        //add(servicesMenu);
        //add(viewMenu);
        // add(ontologyMenu);
        //add(helpMenu);
        add(menuGlue);
        add(WorkerProgressMeter.getProgressMeter().getMenuLabel());
    }
}
