package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.ActiveThreadModel;
import org.janelia.it.FlyWorkstation.gui.framework.progress_meter.WorkerProgressMeter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.MultiHash;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * Created by IntelliJ IDEA. User: saffordt Date: 2/8/11 Time: 1:07 PM
 */
public class ConsoleMenuBar extends JMenuBar {
    
    protected Browser console; // protected so that when subclasses, the subclasses menus can have access to it.
    protected WorkerProgressMeter meter;

    private MultiHash addedMenus = new MultiHash();

    protected JLabel imageLabel;
    protected ImageIcon staticIcon, animatedIcon;
    protected FileMenu fileMenu;
    protected EditMenu editMenu;
    protected BookmarkMenu bookmarkMenu;
    protected JMenu toolsMenu;
    protected JMenu servicesMenu;
    protected JMenu searchMenu;
    protected JMenu ontologyMenu;
    protected JMenu helpMenu;
    protected JMenu viewMenu;
    // protected JMenu windowMenu;
    protected Component menuGlue = Box.createHorizontalGlue();

    public ConsoleMenuBar(Browser console) {
        this.console = console;
        this.meter = WorkerProgressMeter.getProgressMeter();
        try {
            this.setAnimationIcon(Utils.getClasspathImage("fly_progress.gif"));
            this.setStaticIcon(Utils.getClasspathImage("fly_start.gif"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        constructMenus();
        addMenus();
        ActiveThreadModel.getActiveThreadModel().addObserver(new ThreadModelObserver());
        console.addBrowserObserver(new MenuBarBrowserObserver());
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
        imageLabel = new JLabel(staticIcon);
        imageLabel.addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                Point bp = SessionMgr.getBrowser().getLocation();
                Dimension bs = SessionMgr.getBrowser().getSize();
                Point tp = imageLabel.getLocation();
                int titleBarHeight = getSize().height; // Fudge the title bar height, since it's probably he same as the menu height 
                meter.setLocation(new Point(bp.x + bs.width - meter.getWidth(), bp.y + titleBarHeight + tp.y));
                meter.setVisible(true);
                modifyImageState(true);
            }
        });
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
        add(imageLabel);
    }

    // public void add(Component comp, Position pos) {
    // addedMenus.put(pos, comp);
    // redraw();
    // }

    public void remove(Component comp) {
        addedMenus.remove(comp);
        // Vector vec;
        // for (Enumeration e=addedMenus.elements();e.hasMoreElements();) {
        // vec=(Vector)e.nextElement();
        // if (vec.contains(comp)) {
        // vec.remove(comp);
        // break;
        // }
        // }
        redraw();
    }

    public void setAnimationIcon(ImageIcon animationIcon) {
        this.animatedIcon = animationIcon;
    }

    public void setStaticIcon(ImageIcon staticIcon) {
        this.staticIcon = staticIcon;
    }

    // private void setEditorSpecificMenus(JMenuItem[] menus) {
    // addedMenus.remove(EDITOR_SPECIFIC);
    // if (menus != null) for (int i = 0; i < menus.length; i++) {
    // addedMenus.put(EDITOR_SPECIFIC, menus[i]);
    // }
    // redraw();
    // }

    private void redraw() {
        // Vector menuList = new Vector();
        // Vector tmpVec;
        // if (addedMenus.containsKey(LEFT)) {
        // tmpVec = (Vector) addedMenus.get(LEFT);
        // menuList.addAll(tmpVec);
        // }
        // menuList.addElement(fileMenu);
        // if (addedMenus.containsKey(AFTER_FILE)) {
        // tmpVec = (Vector) addedMenus.get(AFTER_FILE);
        // menuList.addAll(tmpVec);
        // }
        // menuList.add(viewMenu);
        // if (addedMenus.containsKey(AFTER_VIEW)) {
        // tmpVec = (Vector) addedMenus.get(AFTER_VIEW);
        // menuList.addAll(tmpVec);
        // }
        // if (addedMenus.containsKey(EDITOR_SPECIFIC)) {
        // tmpVec = (Vector) addedMenus.get(EDITOR_SPECIFIC);
        // menuList.addAll(tmpVec);
        // }
        // menuList.add(windowMenu);
        // if (addedMenus.containsKey(AFTER_EDITOR_SPECIFIC)) {
        // tmpVec = (Vector) addedMenus.get(AFTER_EDITOR_SPECIFIC);
        // menuList.addAll(tmpVec);
        // }
        // menuList.add(menuGlue);
        // if (addedMenus.containsKey(AFTER_SPACING_GLUE)) {
        // tmpVec = (Vector) addedMenus.get(AFTER_SPACING_GLUE);
        // menuList.addAll(tmpVec);
        // }
        // if (staticImageLabel != null) if (!animation)
        // menuList.add(staticImageLabel);
        // else menuList.add(animatedImageLabel);
        //
        //
        // removeAll();
        // for (int i = 0; i < menuList.size(); i++) {
        // add((Component) menuList.elementAt(i));
        // }
        validate();
        console.repaint();
    }

    void modifyImageState(boolean animated) {
        // if (animated) {
        // imageLabel.setIcon(animatedIcon);
        // }
        // else {
        // imageLabel.setIcon(staticIcon);
        // }
        // // redraw();
        // imageLabel.repaint();
        // validate();
        // console.repaint();
    }

    // static class Position {
    // int pos;
    //
    // Position(int position) {
    // pos = position;
    // }
    // }

    class MenuBarBrowserObserver extends BrowserObserverAdapter {
        public void editorSpecificMenusChanged(JMenuItem[] menus) {
            // setEditorSpecificMenus(menus);
        }
    }

    class ThreadModelObserver implements Observer {
        public void update(Observable o, Object arg) {
            if (o instanceof ActiveThreadModel)
                if (((ActiveThreadModel) o).getActiveThreadCount() > 0)
                    modifyImageState(true);
                else
                    modifyImageState(false);
        }
    }


}
