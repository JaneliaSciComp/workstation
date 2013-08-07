package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.janelia.it.FlyWorkstation.gui.framework.progress_meter.WorkerProgressMeter;

/**
 * Created by IntelliJ IDEA. User: saffordt Date: 2/8/11 Time: 1:07 PM
 */
public class ConsoleMenuBar extends JMenuBar {
    
    protected Browser console; // protected so that when subclasses, the subclasses menus can have access to it.
    protected WorkerProgressMeter meter;

    protected FileMenu fileMenu;
    protected EditMenu editMenu;
    protected BookmarkMenu bookmarkMenu;
    protected JMenu toolsMenu;
    protected JMenu servicesMenu;
    protected JMenu searchMenu;
    protected JMenu ontologyMenu;
    protected JMenu helpMenu;
    protected JMenu viewMenu;
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
        fileMenu = new FileMenu(console);
        editMenu = new EditMenu(console);
        bookmarkMenu = new BookmarkMenu(console);
        toolsMenu = new ToolsMenu(console);
        searchMenu = new SearchMenu(console);
        servicesMenu = new ServicesMenu(console);
        ontologyMenu = new OntologyMenu(console);
        helpMenu = new HelpMenu(console);
        viewMenu = new ViewMenu(console);
    }
    
    private void addMenus() {
        add(fileMenu);
        add(editMenu);
        add(searchMenu);
        // add(bookmarkMenu);
        add(toolsMenu);
        add(servicesMenu);
        add(viewMenu);
        // add(ontologyMenu);
        add(helpMenu);
        add(menuGlue);
        add(WorkerProgressMeter.getProgressMeter().getMenuLabel());
    }
}
