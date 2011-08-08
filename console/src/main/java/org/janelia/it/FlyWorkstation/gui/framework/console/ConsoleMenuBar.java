package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.application.EditMenu;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 1:07 PM
 */
public class ConsoleMenuBar extends JMenuBar {
    public static final Position LEFT = new Position(0);
    public static final Position AFTER_FILE = new Position(1);
    public static final Position AFTER_VIEW = new Position(2);
    public static final Position AFTER_EDITOR_SPECIFIC = new Position(3);
    public static final Position AFTER_SPACING_GLUE = new Position(4);
    public static final Position RIGHT = AFTER_SPACING_GLUE;
    private static final Position EDITOR_SPECIFIC = new Position(5);

    protected Browser console;   //protected so that when subclasses, the subclasses menus can have access to it.
    private boolean animation;
    private HashMap /*MultiHash*/ addedMenus = new HashMap();/*MultiHash()*/

    protected JLabel staticImageLabel, animatedImageLabel;
    protected FileMenu fileMenu;
    protected EditMenu editMenu;
    protected JMenu toolsMenu;
    protected JMenu ontologyMenu;
    protected JMenu helpMenu;
    protected JMenu viewMenu;
    protected JMenu windowMenu;
    protected Component menuGlue = Box.createHorizontalGlue();


    public ConsoleMenuBar(Browser console) {
        this.console = console;
        BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        constructMenus();
        addMenus();
//       ActiveThreadModel.getActiveThreadModel().addObserver(new ThreadModelObserver());
//       console.addBrowserObserver(new MenuBarBrowserObserver());
    }

    private void constructMenus() {
        fileMenu = new FileMenu(console);
        editMenu = new EditMenu(console);
        toolsMenu = new ToolsMenu(console);
        ontologyMenu = new OntologyMenu(console);
        helpMenu = new HelpMenu(console);
        viewMenu = new ViewMenu(console);
    }

    private void addMenus() {
        add(fileMenu);
        add(editMenu);
        add(toolsMenu);
        add(viewMenu);
//        add(ontologyMenu);
        add(helpMenu);
        add(menuGlue);
        if (staticImageLabel != null) add(staticImageLabel);
    }

    public void add(Component comp, Position pos) {
        addedMenus.put(pos, comp);
        redraw();
    }

    public void remove(Component comp) {
        addedMenus.remove(comp);
//       Vector vec;
//       for (Enumeration e=addedMenus.elements();e.hasMoreElements();) {
//         vec=(Vector)e.nextElement();
//         if (vec.contains(comp)) {
//           vec.remove(comp);
//           break;
//         }
//       }
        redraw();
    }

    public void setAnimationIcon(ImageIcon animationIcon) {
        animatedImageLabel = new JLabel(animationIcon);
//        animatedImageLabel.addMouseListener(new MyMouseListener());
    }

    public void setStaticIcon(ImageIcon staticIcon) {
        staticImageLabel = new JLabel(staticIcon);
//        staticImageLabel.addMouseListener(new MyMouseListener());
    }

    private void setEditorSpecificMenus(JMenuItem[] menus) {
        addedMenus.remove(EDITOR_SPECIFIC);
        if (menus != null) for (int i = 0; i < menus.length; i++) {
            addedMenus.put(EDITOR_SPECIFIC, menus[i]);
        }
        redraw();
    }

    private void redraw() {
        Vector menuList = new Vector();
        Vector tmpVec;
        if (addedMenus.containsKey(LEFT)) {
            tmpVec = (Vector) addedMenus.get(LEFT);
            menuList.addAll(tmpVec);
        }
        menuList.addElement(fileMenu);
        if (addedMenus.containsKey(AFTER_FILE)) {
            tmpVec = (Vector) addedMenus.get(AFTER_FILE);
            menuList.addAll(tmpVec);
        }
        menuList.add(viewMenu);
        if (addedMenus.containsKey(AFTER_VIEW)) {
            tmpVec = (Vector) addedMenus.get(AFTER_VIEW);
            menuList.addAll(tmpVec);
        }
        if (addedMenus.containsKey(EDITOR_SPECIFIC)) {
            tmpVec = (Vector) addedMenus.get(EDITOR_SPECIFIC);
            menuList.addAll(tmpVec);
        }
        menuList.add(windowMenu);
        if (addedMenus.containsKey(AFTER_EDITOR_SPECIFIC)) {
            tmpVec = (Vector) addedMenus.get(AFTER_EDITOR_SPECIFIC);
            menuList.addAll(tmpVec);
        }
        menuList.add(menuGlue);
        if (addedMenus.containsKey(AFTER_SPACING_GLUE)) {
            tmpVec = (Vector) addedMenus.get(AFTER_SPACING_GLUE);
            menuList.addAll(tmpVec);
        }
        if (staticImageLabel != null) if (!animation) menuList.add(staticImageLabel);
        else menuList.add(animatedImageLabel);


        removeAll();
        for (int i = 0; i < menuList.size(); i++) {
            add((Component) menuList.elementAt(i));
        }
        validate();
        console.repaint();
    }

    private void modifyImageState(boolean animated) {
        if (staticImageLabel == null || animatedImageLabel == null) return;
        animation = animated;
        redraw();
    }


    static class Position {
        int pos;

        Position(int position) {
            pos = position;
        }
    }

//    class MenuBarBrowserObserver extends BrowserObserverAdapter {
//       public void editorSpecificMenusChanged(JMenuItem[] menus){
//         setEditorSpecificMenus(menus);
//       }
//    }

//    class ThreadModelObserver implements Observer {
//       public void update(Observable o, Object arg) {
//         if (o instanceof ActiveThreadModel)
//           if (((ActiveThreadModel)o).getActiveThreadCount()>0) modifyImageState(true);
//           else modifyImageState(false);
//      }
//    }
//
//    class MyMouseListener implements MouseListener {
//      private ProgressMeter meter = ProgressMeter.getProgressMeter();
//      public void mouseClicked(MouseEvent e){}
//      public void mousePressed(MouseEvent e){}
//      public void mouseReleased(MouseEvent e){}
//      public void mouseEntered(MouseEvent e) {
//        meter.setLocationRelativeTo(console);
//        meter.setVisible(true);
//      }
//      public void mouseExited(MouseEvent e){
//        meter.setVisible(false);
//      }
//    }

}
